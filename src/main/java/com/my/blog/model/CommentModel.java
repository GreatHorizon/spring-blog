package com.my.blog.model;

public record CommentModel(
        Long id,
        String text,
        Long postId
) {
}