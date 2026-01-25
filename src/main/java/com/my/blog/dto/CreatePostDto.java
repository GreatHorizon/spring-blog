package com.my.blog.dto;

import java.util.List;

public record CreatePostDto(int id,
                            String text,
                            String title,
                            List<String> tags
) {
}
