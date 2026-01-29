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
        final var pageCount = (postsCount + pageSize - 1) / pageSize;
        final var hasNext = pageNumber + 1 <= pageCount;
        final var hasPrev = pageNumber > 1;

        final var posts = postRepository.getPosts(searchParams, offset, pageSize);

        return new PostsDto(posts, hasPrev, hasNext, pageCount);
    }

    private static SearchParams parseSearchParams(String search) {
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
        return postRepository.updatePost(postUpdateDto);
    }

    public PostModel getPost(long id) {
        return postRepository.getPost(id);
    }

    public void deletePost(long id) {
        postRepository.deletePost(id);
    }

    public int incrementLikesCount(long id) {
        return postRepository.incrementLikesCount(id);
    }

    public List<CommentModel> getComments(long id) {
        return postRepository.getComments(id);
    }

    public CommentModel getComment(long commentId) {
        return postRepository.getComment(commentId);
    }

    public void deleteComment(long commentId) {
        postRepository.deleteComment(commentId);
    }

    public CommentModel createComment(CommentModel commentModel) {
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
}
