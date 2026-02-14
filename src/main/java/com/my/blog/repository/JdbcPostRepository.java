package com.my.blog.repository;

import com.my.blog.exception.EntityNotFoundException;
import com.my.blog.mapper.CommentRowMapper;
import com.my.blog.mapper.PostRowMapper;
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
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPostRepository implements IPostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String POST_FILTER_DATASET_CTE = """
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


    private static final String REMOVE_TAG_RELATION_QUERY = """
                DELETE FROM post_tag pt
                WHERE pt.post_id = :postId
                  AND pt.tag_id NOT IN (
                      SELECT t.id
                      FROM tag t
                      WHERE t.title IN (:titles)
                  )
            """;

    private static final String UPDATE_POST_QUERY = """
            UPDATE POST SET
            title = COALESCE(?, title),
            text = COALESCE(?, text)
            where id = ?
            """;


    private static final String CREATE_POST_QUERY = """
                    INSERT INTO post(text, title)
                    values (:text, :title)
                    RETURNING id
            """;

    private static final String INSERT_TAGS_QUERY = """
                 INSERT INTO tag (title)
                 VALUES(?)
                 ON CONFLICT (title) DO NOTHING
            """;


    private static final String INSERT_POST_TAG_RELATION_QUERY = """
            INSERT INTO post_tag(post_id, tag_id)
            VALUES (?, ?)
            """;

    private static final String GET_POSTS_QUERY = POST_FILTER_DATASET_CTE + """
                SELECT *
                FROM dataset
                ORDER BY created_at DESC
                OFFSET :offset
                LIMIT :limit
            """;


    private static final String GET_TAGS_QUERY = """
            SELECT id, title
            FROM tag
            WHERE title IN (:names)
            """;


    private static final String GET_TAG_QUERY = """
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
            """;

    private static final String DELETE_POST_QUERY = """
            DELETE FROM post
            WHERE ID = ?
            """;

    private static final String INCREMENT_LIKES_COUNT_QUERY = """
            UPDATE POST SET likes_count = likes_count + 1
            where id = ?
            RETURNING likes_count;
            """;


    private static final String GET_COMMENTS_QUERY = """
            SELECT id, text, post_id FROM comment
            WHERE post_id = ?
            """;

    private static final String GET_COMMENT_QUERY = """
            SELECT id, text, post_id FROM comment
            WHERE id = ?
            """;

    private static final String DELETE_COMMENT_QUERY = """
            DELETE FROM comment
            WHERE id = ?;
            """;

    private static final String CREATE_COMMENT_QUERY = """
            INSERT INTO comment(post_id, text) values(?, ?)
            RETURNING id;
            """;

    private static final String UPDATE_COMMENT_QUERY = """
            UPDATE comment SET
            text = ?
            where post_id = ?
            """;

    private static final String UPDATE_POST_IMAGE_QUERY = """
            INSERT INTO post_image(post_id, filename) values (?, ?)
            ON CONFLICT (post_id) DO UPDATE
            SET filename = excluded.filename
            """;

    private static final String GET_POST_IMAGE_QUERY = """
            SELECT filename FROM post_image
            WHERE post_id = ?
            """;

    private static final String GET_POSTS_COUNT_QUERY = POST_FILTER_DATASET_CTE + "SELECT COUNT(*) FROM dataset";

    private static final String POST_NOT_FOUND_ERROR_TEXT = "Post not found";


    @Transactional
    @Override
    public PostModel updatePost(PostUpdateDto postUpdateDto) throws EntityNotFoundException {
        updatePostTable(postUpdateDto);

        updatePostTags(postUpdateDto);

        final var post = getPost(postUpdateDto.id());

        if (post.isEmpty()) {
            throw new EntityNotFoundException(POST_NOT_FOUND_ERROR_TEXT);
        }

        return post.get();
    }

    private void updatePostTags(PostUpdateDto postUpdateDto) {
        removeTagRelations(postUpdateDto);

        insertTags(postUpdateDto.id(), postUpdateDto.tags());

    }

    private void insertTags(Long postId, List<String> tagNames) {
        insertTags(tagNames);

        insertPostTagRelations(postId, tagNames);
    }

    private void removeTagRelations(PostUpdateDto postUpdateDto) {
        final var tagsNames = postUpdateDto.tags();

        if (tagsNames == null || tagsNames.isEmpty()) {
            return;
        }

        final var namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        final var params = new MapSqlParameterSource()
                .addValue("postId", postUpdateDto.id())
                .addValue("titles", tagsNames);

        namedParameterJdbcTemplate.update(REMOVE_TAG_RELATION_QUERY, params);
    }

    private void updatePostTable(PostUpdateDto postUpdateDto) throws EntityNotFoundException {
        final var updated = jdbcTemplate.update(
                UPDATE_POST_QUERY,
                postUpdateDto.title(),
                postUpdateDto.text(),
                postUpdateDto.id()
        );

        if (updated == 0) {
            throw new EntityNotFoundException(POST_NOT_FOUND_ERROR_TEXT);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostModel createPost(PostUpdateDto postUpdateDto) {
        final var id = insertPost(postUpdateDto);

        insertTags(id, postUpdateDto.tags());

        final var post = getPost(id);

        if (post.isEmpty()) {
            throw new IllegalArgumentException(POST_NOT_FOUND_ERROR_TEXT);
        }

        return post.get();
    }

    private Long insertPost(PostUpdateDto postUpdateDto) {
        final var namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        final var params = new MapSqlParameterSource()
                .addValue("text", postUpdateDto.text())
                .addValue("title", postUpdateDto.title());

        final var postId = namedTemplate.queryForObject(CREATE_POST_QUERY, params, Long.class);

        if (postId == null) {
            throw new IllegalStateException(POST_NOT_FOUND_ERROR_TEXT);
        }

        return postId;
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

        jdbcTemplate.batchUpdate(INSERT_TAGS_QUERY, batch);
    }

    private void insertPostTagRelations(long postId, List<String> tagsNames) {
        final List<TagModel> tags = getTagModels(tagsNames);

        jdbcTemplate.batchUpdate(
                INSERT_POST_TAG_RELATION_QUERY,
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
        NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(jdbcTemplate);

        final var params = formPostsQuerySearchParams(searchParams)
                .addValue("offset", offset)
                .addValue("limit", limit);

        return named.query(GET_POSTS_QUERY, params, new PostRowMapper());
    }

    private MapSqlParameterSource formPostsQuerySearchParams(SearchParams searchParams) {

        final var search = searchParams == null ? null : searchParams.searchQuery();
        final var searchLike = (search == null || search.isBlank())
                ? null
                : "%" + search + "%";

        final var tags = searchParams == null || searchParams.tagNames() == null
                ? new String[0]
                : searchParams.tagNames().toArray(new String[0]);


        return new MapSqlParameterSource()
                .addValue("search", searchLike)
                .addValue("tags", tags);
    }


    @Override
    public long countPosts(SearchParams searchParams) {
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        final var params = formPostsQuerySearchParams(searchParams);

        return Optional.ofNullable(
                namedTemplate.queryForObject(GET_POSTS_COUNT_QUERY, params, Long.class)
        ).orElse(0L);
    }

    private List<TagModel> getTagModels(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        MapSqlParameterSource params = new MapSqlParameterSource("names", tags);

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

        return namedParameterJdbcTemplate.query(
                GET_TAGS_QUERY,
                params,
                (rs, i) -> new TagModel(rs.getLong("id"), rs.getString("title")));
    }

    @Override
    public Optional<PostModel> getPost(long id) {
        return jdbcTemplate.query(GET_TAG_QUERY, new PostRowMapper(), id).stream().findFirst();
    }


    @Override
    public void deletePost(long id) {
        jdbcTemplate.update(DELETE_POST_QUERY, id);
    }

    @Override
    public int incrementLikesCount(long id) {
        final var likesCount = jdbcTemplate.queryForObject(INCREMENT_LIKES_COUNT_QUERY, Integer.class, id);

        return likesCount != null ? likesCount : 0;
    }

    @Override
    public List<CommentModel> getComments(long postId) {
        return jdbcTemplate.query(GET_COMMENTS_QUERY, new CommentRowMapper(), postId);
    }

    @Override
    public CommentModel getComment(long commentId) {
        return jdbcTemplate.queryForObject(GET_COMMENT_QUERY, new CommentRowMapper(), commentId);
    }

    @Override
    public void deleteComment(long commentId) {
        jdbcTemplate.update(DELETE_COMMENT_QUERY, commentId);
    }

    @Override
    public CommentModel createComment(CommentModel commentModel) {
        final var commentId = jdbcTemplate.queryForObject(
                CREATE_COMMENT_QUERY,
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
        jdbcTemplate.update(UPDATE_COMMENT_QUERY, commentModel.text(), commentModel.postId());

        return getComment(commentModel.id());
    }

    @Override
    public void updatePostImage(Long postId, String path) {
        jdbcTemplate.update(UPDATE_POST_IMAGE_QUERY, postId, path);
    }

    @Override
    public Optional<String> getPostImagePath(Long postId) {
        return jdbcTemplate.query(
                GET_POST_IMAGE_QUERY
                , rs -> rs.next()
                        ? Optional.of(rs.getString("filename"))
                        : Optional.empty(), postId
        );

    }
}
