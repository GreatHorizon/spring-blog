package com.my.blog.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

@Configuration
public class RestConfiguration {
    @Bean
    public HttpMessageConverter<Object> httpMessageConverter() {
        return new JacksonJsonHttpMessageConverter();
    }
}
