package com.enterprise.csai.router;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses classifier model output into a structured result.
 * Tolerates surrounding markdown fences and invalid JSON (degrades to UNKNOWN).
 */
public final class IntentJsonParser {

    private static final Logger log = LoggerFactory.getLogger(IntentJsonParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IntentJsonParser() {
    }

    public static ClassificationResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ClassificationResult(IntentType.UNKNOWN, 0.0, "empty_classifier_output");
        }
        String json = extractJsonObject(raw.trim());
        try {
            JsonNode node = MAPPER.readTree(json);
            IntentType intent = IntentType.fromString(text(node, "intent"));
            double confidence = node.path("confidence").isNumber()
                    ? node.path("confidence").asDouble()
                    : 0.0;
            String reason = text(node, "reason");
            if (reason == null || reason.isBlank()) {
                reason = "n/a";
            }
            confidence = Math.max(0.0, Math.min(1.0, confidence));
            return new ClassificationResult(intent, confidence, reason);
        } catch (Exception ex) {
            log.debug("failed to parse classifier JSON: {}", raw, ex);
            return new ClassificationResult(IntentType.UNKNOWN, 0.0, "invalid_classifier_json");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    static String extractJsonObject(String raw) {
        String cleaned = raw;
        if (cleaned.startsWith("```")) {
            int firstNl = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                cleaned = cleaned.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
