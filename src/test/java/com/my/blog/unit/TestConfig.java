package com.my.blog.unit;


import com.my.blog.repository.IPostRepository;
import com.my.blog.service.PostService;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;

@Configuration
public class TestConfig {

    @Bean
    public IPostRepository postRepository() {
        return Mockito.mock(IPostRepository.class);
    }

    @Bean
    public PostService postService(IPostRepository postRepository) {
        return new PostService(postRepository);
    }
}