package com.my.blog.unit;


import com.my.blog.repository.IPostRepository;
import com.my.blog.service.PostService;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;

@Configuration
public class TestConfig {

    @Bean
    public IPostRepository mockRepository() {
        return Mockito.mock(IPostRepository.class);
    }

    @Bean
    @Primary
    public PostService postService(IPostRepository mockRepository) {
        return new PostService(mockRepository);
    }
}