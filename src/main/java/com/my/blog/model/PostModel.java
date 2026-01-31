package com.my.blog.model;

import java.util.List;

public record PostModel(Long id, String title, String text, List<String> tags, long likesCount, long commentsCount) {

}