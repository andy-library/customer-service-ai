package com.enterprise.csai.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChatSessionRepository {

    private static final RowMapper<ChatSessionEntity> MAPPER = (rs, rowNum) -> {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(rs.getObject("id", UUID.class));
        entity.setTitle(rs.getString("title"));
        try {
            entity.setOwnerId(rs.getString("owner_id"));
        } catch (Exception ignored) {
            entity.setOwnerId(null);
        }
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        entity.setCreatedAt(created == null ? null : created.toInstant().atOffset(ZoneOffset.UTC));
        entity.setUpdatedAt(updated == null ? null : updated.toInstant().atOffset(ZoneOffset.UTC));
        return entity;
    };

    private final JdbcTemplate jdbcTemplate;

    public ChatSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ChatSessionEntity entity) {
        jdbcTemplate.update("""
                INSERT INTO cs_chat_session (id, title, owner_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                entity.getId(),
                entity.getTitle(),
                entity.getOwnerId(),
                Timestamp.from(entity.getCreatedAt().toInstant()),
                Timestamp.from(entity.getUpdatedAt().toInstant()));
    }

    public Optional<ChatSessionEntity> findById(UUID id) {
        List<ChatSessionEntity> list = jdbcTemplate.query(
                "SELECT * FROM cs_chat_session WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public void touch(UUID id) {
        jdbcTemplate.update("UPDATE cs_chat_session SET updated_at = NOW() WHERE id = ?", id);
    }
}
