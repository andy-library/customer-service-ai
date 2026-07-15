package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelRegistryTest {

    @Test
    void listsEnabledModelsWithoutApiKeyField() {
        ModelRegistry registry = new ModelRegistry();
        registry.register(model("m1", "gpt-a", ModelRole.ANSWER), new StubChatModel());
        registry.register(model("m2", "gpt-b", ModelRole.CLASSIFIER), new StubChatModel());

        List<ModelView> views = registry.listModels();

        assertThat(views).hasSize(2);
        assertThat(views).extracting(ModelView::id).containsExactlyInAnyOrder("m1", "m2");
        // ModelView has no apiKey component — compile-time guarantee
        assertThat(views.getFirst().modelName()).isNotBlank();
    }

    @Test
    void requireChatModelThrowsWhenMissing() {
        ModelRegistry registry = new ModelRegistry();
        assertThatThrownBy(() -> registry.requireChatModel("missing"))
                .hasMessageContaining("missing");
    }

    @Test
    void normalizeBaseUrlStripsTrailingSlash() {
        assertThat(ModelGatewayConfiguration.normalizeBaseUrl("https://api.example.com/v1/"))
                .isEqualTo("https://api.example.com/v1");
    }

    private static CsaiProperties.ModelConfig model(String id, String modelName, ModelRole role) {
        CsaiProperties.ModelConfig config = new CsaiProperties.ModelConfig();
        config.setId(id);
        config.setDisplayName(id);
        config.setBaseUrl("https://example.com");
        config.setApiKey("secret-key-must-not-leak");
        config.setModelName(modelName);
        config.setRole(role);
        config.setEnabled(true);
        return config;
    }

    private static final class StubChatModel implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return null;
        }
    }
}
