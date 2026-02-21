package com.my.blog.integration;

import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresContainerHolder {

    private static final PostgreSQLContainer<?> CONTAINER;

    static {
        CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test");

        CONTAINER.start();
    }

    public static String jdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    public static String username() {
        return CONTAINER.getUsername();
    }

    public static String password() {
        return CONTAINER.getPassword();
    }

    private PostgresContainerHolder() {
    }
}
