package com.my.blog.integration.dao;


import com.my.blog.dto.PostUpdateDto;
import com.my.blog.exception.EntityNotFoundException;
import com.my.blog.integration.BaseTestContainerTest;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.repository.JdbcPostRepository;
import com.my.blog.utils.SearchParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@Transactional
@SpringBootTest
class JdbcRepositoryTest extends BaseTestContainerTest {
    @Autowired
    JdbcPostRepository repository;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        runSchema(dataSource);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("""
                    TRUNCATE post, tag, post_tag, comment, post_image
                    RESTART IDENTITY CASCADE
                """);
    }

    @Test
    void givenPostId_whenGetPost_whenReturnPostModel() {
        insertPost(
                "post text",
                "post title",
                100,
                "tag title",
                "comment text"
        );

        final var postModel = repository.getPost(1).get();

        assertEquals(1, postModel.id());
        assertEquals("post title", postModel.title());
        assertEquals("post text", postModel.text());
        assertEquals(1, postModel.commentsCount());
        assertEquals(100, postModel.likesCount());
        assertEquals(List.of("tag title"), postModel.tags());
    }

    @Test
    void givenPostUpdateModel_whenCreatePost_thenReturnPostModel() {
        final var postModel = repository.createPost(
                new PostUpdateDto(null, "post text", "post title", List.of("tag1", "tag2")
                )
        );

        assertEquals(1, postModel.id());
        assertEquals("post title", postModel.title());
        assertEquals("post text", postModel.text());
        assertEquals(0, postModel.commentsCount());
        assertEquals(0, postModel.likesCount());
        assertEquals(List.of("tag1", "tag2"), postModel.tags());

        final var postModelFromQuery = jdbcTemplate.queryForObject("""
                        SELECT id, text, title, likes_count FROM post
                        WHERE id = 1
                """, (rs, count) ->
                new PostModel(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("text"),
                        List.of(),
                        rs.getInt("likes_count"),
                        0
                )
        );

        assertNotNull(postModelFromQuery);
        assertEquals(1, postModelFromQuery.id());
        assertEquals("post title", postModelFromQuery.title());
        assertEquals("post text", postModelFromQuery.text());
        assertEquals(0, postModelFromQuery.likesCount());
    }

    @Test
    void givenNoSearchStringWithOffset_whenGetPosts_thenReturnPosts() {
        insertPosts(5);

        final var posts = repository.getPosts(null, 3, 5);

        assertEquals(2, posts.size());

        assertEquals(4, posts.getFirst().id());
    }

    @Test
    void givenWithSearchAndOffset_whenGetPosts_thenReturnPosts() {
        insertPosts(6);

        final var posts = repository.getPosts(new SearchParams("1", List.of()), 0, 5);

        assertEquals(1, posts.size());

        assertEquals(2, posts.getFirst().id());
    }

    @Test
    void givenPostUpdateDto_whenUpdatePost_thenReturnUpdatedPost()  {
        insertPost("post text", "post title", 100, "tag1", "some comment");


        final var updatedText = "post text updated";
        final var updatedTitle = "post title updated";
        final var updatedTags = List.of("tag2", "tag3");

        final var updatedPost = repository.updatePost(
                new PostUpdateDto(
                        1L,
                        updatedText,
                        updatedTitle,
                        updatedTags
                )
        );

        assertNotNull(updatedPost);
        assertEquals(1, updatedPost.id());
        assertEquals(updatedTitle, updatedPost.title());
        assertEquals(updatedText, updatedPost.text());
        assertEquals(updatedTags, updatedPost.tags());

        final var requeriedPost = repository.getPost(1L).get();

        assertEquals(1, requeriedPost.id());
        assertEquals(updatedTitle, requeriedPost.title());
        assertEquals(updatedText, requeriedPost.text());
        assertEquals(updatedTags, requeriedPost.tags());
    }

    @Test
    void givenNoSearchParams_whenCountPosts_thenReturnCount() {
        insertPosts(5);


        final var count = repository.countPosts(
                null
        );

        assertEquals(5, count);

    }

    @Test
    void givenExistingSearch_whenCountPosts_thenReturnCount() {
        insertPosts(5);


        final var count = repository.countPosts(
                new SearchParams("2", List.of())
        );

        assertEquals(1, count);
    }

    @Test
    void givenNotExistedTags_whenCountPosts_thenReturnZero() {
        insertPosts(5);


        final var count = repository.countPosts(
                new SearchParams("2", List.of("tag 10"))
        );

        assertEquals(0, count);
    }

    @Test
    void givenLikesCount_whenIncrementLikesCount_thenIncrement() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");


        final var likesCount = repository.incrementLikesCount(1);

        assertEquals(3, likesCount);

        final var queriedPost = repository.getPost(1).get();

        assertEquals(3, queriedPost.likesCount());
    }

    @Test
    void givenPostId_whenDeletePost_thenDeletePost() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        final var count = repository.countPosts(
                new SearchParams("", List.of())
        );

        assertEquals(1, count);

        repository.deletePost(1);

        final var countAfterDelete = repository.countPosts(
                new SearchParams("", List.of())
        );

        assertEquals(0, countAfterDelete);
    }


    @Test
    void givenCommentModel_whenCreateComment_thenInsertComment() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        final var comment = repository.createComment(new CommentModel(null, "comment text", 1L));


        assertEquals(1, comment.postId());
        assertEquals("comment text", comment.text());

        final var post = repository.getPost(1).get();

        assertEquals(2, post.commentsCount());
    }

    @Test
    void givenCommentId_whenDeleteComment_thenDeleteComment() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        repository.deleteComment(1);

        final var post = repository.getPost(1).get();

        assertEquals(0, post.commentsCount());
    }

    @Test
    void givenCommentModel_whenUpdateComment_thenUpdateComment() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        repository.updateComment(new CommentModel(1L, "new comment text", 1L));

        final var comment = repository.getComment(1);

        assertEquals(1, comment.postId());
        assertEquals("new comment text", comment.text());
    }

    @Test
    void givenCommentId_whenGetComment_thenReturnModel() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        final var comment = repository.getComment(1);

        assertEquals(1, comment.postId());
        assertEquals("some comment", comment.text());
    }

    @Test
    void givenPhotoData_whenUpdatePostImage_thenSavePath() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        repository.updatePostImage(1L, "/some/path");

        final var path = jdbcTemplate.queryForObject("""
                        SELECT filename FROM post_image
                        WHERE id = 1
                        """,
                String.class
        );

        assertEquals("/some/path", path);
    }

    @Test
    void givenCorrectPostId_whenGetPostImagePath_thenReturnPath() {
        insertPost("post text ", "post title ", 2, "tag1", "some comment");

        repository.updatePostImage(1L, "/some/path");

        final var path = repository.getPostImagePath(1L);

        assertEquals("/some/path", path.orElse(""));
    }

    void insertPost(
            String postText,
            String postTitle,
            int likesCount,
            String tagTitle,
            String commentText
    ) {
        final var postId = jdbcTemplate.queryForObject("""
                        INSERT INTO post(text, title, likes_count)
                        VALUES (?, ?, ?)
                        RETURNING id
                """, Long.class, postText, postTitle, likesCount);

        final var tagId = jdbcTemplate.queryForObject("""
                    INSERT INTO tag (title)
                    VALUES (?)
                    ON CONFLICT DO NOTHING
                    RETURNING id
                """, Long.class, tagTitle);

        jdbcTemplate.update("""
                    INSERT INTO post_tag (tag_id, post_id)
                    VALUES (?, ?)
                """, tagId, postId);

        jdbcTemplate.update("""
                    INSERT INTO comment (text, post_id)
                    VALUES (?, ?)
                """, commentText, postId);
    }

    void insertPosts(int count) {
        for (int i = 0; i < count; i++) {
            repository.createPost(
                    new PostUpdateDto(
                            null,
                            "post text",
                            "post title" + i,
                            List.of("tag1", "tag2")
                    )
            );
        }
    }
}