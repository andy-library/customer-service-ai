package com.enterprise.csai.modelgateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleClientsTest {

    @Test
    void dashscopeCompatibleModeUsesPathsWithoutExtraV1() {
        String base = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        assertThat(OpenAiCompatibleClients.completionsPathFor(base)).isEqualTo("/chat/completions");
        assertThat(OpenAiCompatibleClients.embeddingsPathFor(base)).isEqualTo("/embeddings");
    }

    @Test
    void officialOpenAiUsesV1Paths() {
        String base = "https://api.openai.com";
        assertThat(OpenAiCompatibleClients.completionsPathFor(base)).isEqualTo("/v1/chat/completions");
        assertThat(OpenAiCompatibleClients.embeddingsPathFor(base)).isEqualTo("/v1/embeddings");
    }
}
