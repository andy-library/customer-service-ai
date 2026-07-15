package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves effective chat/embedding endpoints for {@code local} vs {@code cloud} model-source.
 */
@Component
public class ActiveModelProfileResolver {

    private final CsaiProperties properties;

    public ActiveModelProfileResolver(CsaiProperties properties) {
        this.properties = properties;
    }

    public String modelSource() {
        return properties.getModelSource() == null ? "local" : properties.getModelSource();
    }

    public List<CsaiProperties.ModelConfig> resolveChatModels() {
        if (properties.isCloudModelSource()) {
            return buildCloudChatModels();
        }
        // local / default: use csai.models from configuration
        return properties.getModels();
    }

    public CsaiProperties.EmbeddingConfig resolveEmbedding() {
        if (!properties.isCloudModelSource()) {
            return properties.getEmbedding();
        }
        CsaiProperties.EndpointBundle cloud = properties.getCloud();
        CsaiProperties.EmbeddingConfig emb = new CsaiProperties.EmbeddingConfig();
        String base = blankTo(cloud.getEmbeddingBaseUrl(), cloud.getBaseUrl());
        String key = blankTo(cloud.getEmbeddingApiKey(), cloud.getApiKey());
        emb.setBaseUrl(base);
        emb.setApiKey(key == null || key.isBlank() ? "sk-placeholder" : key);
        emb.setModelName(cloud.getEmbeddingModel());
        emb.setDimensions(cloud.getEmbeddingDimensions());
        return emb;
    }

    private List<CsaiProperties.ModelConfig> buildCloudChatModels() {
        CsaiProperties.EndpointBundle cloud = properties.getCloud();
        String base = cloud.getBaseUrl();
        String key = cloud.getApiKey() == null || cloud.getApiKey().isBlank()
                ? "sk-placeholder" : cloud.getApiKey();
        List<CsaiProperties.ModelConfig> list = new ArrayList<>();
        list.add(model("classifier-default", "Classifier (cloud)", base, key,
                cloud.getClassifierModel(), ModelRole.CLASSIFIER));
        list.add(model("answer-strong", "Answer Strong (cloud)", base, key,
                cloud.getAnswerStrongModel(), ModelRole.ANSWER));
        list.add(model("answer-fast", "Answer Fast (cloud)", base, key,
                cloud.getAnswerFastModel(), ModelRole.ANSWER));
        return list;
    }

    private static CsaiProperties.ModelConfig model(
            String id, String display, String base, String key, String modelName, ModelRole role) {
        CsaiProperties.ModelConfig c = new CsaiProperties.ModelConfig();
        c.setId(id);
        c.setDisplayName(display);
        c.setBaseUrl(base);
        c.setApiKey(key);
        c.setModelName(modelName);
        c.setRole(role);
        c.setEnabled(true);
        return c;
    }

    private static String blankTo(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
