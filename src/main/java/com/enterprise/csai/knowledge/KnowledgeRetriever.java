package com.enterprise.csai.knowledge;

import java.util.List;

/**
 * Pluggable knowledge retrieval: Dify (enterprise KB), local PgVector, or none.
 */
public interface KnowledgeRetriever {

    /**
     * Provider key matching {@code csai.knowledge.provider}: dify | local | none.
     */
    String provider();

    List<KnowledgeChunk> search(String query, int topK);
}
