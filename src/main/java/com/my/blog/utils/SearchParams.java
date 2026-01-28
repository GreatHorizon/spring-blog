package com.my.blog.utils;

import java.util.List;

public record SearchParams(String searchQuery, List<String> tagNames) {
}
