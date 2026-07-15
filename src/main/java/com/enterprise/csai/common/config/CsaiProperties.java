package com.enterprise.csai.common.config;

import com.enterprise.csai.modelgateway.ModelRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "csai")
public class CsaiProperties {

    /**
     * Active model backend: {@code local} (llama.cpp) or {@code cloud} (Bailian etc.).
     */
    @NotBlank
    private String modelSource = "local";

    @Valid
    private List<ModelConfig> models = new ArrayList<>();

    @Valid
    @NotNull
    private EmbeddingConfig embedding = new EmbeddingConfig();

    /**
     * Optional cloud endpoints; used when {@link #modelSource} is {@code cloud}.
     */
    @Valid
    @NotNull
    private EndpointBundle cloud = new EndpointBundle();

    @Valid
    @NotNull
    private KnowledgeConfig knowledge = new KnowledgeConfig();

    @Valid
    @NotNull
    private RouterConfig router = new RouterConfig();

    @Valid
    @NotNull
    private RagConfig rag = new RagConfig();

    @Valid
    @NotNull
    private ChatConfig chat = new ChatConfig();

    @Valid
    @NotNull
    private UploadConfig upload = new UploadConfig();

    public String getModelSource() {
        return modelSource;
    }

    public void setModelSource(String modelSource) {
        this.modelSource = modelSource;
    }

    public boolean isCloudModelSource() {
        return modelSource != null && modelSource.equalsIgnoreCase("cloud");
    }

    public List<ModelConfig> getModels() {
        return models;
    }

    public void setModels(List<ModelConfig> models) {
        this.models = models;
    }

    public EmbeddingConfig getEmbedding() {
        return embedding;
    }

    public void setEmbedding(EmbeddingConfig embedding) {
        this.embedding = embedding;
    }

    public EndpointBundle getCloud() {
        return cloud;
    }

    public void setCloud(EndpointBundle cloud) {
        this.cloud = cloud;
    }

    public KnowledgeConfig getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(KnowledgeConfig knowledge) {
        this.knowledge = knowledge;
    }

    public RouterConfig getRouter() {
        return router;
    }

    public void setRouter(RouterConfig router) {
        this.router = router;
    }

    public RagConfig getRag() {
        return rag;
    }

    public void setRag(RagConfig rag) {
        this.rag = rag;
    }

    public ChatConfig getChat() {
        return chat;
    }

    public void setChat(ChatConfig chat) {
        this.chat = chat;
    }

    public UploadConfig getUpload() {
        return upload;
    }

    public void setUpload(UploadConfig upload) {
        this.upload = upload;
    }

    /**
     * Cloud OpenAI-compatible endpoints (pluggable).
     */
    public static class EndpointBundle {
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey = "";
        private String classifierModel = "glm-5.1";
        private String answerStrongModel = "glm-5.1";
        private String answerFastModel = "glm-5.1";
        private String embeddingBaseUrl = "";
        private String embeddingApiKey = "";
        private String embeddingModel = "text-embedding-v3";
        private int embeddingDimensions = 1024;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getClassifierModel() {
            return classifierModel;
        }

        public void setClassifierModel(String classifierModel) {
            this.classifierModel = classifierModel;
        }

        public String getAnswerStrongModel() {
            return answerStrongModel;
        }

        public void setAnswerStrongModel(String answerStrongModel) {
            this.answerStrongModel = answerStrongModel;
        }

        public String getAnswerFastModel() {
            return answerFastModel;
        }

        public void setAnswerFastModel(String answerFastModel) {
            this.answerFastModel = answerFastModel;
        }

        public String getEmbeddingBaseUrl() {
            return embeddingBaseUrl;
        }

        public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
            this.embeddingBaseUrl = embeddingBaseUrl;
        }

        public String getEmbeddingApiKey() {
            return embeddingApiKey;
        }

