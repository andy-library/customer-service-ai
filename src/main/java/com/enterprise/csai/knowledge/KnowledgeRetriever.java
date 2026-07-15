package com.enterprise.csai.knowledge;

import java.util.List;

public interface KnowledgeRetriever {

    /**
     * Provider key matching {@code csai.knowledge.provider}: dify | local | none.
     */
    String provider();

    List<KnowledgeChunk> search(String query, int topK);

    /**
     * Detailed search including degraded signals (default wraps {@link #search}).
     */
    default KnowledgeSearchResult searchDetailed(String query, int topK) {
        return KnowledgeSearchResult.ok(search(query, topK));
    }
}
