package com.my.blog.repository;

import com.my.blog.dto.CreatePostDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;

import java.util.List;

public interface IPostRepository {
    void createPost(CreatePostDto createPostDto);

    PostModel getPost(long id);

    void updatePost();

    void deletePost(long id);

    int incrementLikesCount(long id);

    List<CommentModel> getComments(long postId);

    CommentModel getComment(long postId, long commentId);

    void deleteComment(long postId, long commentId);

    void createComment(CommentModel commentModel);

    void updateComment(CommentModel commentModel);

    void updatePostImage(Long postId, String path);

    String getPostImagePath(Long postId);
}
