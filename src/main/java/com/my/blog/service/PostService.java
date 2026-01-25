package com.my.blog.service;

import com.my.blog.dto.CreatePostDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.repository.IPostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final IPostRepository postRepository;

    public PostService(IPostRepository jdbcPostRepository) {
        this.postRepository = jdbcPostRepository;
    }

    public void savePost(CreatePostDto createPostDto) {
        postRepository.createPost(createPostDto);
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

    public void deleteComment(long postId, long commentId) {
        postRepository.deleteComment(postId, commentId);
    }

    public void createComment(CommentModel commentModel) {

    }

    public void updateComment(CommentModel commentModel) {

    }

    public void updatePostImage(Long postId, String path) {
        postRepository.updatePostImage(postId, path);
    }

    public String getPostImagePath(Long postId) {
        return postRepository.getPostImagePath(postId);
    }
}
