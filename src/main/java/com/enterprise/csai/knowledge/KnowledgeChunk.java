package com.enterprise.csai.knowledge;

public record KnowledgeChunk(
        String documentId,
        String title,
        String content,
        double score
) {
}
