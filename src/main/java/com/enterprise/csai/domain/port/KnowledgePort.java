package com.enterprise.csai.domain.port;

import com.enterprise.csai.knowledge.KnowledgeSearchResult;

/**
 * Port for enterprise knowledge retrieval (Dify / local / none).
 */
public interface KnowledgePort {

    String provider();

    KnowledgeSearchResult search(String query, int topK);
}
