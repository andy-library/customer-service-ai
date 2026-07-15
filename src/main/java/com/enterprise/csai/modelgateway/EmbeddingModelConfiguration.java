package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Builds EmbeddingModel from {@code csai.embedding.*} so base-url/api-key
 * align with the multi-model gateway (not only spring.ai.openai defaults).
 */
@Configuration
@Profile("!mock")
public class EmbeddingModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelConfiguration.class);

    @Bean
    @Primary
    EmbeddingModel csaiEmbeddingModel(ActiveModelProfileResolver profileResolver) {
        CsaiProperties.EmbeddingConfig emb = profileResolver.resolveEmbedding();
        String baseUrl = ModelGatewayConfiguration.normalizeBaseUrl(emb.getBaseUrl());
        OpenAiApi api = OpenAiCompatibleClients.create(emb.getBaseUrl(), emb.getApiKey());
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(emb.getModelName())
                .dimensions(emb.getDimensions())
                .build();
        log.info("embedding model ready source={} model={} baseUrl={} dimensions={}",
                profileResolver.modelSource(), emb.getModelName(), baseUrl, emb.getDimensions());
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}

