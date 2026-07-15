package com.enterprise.csai.chat;

public record SourceDto(
        String documentId,
        String title,
        String snippet,
        double score
) {
}
