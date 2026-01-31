package com.my.blog.unit;

import com.my.blog.dto.PostUpdateDto;
import com.my.blog.dto.PostsDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.repository.IPostRepository;
import com.my.blog.service.PostService;
import com.my.blog.utils.SearchParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
class PostServiceTest {

    @Autowired
    PostService postService;

    @Autowired
    IPostRepository postRepository;

    @BeforeEach
    void resetAll() {
        Mockito.reset(postRepository);
    }

    @Test
    void givenNoText_whenCreatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                null,
                "Test text",
                List.of("java", "spring")
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.createPost(dto)
        );

        assertEquals("text should not be null", exception.getMessage());
    }


    @Test
    void givenNoTitle_whenCreatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                "text",
                null,
                List.of("java", "spring")
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.createPost(dto)
        );

        assertEquals("title should not be null", exception.getMessage());
    }

    @Test
    void givenNoTags_whenCreatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                "text",
                "title",
                null
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.createPost(dto)
        );

        assertEquals("tags should not be null", exception.getMessage());
    }

    @Test
    void givenPageSizeLessThan1_whenGetPosts_thenThrows() {

        final var errorMessage = "pageSize should be > 0";

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.getPosts(null, 0, -1)
        );

        assertEquals(errorMessage, exception.getMessage());

        exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.getPosts(null, 0, 0)
        );

        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void givenPageNumberLessThan1_whenGetPosts_thenThrows() {
        final var errorMessage = "pageNumber should be > 0";

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.getPosts(null, 0, 1)
        );

        assertEquals(errorMessage, exception.getMessage());

        exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.getPosts(null, -1, 1)
        );

        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void givenNullSearch_whenGetPosts_thenCallRepo() {
        Mockito.when(postRepository.countPosts(Mockito.any())).thenReturn(1L);

        postService.getPosts(null, 1, 5);


        Mockito.verify(postRepository).getPosts(new SearchParams(null, null), 0, 5);
        Mockito.verify(postRepository).countPosts(new SearchParams(null, null));
    }

    @Test
    void givenSearchWithNoTags_whenGetPosts_thenReturnParsedSearch() {
        Mockito.when(postRepository.countPosts(Mockito.any())).thenReturn(1L);


        postService.getPosts("simple search string", 1, 5);

        Mockito.verify(postRepository).getPosts(new SearchParams("simple search string", new ArrayList<String>()), 0, 5);
    }

    @Test
    void givenSearchWithOnlyTags_whenGetPosts_thenReturnParsedTags() {
        Mockito.when(postRepository.countPosts(Mockito.any())).thenReturn(1L);


        postService.getPosts("#simple #search #string", 1, 5);

        Mockito.verify(postRepository).getPosts(
                new SearchParams(
                        "",
                        new ArrayList<>(List.of("simple", "search", "string"))
                ),
                0,
                5
        );

    }

    @Test
    void givenSearchWithEmptyTag_whenGetPosts_thenIgnoreEmptyTag() {
        Mockito.when(postRepository.countPosts(Mockito.any())).thenReturn(1L);


        postService.getPosts("# #search #string", 1, 5);

        Mockito.verify(postRepository).getPosts(
                new SearchParams(
                        "",
                        new ArrayList<>(List.of("search", "string"))
                ),
                0,
                5
        );

    }

    @Test
    void givenSearchWithWordsAndTags_whenGetPosts_thenReturnParsedTagsAndWords() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>(List.of("simple", "search", "string"))
        );

        Mockito.when(postRepository.countPosts(Mockito.any())).thenReturn(1L);

        postService.getPosts("#simple #search #string some words", 1, 5);

        Mockito.verify(postRepository).getPosts(
                searchParams,
                0,
                5
        );
    }

    @Test
    void givenLongDescPost_whenGetPosts_thenReturnShortDesc() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>()
        );


        Mockito.when(postRepository.countPosts(searchParams)).thenReturn(1L);


        Mockito.when(postRepository.getPosts(searchParams, 0, 5))
                .thenReturn(List.of(new PostModel(1L, "title", "so long textso long textso long textso long textso long textso long textso long textso long textso long textso long textso long textso long text", new ArrayList<>(), 10, 10)));

        final var posts = postService.getPosts("some words", 1, 5);

        assertEquals(1, posts.posts().size());

        final var shortText = "so long textso long textso long textso long textso long textso long textso long textso long textso long textso long textso long ...";

        assertEquals(shortText, posts.posts().getFirst().text());
    }

    @Test
    void givenShortDescPost_whenGetPosts_thenReturnOriginalDesc() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>()
        );

        Mockito.when(postRepository.getPosts(searchParams, 0, 5))
                .thenReturn(List.of(new PostModel(1L, "title", "short text", new ArrayList<>(), 10, 10)));

        Mockito.when(postRepository.countPosts(searchParams)).thenReturn(1L);


        final var posts = postService.getPosts("some words", 1, 5);

        assertEquals(1, posts.posts().size());

        final var shortText = "short text";

        assertEquals(shortText, posts.posts().getFirst().text());
    }

    @Test
    void givenSearchForNoItems_whenGetPosts_thenReturnNoItems() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>()
        );

        Mockito.when(postRepository.countPosts(searchParams)).thenReturn(0L);

        final var posts = postService.getPosts("some words", 1, 5);

        assertEquals(new PostsDto(new ArrayList<>(), false, false, 0), posts);
    }

    @Test
    void givenSearchForFirstPage_whenGetPosts_thenReturnPageCountAndFlags() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>()
        );

        Mockito.when(postRepository.countPosts(searchParams)).thenReturn(5L);

        final var posts = postService.getPosts("some words", 1, 2);

        assertEquals(new PostsDto(new ArrayList<>(), false, true, 3), posts);
    }

    @Test
    void givenSearchForLastPage_whenGetPosts_thenReturnPageCountAndFlags() {
        final var searchParams = new SearchParams(
                "some words",
                new ArrayList<>()
        );

        Mockito.when(postRepository.countPosts(searchParams)).thenReturn(5L);

        final var posts = postService.getPosts("some words", 3, 2);

        assertEquals(new PostsDto(new ArrayList<>(), true, false, 3), posts);
    }


    @Test
    void createPost_success() {
        var dto = new PostUpdateDto(
                null,
                "Test title",
                "Test text",
                List.of("java", "spring")
        );

        var expected = new PostModel(
                1L,
                "Test title",
                "Test text",
                List.of("java", "spring"),
                0,
                0
        );

        Mockito.when(postRepository.createPost(dto)).thenReturn(expected);

        var result = postService.createPost(dto);

        assertEquals(expected, result);
        Mockito.verify(postRepository).createPost(dto);
    }

    @Test
    void givenNoTitle_whenUpdatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                "1231",
                null,
                List.of()
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.updatePost(dto)
        );

        assertEquals("title should not be null", exception.getMessage());
    }

    @Test
    void givenNoText_whenUpdatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                null,
                "1231",
                List.of()
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.updatePost(dto)
        );

        assertEquals("text should not be null", exception.getMessage());
    }

    @Test
    void givenNoTags_whenUpdatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                "1231",
                "123",
                null
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.updatePost(dto)
        );

        assertEquals("tags should not be null", exception.getMessage());
    }

    @Test
    void givenNoId_whenUpdatePost_thenThrows() {
        var dto = new PostUpdateDto(
                null,
                "1231",
                "123",
                List.of()
        );

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.updatePost(dto)
        );

        assertEquals("id should not be null", exception.getMessage());
    }

    @Test
    void givenCorrectDto_whenUpdatePost_thenReturnModel() {
        final var dto = new PostUpdateDto(
                1L,
                "1231",
                "123",
                List.of()
        );

        Mockito.when(postRepository.updatePost(dto)).thenReturn(new PostModel(1L, "1231", "123", List.of(), 0, 0));

        final var post = postService.updatePost(dto);

        assertEquals(1, post.id());
    }

    @Test
    void givenCorrectComment_whenCreateComment_thenReturnModel() {
        final var dto = new CommentModel(null, "text", 1L);

        postService.createComment(dto);

        Mockito.verify(postRepository).createComment(dto);
    }

    @Test
    void givenNoPostId_whenCreateComment_thenThrows() {
        final var dto = new CommentModel(null, "text", null);

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.createComment(dto)
        );

        assertEquals("postId should not be null", exception.getMessage());
    }

    @Test
    void givenNoText_whenCreateComment_thenThrows() {
        final var dto = new CommentModel(null, null, 1L);

        final var exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.createComment(dto)
        );

        assertEquals("text should not be null", exception.getMessage());
    }
}
