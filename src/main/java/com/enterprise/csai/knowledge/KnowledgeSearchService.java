package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.domain.port.KnowledgePort;
import com.enterprise.csai.observability.CsaiMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade over the active {@link KnowledgeRetriever} (Dify / local / none).
 */
@Service
public class KnowledgeSearchService implements KnowledgePort {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeRetriever retriever;
    private final CsaiProperties properties;
    private final CsaiMetrics metrics;

    public KnowledgeSearchService(
            List<KnowledgeRetriever> retrievers,
            CsaiProperties properties,
            CsaiMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
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
        return searchDetailed(query, topK).chunks();
    }

    public KnowledgeSearchResult searchDetailed(String query, Integer topK) {
        int k = topK != null && topK > 0 ? topK : properties.getRag().getTopK();
        KnowledgeSearchResult result = retriever.searchDetailed(query, k);
        boolean fallback = result.degraded()
                && result.degradedReasons() != null
                && result.degradedReasons().stream().anyMatch(r -> r.contains("FALLBACK"));
        String outcome = result.chunks().isEmpty() ? "empty" : "hit";
        if (result.degraded()) {
            outcome = "degraded";
        }
        metrics.recordKnowledge(retriever.provider(), outcome, fallback);
        return result;
    }

    @Override
    public String provider() {
        return retriever.provider();
    }

    @Override
    public KnowledgeSearchResult search(String query, int topK) {
        return searchDetailed(query, topK);
    }

    public String activeProvider() {
        return retriever.provider();
    }
}
