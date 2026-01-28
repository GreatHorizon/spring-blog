package com.my.blog.repository;

import com.my.blog.dto.PostUpdateDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.utils.SearchParams;

import java.util.List;
import java.util.Optional;

public interface IPostRepository {
    void createPost(PostUpdateDto postUpdateDto);

    List<PostModel> getPosts(SearchParams searchParams, int offset, int limit);

    PostModel getPost(long id);

    long countPosts();

    PostModel updatePost(PostUpdateDto postUpdateDto);

    void deletePost(long id);

    int incrementLikesCount(long id);

    List<CommentModel> getComments(long postId);

    CommentModel getComment(long commentId);

    void deleteComment(long commentId);

    CommentModel createComment(CommentModel commentModel);

    CommentModel updateComment(CommentModel commentModel);

    void updatePostImage(Long postId, String path);

    Optional<String> getPostImagePath(Long postId);
}
