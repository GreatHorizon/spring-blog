package com.my.blog.integration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;

public class BaseTestContainerTest {
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresContainerHolder::jdbcUrl);
        registry.add("spring.datasource.username", PostgresContainerHolder::username);
        registry.add("spring.datasource.password", PostgresContainerHolder::password);
    }

    protected void runSchema(DataSource dataSource) {
        final var populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.execute(dataSource);
    }
}
