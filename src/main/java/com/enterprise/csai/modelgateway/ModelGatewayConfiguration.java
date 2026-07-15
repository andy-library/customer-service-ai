package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers one OpenAI-compatible {@link ChatModel} per enabled csai.models entry.
 * Disabled under {@code mock} profile (see {@code MockModelConfiguration}).
 */
@Configuration
@Profile("!mock")
public class ModelGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayConfiguration.class);

    @Bean
    ApplicationRunner modelRegistryInitializer(
            ActiveModelProfileResolver profileResolver,
            ModelRegistry registry) {
        return args -> {
            log.info("model-source={}", profileResolver.modelSource());
            int registered = 0;
            for (CsaiProperties.ModelConfig model : profileResolver.resolveChatModels()) {
                if (!model.isEnabled()) {
                    log.info("skip disabled model id={}", model.getId());
                    continue;
                }
                if (model.getRole() == ModelRole.EMBEDDING) {
                    log.info("skip embedding role in chat registry id={}", model.getId());
                    continue;
                }
                ChatModel chatModel = buildChatModel(model);
                registry.register(model, chatModel);
                registered++;
                log.info("registered chat model id={} role={} modelName={} baseUrl={}",
                        model.getId(), model.getRole(), model.getModelName(),
                        ModelGatewayConfiguration.normalizeBaseUrl(model.getBaseUrl()));
            }
            log.info("model registry ready size={} source={}", registered, profileResolver.modelSource());
        };
    }

    static ChatModel buildChatModel(CsaiProperties.ModelConfig model) {
        OpenAiApi api = OpenAiCompatibleClients.create(model.getBaseUrl(), model.getApiKey());
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model.getModelName())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    /**
     * Spring AI OpenAiApi expects base URL without trailing slash; many vendors
     * also require the path root only (e.g. https://api.openai.com).
     */
    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
