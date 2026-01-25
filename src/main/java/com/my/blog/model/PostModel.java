package com.my.blog.model;

import java.util.List;

public record PostModel(long id, String title, String text, List<String> tags, int likesCount, int commentsCount) {

}