        public void setEmbeddingApiKey(String embeddingApiKey) {
            this.embeddingApiKey = embeddingApiKey;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public int getEmbeddingDimensions() {
            return embeddingDimensions;
        }

        public void setEmbeddingDimensions(int embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }
    }

    public static class KnowledgeConfig {
        /**
         * {@code dify} | {@code local} | {@code none}
         */
        @NotBlank
        private String provider = "dify";

        @Valid
        @NotNull
        private DifyConfig dify = new DifyConfig();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public DifyConfig getDify() {
            return dify;
        }

        public void setDify(DifyConfig dify) {
            this.dify = dify;
        }

        public boolean isDify() {
            return "dify".equalsIgnoreCase(provider);
        }

        public boolean isLocal() {
            return "local".equalsIgnoreCase(provider);
        }
    }

    public static class DifyConfig {
        private String baseUrl = "http://127.0.0.1/v1";
        private String apiKey = "";
        private String datasetId = "";
        @Min(1)
        private int topK = 5;
        private double scoreThreshold = 0.0;
        private String searchMethod = "hybrid_search";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(double scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }

        public String getSearchMethod() {
            return searchMethod;
        }

        public void setSearchMethod(String searchMethod) {
            this.searchMethod = searchMethod;
        }
    }

    public static class ModelConfig {
        @NotBlank
        private String id;
        private String displayName;
        @NotBlank
        private String baseUrl;
        @NotBlank
        private String apiKey;
        @NotBlank
        private String modelName;
        @NotNull
        private ModelRole role = ModelRole.ANSWER;
        private boolean enabled = true;
        @Min(1000)
        private long timeoutMs = 60_000L;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public ModelRole getRole() {
            return role;
        }

        public void setRole(ModelRole role) {
            this.role = role;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class EmbeddingConfig {
        @NotBlank
        private String baseUrl = "https://api.openai.com";
        @NotBlank
        private String apiKey = "sk-placeholder";
        @NotBlank
        private String modelName = "text-embedding-3-small";
        @Min(1)
        private int dimensions = 1024;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }

    public static class RouterConfig {
        @NotBlank
        private String classifierModelId = "classifier-default";
        @NotBlank
        private String defaultAnswerModelId = "answer-fast";
        private boolean forceRag;
        private Map<String, String> intentModelMapping = new HashMap<>();

        public String getClassifierModelId() {
            return classifierModelId;
        }

        public void setClassifierModelId(String classifierModelId) {
            this.classifierModelId = classifierModelId;
        }

        public String getDefaultAnswerModelId() {
            return defaultAnswerModelId;
        }

        public void setDefaultAnswerModelId(String defaultAnswerModelId) {
            this.defaultAnswerModelId = defaultAnswerModelId;
        }

        public boolean isForceRag() {
            return forceRag;
        }

        public void setForceRag(boolean forceRag) {
            this.forceRag = forceRag;
        }

        public Map<String, String> getIntentModelMapping() {
            return intentModelMapping;
        }

        public void setIntentModelMapping(Map<String, String> intentModelMapping) {
            this.intentModelMapping = intentModelMapping;
        }
    }

    public static class RagConfig {
        @Min(1)
        private int topK = 5;
        private double similarityThreshold = 0.50;
        @Min(100)
        private int chunkSize = 800;
        @Min(0)
        private int chunkOverlap = 100;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkOverlap() {
            return chunkOverlap;
        }

        public void setChunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
        }
    }

    public static class ChatConfig {
        @Min(0)
        private int historyMaxMessages = 6;

        public int getHistoryMaxMessages() {
            return historyMaxMessages;
        }

        public void setHistoryMaxMessages(int historyMaxMessages) {
            this.historyMaxMessages = historyMaxMessages;
        }
    }

    public static class UploadConfig {
        @Min(1)
        private long maxBytes = 10_485_760L;
        @NotEmpty
        private List<String> allowedExtensions = List.of("pdf", "md", "txt");

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public List<String> getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(List<String> allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }
    }
}
