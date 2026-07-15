package com.enterprise.csai.common.api;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import com.enterprise.csai.modelgateway.ActiveModelProfileResolver;
import com.enterprise.csai.modelgateway.ModelRegistry;
import com.enterprise.csai.modelgateway.ModelView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime view of active model source and knowledge provider (no secrets).
 */
@RestController
@RequestMapping("/api/v1/runtime")
public class RuntimeConfigController {

    private final CsaiProperties properties;
    private final ActiveModelProfileResolver profileResolver;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ModelRegistry modelRegistry;

    public RuntimeConfigController(
            CsaiProperties properties,
            ActiveModelProfileResolver profileResolver,
            KnowledgeSearchService knowledgeSearchService,
            ModelRegistry modelRegistry) {
        this.properties = properties;
        this.profileResolver = profileResolver;
        this.knowledgeSearchService = knowledgeSearchService;
        this.modelRegistry = modelRegistry;
    }

    @GetMapping
    public Map<String, Object> runtime() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("modelSource", profileResolver.modelSource());
        m.put("knowledgeProvider", knowledgeSearchService.activeProvider());
        List<ModelView> models = modelRegistry.listModels();
        m.put("models", models);
        CsaiProperties.EmbeddingConfig emb = profileResolver.resolveEmbedding();
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put("baseUrl", emb.getBaseUrl());
        embedding.put("modelName", emb.getModelName());
        embedding.put("dimensions", emb.getDimensions());
        m.put("embedding", embedding);
        if (properties.getKnowledge().isDify()) {
            Map<String, Object> dify = new LinkedHashMap<>();
            dify.put("baseUrl", properties.getKnowledge().getDify().getBaseUrl());
            dify.put("datasetIdConfigured",
                    properties.getKnowledge().getDify().getDatasetId() != null
                            && !properties.getKnowledge().getDify().getDatasetId().isBlank());
            m.put("dify", dify);
        }
        return m;
    }
}
