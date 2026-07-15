package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieves segments from a Dify Dataset via Dataset API.
 * <p>
 * {@code POST {baseUrl}/datasets/{datasetId}/retrieve}
 */
@Component
@ConditionalOnProperty(prefix = "csai.knowledge", name = "provider", havingValue = "dify", matchIfMissing = true)
public class DifyKnowledgeRetriever implements KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(DifyKnowledgeRetriever.class);

    private final CsaiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DifyKnowledgeRetriever(CsaiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String provider() {
        return "dify";
    }

    @Override
    public List<KnowledgeChunk> search(String query, int topK) {
        CsaiProperties.DifyConfig dify = properties.getKnowledge().getDify();
        if (dify.getApiKey() == null || dify.getApiKey().isBlank()
                || dify.getDatasetId() == null || dify.getDatasetId().isBlank()) {
            log.warn("Dify knowledge not configured (api-key/dataset-id empty); returning empty hits");
            return List.of();
        }
        String base = trimSlash(dify.getBaseUrl());
        String url = base + "/datasets/" + dify.getDatasetId() + "/retrieve";
        int k = topK > 0 ? topK : dify.getTopK();

        Map<String, Object> retrievalModel = new HashMap<>();
        retrievalModel.put("search_method", dify.getSearchMethod() == null ? "hybrid_search" : dify.getSearchMethod());
        retrievalModel.put("reranking_enable", false);
        retrievalModel.put("top_k", k);
        retrievalModel.put("score_threshold_enabled", dify.getScoreThreshold() > 0);
        if (dify.getScoreThreshold() > 0) {
            retrievalModel.put("score_threshold", dify.getScoreThreshold());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", query == null ? "" : query);
        body.put("retrieval_model", retrievalModel);

        try {
            String raw = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + dify.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseRecords(raw);
        } catch (Exception ex) {
            log.warn("Dify retrieve failed: {}", ex.getMessage());
            throw new IllegalStateException("Dify knowledge retrieve failed: " + ex.getMessage(), ex);
        }
    }

    private List<KnowledgeChunk> parseRecords(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(raw);
        JsonNode records = root.path("records");
        if (!records.isArray()) {
            // some versions nest under data
            records = root.path("data").path("records");
        }
        List<KnowledgeChunk> chunks = new ArrayList<>();
        if (!records.isArray()) {
            return chunks;
        }
        for (JsonNode rec : records) {
            JsonNode segment = rec.path("segment");
            String content = text(segment, "content");
            if (content == null || content.isBlank()) {
                content = text(rec, "content");
            }
            String docId = text(segment.path("document"), "id");
            if (docId == null) {
                docId = text(rec.path("document"), "id");
            }
            String title = text(segment.path("document"), "name");
            if (title == null) {
                title = text(rec.path("document"), "name");
            }
            if (title == null) {
                title = text(segment, "document_name");
            }
            double score = rec.path("score").isNumber() ? rec.path("score").asDouble() : 0.0;
            chunks.add(new KnowledgeChunk(
                    docId != null ? docId : "",
                    title != null ? title : "",
                    content != null ? content : "",
                    score));
        }
        return chunks;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        String t = base.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
