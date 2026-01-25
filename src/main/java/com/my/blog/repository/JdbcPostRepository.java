package com.my.blog.repository;

import com.my.blog.dto.CreatePostDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.model.TagModel;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(CreatePostDto createPostDto) {
        final var createPostQuery = """
                INSERT INTO post(text, title)
                values (?, ?)
                RETURNING id
                """;

        final var postId = jdbcTemplate.queryForObject(
                createPostQuery,
                Long.class,
                createPostDto.text(),
                createPostDto.title()
        );

        if (postId == null) {
            throw new IllegalStateException("Post ID was null after insert");
        }


        final var batch = new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, createPostDto.tags().get(i));
            }

            @Override
            public int getBatchSize() {
                return createPostDto.tags().size();
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


        final List<TagModel> tags = getTagModels(createPostDto);

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

    private List<TagModel> getTagModels(CreatePostDto createPostDto) {
        MapSqlParameterSource params =
                new MapSqlParameterSource("names", createPostDto.tags());

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
                            ARRAY_AGG(t.title) AS tags,
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
    public void updatePost() {

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
        return null;
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

    }

    @Override
    public String getPostImagePath(Long postId) {

    }
}
