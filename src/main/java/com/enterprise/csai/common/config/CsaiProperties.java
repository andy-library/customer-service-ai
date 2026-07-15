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

    @Valid
    private List<ModelConfig> models = new ArrayList<>();

    @Valid
    @NotNull
    private EmbeddingConfig embedding = new EmbeddingConfig();

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
