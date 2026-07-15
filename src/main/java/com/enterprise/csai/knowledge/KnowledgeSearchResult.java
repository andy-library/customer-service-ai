package com.enterprise.csai.knowledge;

import java.util.ArrayList;
import java.util.List;

public record KnowledgeSearchResult(
        List<KnowledgeChunk> chunks,
        boolean degraded,
        List<String> degradedReasons
) {
    public static KnowledgeSearchResult ok(List<KnowledgeChunk> chunks) {
        return new KnowledgeSearchResult(
                chunks == null ? List.of() : chunks,
                false,
                List.of());
    }

    public static KnowledgeSearchResult degraded(List<KnowledgeChunk> chunks, String reason) {
        List<String> reasons = new ArrayList<>();
        if (reason != null && !reason.isBlank()) {
            reasons.add(reason);
        }
        return new KnowledgeSearchResult(
                chunks == null ? List.of() : chunks,
                true,
                List.copyOf(reasons));
    }

    public KnowledgeSearchResult withReason(String reason) {
        List<String> reasons = new ArrayList<>(degradedReasons == null ? List.of() : degradedReasons);
        if (reason != null && !reason.isBlank() && !reasons.contains(reason)) {
            reasons.add(reason);
        }
        return new KnowledgeSearchResult(chunks, true, List.copyOf(reasons));
    }
}
