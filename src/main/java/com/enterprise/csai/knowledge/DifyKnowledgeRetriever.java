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
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Retrieves segments from a Dify Dataset via Dataset API.
 * <p>
 * Primary: {@code POST {baseUrl}/datasets/{datasetId}/retrieve}
 * <p>
 * Fallback: list document segments and do lightweight keyword scoring when retrieve
 * fails (observed on Dify 1.16.0-rc1 SQLAlchemy session bug when hits exist).
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
        int k = topK > 0 ? topK : dify.getTopK();
        try {
            List<KnowledgeChunk> hits = retrieve(dify, query, k);
            if (!hits.isEmpty()) {
                return hits;
            }
            if (dify.isSegmentFallback()) {
                log.info("Dify retrieve returned empty; trying segment fallback");
                return segmentFallback(dify, query, k);
            }
            return hits;
        } catch (Exception ex) {
            if (dify.isSegmentFallback()) {
                log.warn("Dify retrieve failed ({}), using segment fallback", ex.getMessage());
                try {
                    return segmentFallback(dify, query, k);
                } catch (Exception fallbackEx) {
                    log.warn("Dify segment fallback failed: {}", fallbackEx.getMessage());
                    return List.of();
                }
            }
            log.warn("Dify retrieve failed: {}", ex.getMessage());
            throw new IllegalStateException("Dify knowledge retrieve failed: " + ex.getMessage(), ex);
        }
    }

    private List<KnowledgeChunk> retrieve(CsaiProperties.DifyConfig dify, String query, int k)
            throws Exception {
        String base = trimSlash(dify.getBaseUrl());
        String url = base + "/datasets/" + dify.getDatasetId() + "/retrieve";

        Map<String, Object> retrievalModel = new HashMap<>();
        retrievalModel.put("search_method",
                dify.getSearchMethod() == null || dify.getSearchMethod().isBlank()
                        ? "semantic_search" : dify.getSearchMethod());
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
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "HTTP " + ex.getStatusCode().value() + " " + truncate(ex.getResponseBodyAsString(), 200),
                    ex);
        }
    }

    /**
     * Fallback for Dify versions where /retrieve crashes after finding hits.
     * Lists documents + segments and ranks by simple token overlap.
     */
    private List<KnowledgeChunk> segmentFallback(CsaiProperties.DifyConfig dify, String query, int k)
            throws Exception {
        String base = trimSlash(dify.getBaseUrl());
        String docsUrl = base + "/datasets/" + dify.getDatasetId() + "/documents?page=1&limit=50";
        String docsRaw = getAuth(docsUrl, dify.getApiKey());
        JsonNode docsRoot = objectMapper.readTree(docsRaw);
        JsonNode data = docsRoot.path("data");
        if (!data.isArray()) {
            return List.of();
        }

        List<String> tokens = tokenize(query);
        List<KnowledgeChunk> scored = new ArrayList<>();
        for (JsonNode doc : data) {
            String docId = text(doc, "id");
            String title = text(doc, "name");
            if (docId == null || docId.isBlank()) {
                continue;
            }
            String segUrl = base + "/datasets/" + dify.getDatasetId()
                    + "/documents/" + docId + "/segments?page=1&limit=100";
            String segRaw = getAuth(segUrl, dify.getApiKey());
            JsonNode segRoot = objectMapper.readTree(segRaw);
            JsonNode segs = segRoot.path("data");
            if (!segs.isArray()) {
                continue;
            }
            for (JsonNode seg : segs) {
                if (seg.path("enabled").isBoolean() && !seg.path("enabled").asBoolean()) {
                    continue;
                }
                String content = text(seg, "content");
                if (content == null || content.isBlank()) {
                    continue;
                }
                double score = scoreOverlap(content, tokens);
                if (score <= 0 && (query == null || query.isBlank())) {
                    score = 0.1;
                }
                if (score > 0 || tokens.isEmpty()) {
                    scored.add(new KnowledgeChunk(
                            docId,
                            title != null ? title : "",
                            content,
                            score));
                }
            }
        }
        scored.sort(Comparator.comparingDouble(KnowledgeChunk::score).reversed());
        if (scored.size() > k) {
            return new ArrayList<>(scored.subList(0, k));
        }
        // If keyword overlap found nothing, still return top-k segments so RAG has context
        if (scored.isEmpty()) {
            return List.of();
        }
        return scored;
    }

    private String getAuth(String url, String apiKey) {
        try {
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "HTTP " + ex.getStatusCode().value() + " " + truncate(ex.getResponseBodyAsString(), 200),
                    ex);
        }
    }

    List<KnowledgeChunk> parseRecords(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(raw);
        if (root.has("code") && !"0".equals(root.path("code").asText())
                && root.path("status").asInt(0) >= 400) {
            throw new IllegalStateException(root.path("message").asText("dify error"));
        }
        JsonNode records = root.path("records");
        if (!records.isArray()) {
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

    private static List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        // Prefer meaningful CJK n-grams (2–4) + latin words; skip single-char noise.
        StringBuilder han = new StringBuilder();
        StringBuilder latin = new StringBuilder();
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                flushLatin(latin, tokens);
                han.append(c);
            } else if (Character.isLetterOrDigit(c) && c < 128) {
                flushHan(han, tokens);
                latin.append(c);
            } else {
                flushHan(han, tokens);
                flushLatin(latin, tokens);
            }
        }
        flushHan(han, tokens);
        flushLatin(latin, tokens);
        return tokens.stream().filter(t -> t.length() >= 2).distinct().toList();
    }

    private static void flushHan(StringBuilder han, List<String> tokens) {
        if (han.length() >= 2) {
            String s = han.toString();
            tokens.add(s);
            // also add 2-grams from the phrase for partial match
            for (int i = 0; i + 2 <= s.length(); i++) {
                tokens.add(s.substring(i, i + 2));
            }
        }
        han.setLength(0);
    }

    private static void flushLatin(StringBuilder latin, List<String> tokens) {
        if (latin.length() >= 2) {
            tokens.add(latin.toString());
        }
        latin.setLength(0);
    }

    private static double scoreOverlap(String content, List<String> tokens) {
        if (tokens.isEmpty()) {
            return 0.05;
        }
        String c = content.toLowerCase(Locale.ROOT);
        double score = 0;
        double weightSum = 0;
        for (String t : tokens) {
            double w = t.length() >= 4 ? 3.0 : (t.length() >= 3 ? 2.0 : 1.0);
            weightSum += w;
            if (c.contains(t)) {
                score += w;
            }
        }
        return weightSum == 0 ? 0 : score / weightSum;
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

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
