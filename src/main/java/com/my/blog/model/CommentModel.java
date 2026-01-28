package com.my.blog.model;

public record CommentModel(long id, String text, long postId) {
}