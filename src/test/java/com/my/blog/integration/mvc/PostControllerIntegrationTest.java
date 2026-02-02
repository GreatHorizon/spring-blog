package com.my.blog.integration.mvc;

import com.my.blog.configuration.WebConfiguration;
import com.my.blog.dto.PostUpdateDto;
import com.my.blog.model.CommentModel;
import com.my.blog.repository.IPostRepository;
import com.my.blog.utils.SearchParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(classes = {
        DataSourceConfig.class,
        WebConfiguration.class,
})
@WebAppConfiguration
@TestPropertySource(locations = "classpath:test-application.properties")
public class PostControllerIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IPostRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        // Чистим БД перед каждым тестом
        jdbcTemplate.execute("DELETE FROM post");
        jdbcTemplate.execute("DELETE FROM comment");
        jdbcTemplate.execute("ALTER SEQUENCE post_id_seq RESTART WITH 1");
        jdbcTemplate.execute("ALTER SEQUENCE comment_id_seq RESTART WITH 1");
    }

    @Test
    void givenUpdatePostModel_whenCreatePost_thenReturnPost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .acceptCharset(StandardCharsets.UTF_8)
                        .content(
                                """
                                        {
                                            "text": "text1",
                                            "title": "title1",
                                            "tags": ["123"]
                                        }
                                        
                                        """
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("text1"))
                .andExpect(jsonPath("$.title").value("title1"))
                .andExpect(jsonPath("$.tags[0]").value("123"));

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("text1"))
                .andExpect(jsonPath("$.title").value("title1"))
                .andExpect(jsonPath("$.tags[0]").value("123"));
    }

    @Test
    void givenBorders_whenGetPosts_thenReturnsNoData() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts").isEmpty())
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(0));
    }

    @Test
    void givenBorders_whenGetPosts_thenReturnsPosts() throws Exception {
        insertPosts(5);

        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void givenNewTagFilter_whenGetPosts_thenReturnsZero() throws Exception {
        insertPosts(5);

        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("search", "#new_tag"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts.length()").value(0))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(0));
    }

    @Test
    void givenTag_whenGetPosts_thenReturnsItems() throws Exception {
        insertPosts(10);

        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("search", "#2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts.length()").value(5))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void givenSearch_whenGetPosts_thenReturnsItems() throws Exception {
        insertPosts(3);

        mockMvc.perform(get("/api/posts")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("search", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts.length()").value(1))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void givenNewPostData_whenUpdatePost_thenReturnUpdatedPost() throws Exception {
        insertPosts(1);

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .acceptCharset(StandardCharsets.UTF_8)
                        .content(
                                """
                                        {
                                            "id": 1,
                                            "text": "updated text",
                                            "title": "updated title",
                                            "tags": ["updated tag title"]
                                        }
                                        
                                        """
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("updated text"))
                .andExpect(jsonPath("$.title").value("updated title"))
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0]").value("updated tag title"));

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("updated text"))
                .andExpect(jsonPath("$.title").value("updated title"))
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.tags[0]").value("updated tag title"));
    }

    @Test
    void givenCommentText_whenCreateComment_thenSave() throws Exception {
        insertPosts(1);

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .acceptCharset(StandardCharsets.UTF_8)
                        .content(
                                """
                                        {
                                            "postId": 1,
                                            "text": "commentText"
                                        }
                                        
                                        """
                        ))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("commentText"));

        mockMvc.perform(get("/api/posts/1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("commentText"))
                .andExpect(jsonPath("$.postId").value(1));
    }

    @Test
    void givenCommentId_whenGetComment_thenReturn() throws Exception {
        insertPosts(1);

        repository.createComment(new CommentModel(null, "commentText", 1L));

        mockMvc.perform(get("/api/posts/1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("commentText"));

    }

    @Test
    void givenCommentText_whenUpdateComment_thenUpdate() throws Exception {
        insertPosts(1);

        repository.createComment(new CommentModel(null, "commentText", 1L));

        mockMvc.perform(put("/api/posts/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .acceptCharset(StandardCharsets.UTF_8)
                        .content(
                                """
                                        {
                                            "postId": 1,
                                            "id": 1,
                                            "text": "commentTextNew"
                                        }
                                        
                                        """
                        ))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("commentTextNew"));

        mockMvc.perform(get("/api/posts/1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("commentTextNew"))
                .andExpect(jsonPath("$.postId").value(1));
    }

    @Test
    void givenPostId_whenGetComments_thenReturnList() throws Exception {
        insertPosts(1);

        repository.createComment(new CommentModel(null, "commentText", 1L));
        repository.createComment(new CommentModel(null, "commentText", 1L));
        repository.createComment(new CommentModel(null, "commentText", 1L));
        repository.createComment(new CommentModel(null, "commentText", 1L));


        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }


    @Test
    void givenPostId_whenDeletePost_thenSuccess() throws Exception {
        insertPosts(1);

        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isNotFound());
    }


    void insertPosts(int count) {
        for (int i = 0; i < count; i++) {
            repository.createPost(new PostUpdateDto(null, "text", "title" + i, i % 2 == 0 ? List.of("2") : List.of("1")));
        }


        final var postsCount = repository.countPosts(new SearchParams(null, null));
        System.out.println(postsCount);
    }
}


