package com.my.blog.mapper;

import com.my.blog.model.CommentModel;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class CommentRowMapper implements RowMapper<CommentModel> {
    @Override
    public CommentModel mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CommentModel(
                rs.getLong("id"),
                rs.getString("text"),
                rs.getLong("post_id")
        );
    }
}
