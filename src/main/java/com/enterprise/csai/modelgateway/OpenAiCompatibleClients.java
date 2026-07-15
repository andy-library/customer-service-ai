package com.enterprise.csai.modelgateway;

import org.springframework.ai.openai.api.OpenAiApi;

/**
 * Helpers for OpenAI-compatible providers (DashScope, DeepSeek, etc.).
 * <p>
 * Official OpenAI uses base {@code https://api.openai.com} + path {@code /v1/chat/completions}.
 * Many vendors ship a base that already ends with {@code /v1} (e.g. DashScope
 * {@code .../compatible-mode/v1}). In that case paths must be {@code /chat/completions}
 * and {@code /embeddings} to avoid {@code .../v1/v1/...} 404s.
 */
public final class OpenAiCompatibleClients {

    private OpenAiCompatibleClients() {
    }

    public static OpenAiApi create(String baseUrl, String apiKey) {
        String normalized = ModelGatewayConfiguration.normalizeBaseUrl(baseUrl);
        return OpenAiApi.builder()
                .baseUrl(normalized)
                .apiKey(apiKey)
                .completionsPath(completionsPathFor(normalized))
                .embeddingsPath(embeddingsPathFor(normalized))
                .build();
    }

    /** Visible for tests: path selection when vendor base already ends with /v1. */
    static String completionsPathFor(String normalizedBaseUrl) {
        return baseIncludesV1(normalizedBaseUrl) ? "/chat/completions" : "/v1/chat/completions";
    }

    static String embeddingsPathFor(String normalizedBaseUrl) {
        return baseIncludesV1(normalizedBaseUrl) ? "/embeddings" : "/v1/embeddings";
    }

    private static boolean baseIncludesV1(String normalizedBaseUrl) {
        return normalizedBaseUrl != null && normalizedBaseUrl.matches("(?i).*/v1$");
    }
}

