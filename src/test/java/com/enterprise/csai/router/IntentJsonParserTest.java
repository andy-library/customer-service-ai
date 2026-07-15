package com.enterprise.csai.router;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentJsonParserTest {

    @Test
    void parsesValidJson() {
        String raw = "{\"intent\":\"PRODUCT\",\"confidence\":0.91,\"reason\":\"product feature\"}";
        ClassificationResult r = IntentJsonParser.parse(raw);
        assertThat(r.intent()).isEqualTo(IntentType.PRODUCT);
        assertThat(r.confidence()).isEqualTo(0.91);
        assertThat(r.reason()).contains("product");
    }

    @Test
    void invalidJsonBecomesUnknown() {
        ClassificationResult r = IntentJsonParser.parse("not-json");
        assertThat(r.intent()).isEqualTo(IntentType.UNKNOWN);
    }

    @Test
    void stripsMarkdownFence() {
        String raw = """
                ```json
                {"intent":"BILLING","confidence":0.8,"reason":"refund"}
                ```
                """;
        ClassificationResult r = IntentJsonParser.parse(raw);
        assertThat(r.intent()).isEqualTo(IntentType.BILLING);
    }
}
