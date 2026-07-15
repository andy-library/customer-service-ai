package com.enterprise.csai.knowledge;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepository {

    private static final RowMapper<DocumentEntity> MAPPER = DocumentRepository::mapRow;

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(DocumentEntity entity) {
        jdbcTemplate.update("""
                INSERT INTO cs_document
                (id, title, filename, content_type, status, chunk_count, error_message, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entity.getId(),
                entity.getTitle(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getStatus().name(),
                entity.getChunkCount(),
                entity.getErrorMessage(),
                toTimestamp(entity.getCreatedAt()),
                toTimestamp(entity.getUpdatedAt()));
    }

    public void updateStatus(UUID id, DocumentStatus status, int chunkCount, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE cs_document
                SET status = ?, chunk_count = ?, error_message = ?, updated_at = NOW()
                WHERE id = ?
                """,
                status.name(), chunkCount, errorMessage, id);
    }

    public Optional<DocumentEntity> findById(UUID id) {
        List<DocumentEntity> list = jdbcTemplate.query(
                "SELECT * FROM cs_document WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public List<DocumentEntity> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM cs_document ORDER BY created_at DESC", MAPPER);
    }

    public boolean deleteById(UUID id) {
        return jdbcTemplate.update("DELETE FROM cs_document WHERE id = ?", id) > 0;
    }

    private static DocumentEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(rs.getObject("id", UUID.class));
        entity.setTitle(rs.getString("title"));
        entity.setFilename(rs.getString("filename"));
        entity.setContentType(rs.getString("content_type"));
        entity.setStatus(DocumentStatus.valueOf(rs.getString("status")));
        entity.setChunkCount(rs.getInt("chunk_count"));
        entity.setErrorMessage(rs.getString("error_message"));
        entity.setCreatedAt(toOffset(rs.getTimestamp("created_at")));
        entity.setUpdatedAt(toOffset(rs.getTimestamp("updated_at")));
        return entity;
    }

    private static Timestamp toTimestamp(OffsetDateTime time) {
        return time == null ? null : Timestamp.from(time.toInstant());
    }

    private static OffsetDateTime toOffset(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}
