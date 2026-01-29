package com.my.blog;

import com.my.blog.model.PostModel;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PostMapper implements RowMapper<PostModel> {
    @Override
    public PostModel mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PostModel(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("text"),
                Optional.ofNullable(rs.getArray("tags"))
                        .map(arr -> {
                            try {
                                return Arrays.asList((String[]) arr.getArray());
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .orElse(List.of()),
                rs.getInt("likes_count"),
                rs.getLong("comments_count")
        );
    }
}
