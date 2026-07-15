package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade over the active {@link KnowledgeRetriever} (Dify / local / none).
 */
@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeRetriever retriever;
    private final CsaiProperties properties;

    public KnowledgeSearchService(List<KnowledgeRetriever> retrievers, CsaiProperties properties) {
        this.properties = properties;
        String wanted = properties.getKnowledge().getProvider();
        this.retriever = retrievers.stream()
                .filter(r -> r.provider().equalsIgnoreCase(wanted))
                .findFirst()
                .orElseGet(() -> retrievers.stream()
                        .filter(r -> "none".equalsIgnoreCase(r.provider()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No KnowledgeRetriever for provider=" + wanted
                                        + "; available=" + retrievers.stream().map(KnowledgeRetriever::provider).toList())));
        log.info("knowledge retriever active provider={}", retriever.provider());
    }

    public List<KnowledgeChunk> search(String query, Integer topK) {
        int k = topK != null && topK > 0 ? topK : properties.getRag().getTopK();
        return retriever.search(query, k);
    }

    public String activeProvider() {
        return retriever.provider();
    }
}
