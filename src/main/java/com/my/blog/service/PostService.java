package com.my.blog.service;

import com.my.blog.dto.PostUpdateDto;
import com.my.blog.dto.PostsDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.repository.IPostRepository;
import com.my.blog.utils.SearchParams;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostService {
    private final IPostRepository postRepository;

    public PostService(IPostRepository jdbcPostRepository) {
        this.postRepository = jdbcPostRepository;
    }

    public PostModel createPost(PostUpdateDto postUpdateDto) {
        validatePost(postUpdateDto);

        return postRepository.createPost(postUpdateDto);
    }

    public PostsDto getPosts(String search, int pageNumber, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize should be > 0");
        }

        if (pageNumber <= 0) {
            throw new IllegalArgumentException("pageNumber should be > 0");
        }

        final var searchParams = parseSearchParams(search);
        final var offset = (pageNumber - 1) * pageSize;
        final var postsCount = postRepository.countPosts(searchParams);

        if (postsCount == 0) {
            return new PostsDto(new ArrayList<>(), false, false, 0);
        }

        final var pageCount = (postsCount + pageSize - 1) / pageSize;
        final var hasNext = pageNumber + 1 <= pageCount;
        final var hasPrev = pageNumber > 1;

        final var posts = postRepository.getPosts(searchParams, offset, pageSize).stream().map((post) -> {
            if (post.text().length() <= 128) {
                return post;
            }

            final var shortText = post.text().substring(0, 128) + "...";

            return new PostModel(post.id(), post.title(), shortText, post.tags(), post.likesCount(), post.commentsCount());
        }).toList();


        return new PostsDto(posts, hasPrev, hasNext, pageCount);
    }

    private static SearchParams parseSearchParams(String search) {
        if (search == null || search.isBlank()) {
            return new SearchParams(null, null);
        }

        final var parts = Arrays.stream(search.split(" ")).filter((item) -> !item.isBlank()).toList();
        final var tags = new ArrayList<String>();
        StringJoiner joiner = new StringJoiner(" ", "", "");

        parts.forEach(item -> {
            if (item.startsWith("#")) {
                final var tagName = item.substring(1);
                if (!tagName.isBlank()) {
                    tags.add(tagName);
                }
            } else {
                joiner.add(item);
            }
        });

        return new SearchParams(joiner.toString(), tags);
    }

    public PostModel updatePost(PostUpdateDto postUpdateDto) {
        validatePost(postUpdateDto);

        if (postUpdateDto.id() == null) {
            throw new IllegalArgumentException("id should not be null");
        }

        return postRepository.updatePost(postUpdateDto);
    }

    public PostModel getPost(Long id) {
        return postRepository.getPost(id);
    }

    public void deletePost(Long id) {
        postRepository.deletePost(id);
    }

    public int incrementLikesCount(Long id) {
        return postRepository.incrementLikesCount(id);
    }

    public List<CommentModel> getComments(Long id) {
        return postRepository.getComments(id);
    }

    public CommentModel getComment(Long commentId) {
        return postRepository.getComment(commentId);
    }

    public void deleteComment(Long commentId) {
        postRepository.deleteComment(commentId);
    }

    public CommentModel createComment(CommentModel commentModel) {
        if (commentModel.postId() == null) {
            throw new IllegalArgumentException("postId should not be null");
        }

        if (commentModel.text() == null) {
            throw new IllegalArgumentException("text should not be null");
        }

        return postRepository.createComment(commentModel);
    }

    public CommentModel updateComment(CommentModel commentModel) {
        return postRepository.updateComment(commentModel);
    }

    public void updatePostImage(Long postId, String path) {
        postRepository.updatePostImage(postId, path);
    }

    public Optional<String> getPostImagePath(Long postId) {
        return postRepository.getPostImagePath(postId);
    }

    private void validatePost(PostUpdateDto postUpdateDto) {
        if (postUpdateDto.title() == null) {
            throw new IllegalArgumentException("title should not be null");
        }

        if (postUpdateDto.text() == null) {
            throw new IllegalArgumentException("text should not be null");
        }

        if (postUpdateDto.tags() == null) {
            throw new IllegalArgumentException("tags should not be null");
        }
    }
}
