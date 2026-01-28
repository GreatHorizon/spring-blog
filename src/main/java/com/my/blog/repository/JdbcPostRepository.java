package com.my.blog.repository;

import com.my.blog.dto.PostUpdateDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.model.TagModel;
import com.my.blog.utils.SearchParams;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPostRepository implements IPostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @Override
    public PostModel updatePost(PostUpdateDto postUpdateDto) {
        final var updatePostQuery = """
                UPDATE POST SET
                title = COALESCE(?, title),
                text = COALESCE(?, text)
                where id = ?
                """;

        jdbcTemplate.update(
                updatePostQuery,
                postUpdateDto.title(),
                postUpdateDto.text(),
                postUpdateDto.id()
        );

        final var tagsNames = postUpdateDto.tags();

        if (tagsNames != null && !tagsNames.isEmpty()) {
            final var removeTagRelationQuery = """
                        DELETE FROM post_tag pt
                        WHERE pt.post_id = :postId
                          AND pt.tag_id NOT IN (
                              SELECT t.id
                              FROM tag t
                              WHERE t.title IN (:titles)
                          )
                    """;

            var params = new MapSqlParameterSource()
                    .addValue("postId", postUpdateDto.id())
                    .addValue("titles", tagsNames);

            NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);


            namedParameterJdbcTemplate.update(removeTagRelationQuery, params);
        }


        insertTags(tagsNames);
        insertPostTagRelations(postUpdateDto.id(), tagsNames);

        return getPost(postUpdateDto.id());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(PostUpdateDto postUpdateDto) {
        final var createPostQuery = """
                INSERT INTO post(text, title)
                values (?, ?)
                RETURNING id
                """;

        final var postId = jdbcTemplate.queryForObject(
                createPostQuery,
                Long.class,
                postUpdateDto.text(),
                postUpdateDto.title()
        );

        if (postId == null) {
            throw new IllegalStateException("Post ID was null after insert");
        }

        final var tagsNames = postUpdateDto.tags();

        insertTags(tagsNames);
        insertPostTagRelations(postId, tagsNames);

    }

    private void insertTags(List<String> tags) {
        final var batch = new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, tags.get(i));
            }

            @Override
            public int getBatchSize() {
                return tags.size();
            }
        };

        jdbcTemplate.batchUpdate(
                """
                              INSERT INTO tag (title)
                              VALUES(?)
                              ON CONFLICT (title) DO NOTHING
                        """,
                batch
        );
    }

    private void insertPostTagRelations(long postId, List<String> tagsNames) {
        final List<TagModel> tags = getTagModels(tagsNames);

        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO post_tag(post_id, tag_id)
                        VALUES (?, ?)
                        """,
                tags.stream().map(TagModel::id).toList(),
                tags.size(),
                (ps, tagId) -> {
                    ps.setLong(1, postId);
                    ps.setLong(2, tagId);
                }
        );
    }


    @Override
    public List<PostModel> getPosts(SearchParams searchParams, int offset, int limit) {
        final var getPostsQuery = """
                SELECT
                    p.id,
                    p.title,
                    p.text,
                    p.likes_count,
                    COUNT(DISTINCT c.id) AS comments_count,
                    ARRAY_AGG(DISTINCT t.title)
                        FILTER (WHERE t.title IS NOT NULL) AS tags
                FROM post p
                LEFT JOIN comment c ON c.post_id = p.id
                LEFT JOIN post_tag pt ON pt.post_id = p.id
                LEFT JOIN tag t ON t.id = pt.tag_id
                WHERE (? IS NULL OR p.title LIKE ?)
                GROUP BY p.id, p.title, p.text, p.likes_count, p.created_at
                HAVING (cardinality(?) = 0 OR BOOL_OR(t.title = ANY (?)))
                ORDER BY p.created_at desc
                OFFSET ?
                LIMIT ?
                """;


        final var rowMapper = new RowMapper<PostModel>() {
            @Override
            public PostModel mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new PostModel(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("text"),
                        Optional.ofNullable(rs.getArray("tags"))
                                .map(arr -> {
                                    try {
                                        return Arrays.asList((String[]) arr.getArray());
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .orElse(List.of()),
                        rs.getInt("likes_count"),
                        rs.getLong("comments_count")
                );
            }
        };
        return jdbcTemplate.query(
                getPostsQuery,
                ps -> {
                    String search = searchParams.searchQuery();
                    String searchLike = (search == null || search.isBlank())
                            ? null
                            : "%" + search + "%";

                    // 1,2 — search
                    ps.setString(1, searchLike);
                    ps.setString(2, searchLike);

                    Array tagsArray = ps.getConnection().createArrayOf(
                            "text",
                            searchParams.tagNames() == null
                                    ? new String[0]
                                    : searchParams.tagNames().toArray(new String[0])
                    );
                    ps.setArray(3, tagsArray);
                    ps.setArray(4, tagsArray);


                    ps.setInt(5, offset);
                    ps.setInt(6, limit);
                },
                rowMapper
        );
    }

    private List<TagModel> getTagModels(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource params =
                new MapSqlParameterSource("names", tags);

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        final var tagsQuery = """
                    SELECT id, title
                    FROM tag
                    WHERE title IN (:names)
                """;

        return namedParameterJdbcTemplate.query(
                tagsQuery,
                params,
                (rs, i) -> new TagModel(rs.getLong("id"), rs.getString("title")));
    }

    @Override
    public PostModel getPost(long id) {
        return jdbcTemplate.queryForObject(
                """
                        
                        SELECT
                            p.id,
                            p.title,
                            p.text,
                            p.likes_count,
                            COALESCE(
                                array_agg(t.title) FILTER (WHERE t.title IS NOT NULL),
                                '{}'
                            ) AS tags,
                            COUNT(c.id) as comments_count
                        FROM post p
                                 LEFT JOIN post_tag pt ON p.id = pt.post_id
                                 LEFT JOIN tag t ON pt.tag_id = t.id
                                 LEFT JOIN comment c ON PT.tag_id = C.post_id
                        WHERE p.id = ?
                        GROUP BY p.id, p.title, p.text, p.likes_count;
                        """,
                (rs, rowNum) -> new PostModel(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("text"),
                        Optional.ofNullable(rs.getArray("tags"))
                                .map(a -> {

                                    try {
                                        return Arrays.asList((String[]) a.getArray());
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .orElse(new ArrayList<>()),
                        rs.getInt("likes_count"),
                        rs.getInt("comments_count")
                ),
                id
        );
    }

    @Override
    public long countPosts() {
        final var getCountQuery = "SELECT COUNT(*) FROM post";

        return Optional.ofNullable(jdbcTemplate.queryForObject(getCountQuery, Long.class)).orElse(0L);
    }


    @Override
    public void deletePost(long id) {
        jdbcTemplate.update(
                """
                        DELETE FROM post
                        WHERE ID = ?
                        """,
                id
        );
    }

    @Override
    public int incrementLikesCount(long id) {
        final var incrementLikesQuery = """
                UPDATE POST SET likes_count = likes_count + 1
                where id = ?
                RETURNING likes_count;
                """;

        final var likesCount = jdbcTemplate.queryForObject(incrementLikesQuery, Integer.class, id);

        return likesCount != null ? likesCount : 0;
    }

    @Override
    public List<CommentModel> getComments(long id) {
        return List.of();
    }

    @Override
    public CommentModel getComment(long postId, long commentId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id, text FROM comment
                        WHERE id = ?
                        """,

                (rs, rowNum) -> new CommentModel(
                        rs.getLong("id"),
                        rs.getString("text"),
                        postId
                ),
                commentId
        );
    }

    @Override
    public void deleteComment(long postId, long commentId) {

    }

    @Override
    public void createComment(CommentModel commentModel) {

    }

    @Override
    public void updateComment(CommentModel commentModel) {

    }

    @Override
    public void updatePostImage(Long postId, String path) {
        jdbcTemplate.update(
                """
                        INSERT INTO post_image(post_id, filename) values (?, ?)
                        ON CONFLICT (post_id) DO UPDATE
                        SET filename = excluded.filename
                        """,
                postId,
                path
        );
    }

    @Override
    public Optional<String> getPostImagePath(Long postId) {
        return jdbcTemplate.query(
                """
                        SELECT filename FROM post_image
                        WHERE post_id = ?
                        """, rs -> rs.next()
                        ? Optional.of(rs.getString("filename"))
                        : Optional.empty(), postId);

    }
}
