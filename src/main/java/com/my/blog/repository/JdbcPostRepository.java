package com.my.blog.repository;

import com.my.blog.PostMapper;
import com.my.blog.dto.PostUpdateDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.model.TagModel;
import com.my.blog.utils.SearchParams;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
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


    private static final String DATASET_CTE = """
            WITH dataset AS (
                SELECT
                    p.id,
                    p.title,
                    p.text,
                    p.likes_count,
                    COUNT(DISTINCT c.id) AS comments_count,
                    ARRAY_AGG(DISTINCT t.title)
                        FILTER (WHERE t.title IS NOT NULL) AS tags,
                    p.created_at
                FROM post p
                LEFT JOIN comment c ON c.post_id = p.id
                LEFT JOIN post_tag pt ON pt.post_id = p.id
                LEFT JOIN tag t ON t.id = pt.tag_id
                WHERE (:search::text IS NULL OR p.title LIKE :search::text)
                GROUP BY p.id, p.title, p.text, p.likes_count, p.created_at
                HAVING (
                    cardinality(:tags::text[]) = 0
                    OR BOOL_OR(t.title = ANY(:tags::text[]))
                )
            )
            """;

    @Transactional
    @Override
    public PostModel updatePost(PostUpdateDto postUpdateDto) {
        updatePostTable(postUpdateDto);

        updatePostTags(postUpdateDto);

        return getPost(postUpdateDto.id());
    }

    private void updatePostTags(PostUpdateDto postUpdateDto) {
        removeTagRelations(postUpdateDto);

        insertTags(postUpdateDto);

    }

    private void insertTags(PostUpdateDto postUpdateDto) {
        insertTags(postUpdateDto.tags());

        insertPostTagRelations(postUpdateDto.id(), postUpdateDto.tags());
    }

    private void removeTagRelations(PostUpdateDto postUpdateDto) {
        final var tagsNames = postUpdateDto.tags();

        if (tagsNames == null || tagsNames.isEmpty()) {
            return;
        }

        final var removeTagRelationQuery = """
                    DELETE FROM post_tag pt
                    WHERE pt.post_id = :postId
                      AND pt.tag_id NOT IN (
                          SELECT t.id
                          FROM tag t
                          WHERE t.title IN (:titles)
                      )
                """;

        final var namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        final var params = new MapSqlParameterSource()
                .addValue("postId", postUpdateDto.id())
                .addValue("titles", tagsNames);

        namedParameterJdbcTemplate.update(removeTagRelationQuery, params);
    }

    private void updatePostTable(PostUpdateDto postUpdateDto) {
        final var updatePostQuery = """
                UPDATE POST SET
                title = COALESCE(?, title),
                text = COALESCE(?, text)
                where id = ?
                """;

        final var updated = jdbcTemplate.update(
                updatePostQuery,
                postUpdateDto.title(),
                postUpdateDto.text(),
                postUpdateDto.id()
        );

        if (updated == 0) {
            throw new RuntimeException("Post not found");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(PostUpdateDto postUpdateDto) {
        insertPost(postUpdateDto);
        insertTags(postUpdateDto);
    }

    private void insertPost(PostUpdateDto postUpdateDto) {
        final var createPostQuery = """
                INSERT INTO post(text, title)
                values (:text, :title)
                RETURNING id
                """;

        final var namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        final var params = new MapSqlParameterSource()
                .addValue("text", postUpdateDto.text())
                .addValue("titles", postUpdateDto.title());

        final var postId = namedTemplate.queryForObject(createPostQuery, params, Long.class);

        if (postId == null) {
            throw new IllegalStateException("Post ID was null after insert");
        }
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
//        final var query = """
//                SELECT
//                    p.id,
//                    p.title,
//                    p.text,
//                    p.likes_count,
//                    COUNT(DISTINCT c.id) AS comments_count,
//                    ARRAY_AGG(DISTINCT t.title)
//                        FILTER (WHERE t.title IS NOT NULL) AS tags
//                FROM post p
//                LEFT JOIN comment c ON c.post_id = p.id
//                LEFT JOIN post_tag pt ON pt.post_id = p.id
//                LEFT JOIN tag t ON t.id = pt.tag_id
//                WHERE (:search::text IS NULL OR p.title LIKE :search::text)
//                GROUP BY p.id, p.title, p.text, p.likes_count, p.created_at
//                HAVING (cardinality(:tags) = 0 OR BOOL_OR(t.title = ANY (:tags)))
//                ORDER BY p.created_at DESC
//                OFFSET :offset
//                LIMIT :limit
//                """;

        String query = DATASET_CTE + """
                    SELECT *
                    FROM dataset
                    ORDER BY created_at DESC
                    OFFSET :offset
                    LIMIT :limit
                """;

        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate);

        String search = searchParams.searchQuery();
        String searchLike = (search == null || search.isBlank())
                ? null
                : "%" + search + "%";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("search", searchLike)
                .addValue("offset", offset)
                .addValue("limit", limit)
                .addValue(
                        "tags",
                        searchParams.tagNames() == null
                                ? new String[0]
                                : searchParams.tagNames().toArray(new String[0])
                );

        return named.query(query, params, new PostMapper());
    }

    @Override
    public long countPosts(SearchParams searchParams) {
        String sql = DATASET_CTE + "SELECT COUNT(*) FROM dataset";

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        String search = searchParams.searchQuery();
        String searchLike = (search == null || search.isBlank())
                ? null
                : "%" + search + "%";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("search", searchLike)
                .addValue(
                        "tags",
                        searchParams.tagNames() == null
                                ? new String[0]
                                : searchParams.tagNames().toArray(new String[0])
                );

        return Optional.ofNullable(namedTemplate.queryForObject(sql, params, Long.class)).orElse(0L);
    }

    private List<TagModel> getTagModels(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource("names", tags);

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
                                  COALESCE(t.tags, ARRAY[]::text[]) AS tags,
                                  COALESCE(c.comments_count, 0) AS comments_count
                              FROM post p
                              LEFT JOIN (
                                  SELECT pt.post_id,
                                         array_agg(t.title) AS tags
                                  FROM post_tag pt
                                  JOIN tag t ON pt.tag_id = t.id
                                  GROUP BY pt.post_id
                              ) t ON t.post_id = p.id
                              LEFT JOIN (
                                  SELECT post_id,
                                         COUNT(*) AS comments_count
                                  FROM comment
                                  GROUP BY post_id
                              ) c ON c.post_id = p.id
                              WHERE p.id = ?
                        
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
                                        throw new IllegalStateException("Failed to read tags array", e);
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
    public List<CommentModel> getComments(long postId) {
        final var getCommentsQuery = """
                SELECT id, text, post_id FROM comment
                WHERE post_id = ?
                """;

        return jdbcTemplate.query(
                getCommentsQuery,
                (rs, rowNum) ->
                        new CommentModel(
                                rs.getLong("id"),
                                rs.getString("text"),
                                rs.getLong("post_id")
                        ),
                postId
        );
    }

    @Override
    public CommentModel getComment(long commentId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT id, text, post_id FROM comment
                        WHERE id = ?
                        """,

                (rs, rowNum) -> new CommentModel(
                        rs.getLong("id"),
                        rs.getString("text"),
                        rs.getLong("post_id")
                ),
                commentId
        );
    }

    @Override
    public void deleteComment(long commentId) {
        jdbcTemplate.update(
                """
                        DELETE FROM comment
                        WHERE id = ?;
                        """, commentId);
    }

    @Override
    public CommentModel createComment(CommentModel commentModel) {
        final var query = """
                INSERT INTO comment(post_id, text) values(?, ?)
                RETURNING id;
                """;

        final var commentId = jdbcTemplate.queryForObject(
                query,
                Long.class,
                commentModel.postId(),
                commentModel.text()
        );

        if (commentId == null) {
            throw new IllegalStateException("Failed to add comment");
        }

        return getComment(commentId);
    }

    @Override
    public CommentModel updateComment(CommentModel commentModel) {
        final var query = """
                UPDATE comment SET
                text = ?
                where post_id = ?
                """;

        jdbcTemplate.update(query, commentModel.text(), commentModel.postId());

        return getComment(commentModel.id());
    }

    @Override
    public void updatePostImage(Long postId, String path) {
        final var query = """
                INSERT INTO post_image(post_id, filename) values (?, ?)
                ON CONFLICT (post_id) DO UPDATE
                SET filename = excluded.filename
                """;

        jdbcTemplate.update(query, postId, path);
    }

    @Override
    public Optional<String> getPostImagePath(Long postId) {
        final var query = """
                SELECT filename FROM post_image
                WHERE post_id = ?
                """;

        return jdbcTemplate.query(
                query
                , rs -> rs.next()
                        ? Optional.of(rs.getString("filename"))
                        : Optional.empty(), postId
        );

    }
}
