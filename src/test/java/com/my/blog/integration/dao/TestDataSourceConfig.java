package com.my.blog.integration.dao;

import com.my.blog.repository.JdbcPostRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TestDataSourceConfig {

    @Bean
    DataSource dataSource() {
        var ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(PostgresContainerHolder.jdbcUrl());
        ds.setUsername(PostgresContainerHolder.username());
        ds.setPassword(PostgresContainerHolder.password());
        return ds;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    JdbcPostRepository jdbcRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcPostRepository(jdbcTemplate);
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}