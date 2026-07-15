package com.enterprise.csai.chat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class ChatMessageRepository {

    private static final RowMapper<ChatMessageEntity> MAPPER = (rs, rowNum) -> {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(rs.getObject("id", UUID.class));
        entity.setSessionId(rs.getObject("session_id", UUID.class));
        entity.setRole(rs.getString("role"));
        entity.setContent(rs.getString("content"));
        Timestamp created = rs.getTimestamp("created_at");
        entity.setCreatedAt(created == null ? null : created.toInstant().atOffset(ZoneOffset.UTC));
        return entity;
    };

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ChatMessageEntity entity) {
        jdbcTemplate.update("""
                INSERT INTO cs_chat_message (id, session_id, role, content, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                entity.getId(),
                entity.getSessionId(),
                entity.getRole(),
                entity.getContent(),
                Timestamp.from(entity.getCreatedAt().toInstant()));
    }

    public List<ChatMessageEntity> findRecentBySession(UUID sessionId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        // Fetch newest first then reverse for chronological order
        List<ChatMessageEntity> newestFirst = jdbcTemplate.query("""
                SELECT * FROM cs_chat_message
                WHERE session_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """, MAPPER, sessionId, limit);
        return newestFirst.reversed();
    }
}
