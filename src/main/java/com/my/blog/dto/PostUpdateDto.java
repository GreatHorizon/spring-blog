package com.my.blog.dto;

import java.util.List;

public record PostUpdateDto(Long id,
                            String text,
                            String title,
                            List<String> tags
) {
}
