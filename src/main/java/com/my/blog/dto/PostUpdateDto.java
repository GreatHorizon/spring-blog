package com.my.blog.dto;

import java.util.List;

public record PostUpdateDto(int id,
                            String text,
                            String title,
                            List<String> tags
) {
}
