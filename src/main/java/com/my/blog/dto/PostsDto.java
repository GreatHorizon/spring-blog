package com.my.blog.dto;

import com.my.blog.model.PostModel;

import java.util.List;

public record PostsDto(List<PostModel> posts, boolean hasPrev, boolean hasNext, int lastPage) {
}