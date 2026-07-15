package com.enterprise.csai.modelgateway.mock;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.modelgateway.ModelRegistry;
import com.enterprise.csai.modelgateway.ModelRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Offline profile: deterministic chat/embedding/vector beans so CI can run without API keys.
 */
@Configuration
@Profile("mock")
public class MockModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MockModelConfiguration.class);

    @Bean
    @Primary
    EmbeddingModel mockEmbeddingModel(CsaiProperties properties) {
        int dim = properties.getEmbedding().getDimensions();
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                List<String> inputs = request.getInstructions();
                for (int i = 0; i < inputs.size(); i++) {
                    embeddings.add(new Embedding(deterministicVector(inputs.get(i), dim), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return deterministicVector(document.getText(), dim);
            }
        };
    }

    @Bean
    @Primary
    VectorStore mockVectorStore() {
        return new InMemoryVectorStore();
    }

    @Bean
    ApplicationRunner mockModelRegistryInitializer(CsaiProperties properties, ModelRegistry registry) {
        return args -> {
            for (CsaiProperties.ModelConfig model : properties.getModels()) {
                if (!model.isEnabled() || model.getRole() == ModelRole.EMBEDDING) {
                    continue;
                }
                registry.register(model, new MockChatModel(model.getId(), model.getRole()));
                log.info("mock registered model id={} role={}", model.getId(), model.getRole());
            }
            log.info("mock model registry ready size={}", registry.size());
        };
    }

    static float[] deterministicVector(String text, int dim) {
        float[] v = new float[dim];
        int hash = text == null ? 0 : text.hashCode();
        for (int i = 0; i < dim; i++) {
            int h = hash * 31 + i;
            v[i] = ((h % 1000) / 1000.0f);
        }
        // L2 normalize lightly
        double sum = 0;
        for (float x : v) {
            sum += x * x;
        }
        double norm = Math.sqrt(sum) + 1e-8;
        for (int i = 0; i < dim; i++) {
            v[i] = (float) (v[i] / norm);
        }
        return v;
    }

    static final class MockChatModel implements ChatModel {
        private final String id;
        private final ModelRole role;

        MockChatModel(String id, ModelRole role) {
            this.id = id;
            this.role = role;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = respond(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            String text = respond(prompt);
            // stream by characters in small chunks
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < text.length(); i += 8) {
                parts.add(text.substring(i, Math.min(text.length(), i + 8)));
            }
            return Flux.fromIterable(parts)
                    .map(part -> new ChatResponse(List.of(new Generation(new AssistantMessage(part)))));
        }

        private String respond(Prompt prompt) {
            String user = lastUser(prompt);
            if (role == ModelRole.CLASSIFIER) {
                return classifyJson(user);
            }
            String lower = user.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "你好", "hello", "hi")) {
                return "您好，我是智能客服（mock:" + id + "）。有什么可以帮您？";
            }
            String context = prompt.getInstructions().stream()
                    .filter(m -> m instanceof org.springframework.ai.chat.messages.SystemMessage)
                    .map(Message::getText)
                    .findFirst()
                    .orElse("");
            if (context.contains("7 日内") || context.contains("退款")) {
                return "根据知识库（mock:" + id + "），购买后 7 日内未激活可全额退款。";
            }
            if (context.contains("Knowledge snippets: (none)")) {
                return "知识库暂无足够依据（mock:" + id + "），建议补充相关文档后再询问。";
            }
            return "这是 mock 回答（model=" + id + "）：" + user;
        }

        private static String classifyJson(String user) {
            String lower = user == null ? "" : user.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "你好", "hello", "hi", "谢谢")) {
                return "{\"intent\":\"CHITCHAT\",\"confidence\":0.9,\"reason\":\"greeting\"}";
            }
            if (containsAny(lower, "退款", "账单", "支付", "发票")) {
                return "{\"intent\":\"BILLING\",\"confidence\":0.92,\"reason\":\"billing\"}";
            }
            if (containsAny(lower, "故障", "报错", "接口", "对接", "bug")) {
                return "{\"intent\":\"TECH_SUPPORT\",\"confidence\":0.9,\"reason\":\"tech\"}";
            }
            if (containsAny(lower, "政策", "条款", "流程")) {
                return "{\"intent\":\"POLICY\",\"confidence\":0.88,\"reason\":\"policy\"}";
            }
            return "{\"intent\":\"PRODUCT\",\"confidence\":0.8,\"reason\":\"default product\"}";
        }

        private static boolean containsAny(String text, String... keys) {
            for (String k : keys) {
                if (text.contains(k.toLowerCase(Locale.ROOT)) || text.contains(k)) {
                    return true;
                }
            }
            return false;
        }

        private static String lastUser(Prompt prompt) {
            List<Message> msgs = prompt.getInstructions();
            for (int i = msgs.size() - 1; i >= 0; i--) {
                if (msgs.get(i) instanceof UserMessage um) {
                    return um.getText();
                }
            }
            return "";
        }
    }

    /**
     * Minimal in-memory vector store for mock profile.
     */
    static final class InMemoryVectorStore implements VectorStore {
        private final List<Document> docs = new CopyOnWriteArrayList<>();

        @Override
        public void add(List<Document> documents) {
            docs.addAll(documents);
        }

        @Override
        public void delete(List<String> idList) {
            docs.removeIf(d -> idList.contains(d.getId()));
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            // Best-effort: clear all when filter delete is used in mock
            docs.clear();
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            String q = request.getQuery() == null ? "" : request.getQuery();
            return docs.stream()
                    .filter(d -> matches(q, d))
                    .limit(request.getTopK())
                    .map(d -> Document.builder()
                            .id(d.getId())
                            .text(d.getText())
                            .metadata(d.getMetadata())
                            .score(0.9)
                            .build())
                    .toList();
        }

        private static boolean matches(String query, Document d) {
            if (d.getText() == null) {
                return false;
            }
            if (query == null || query.isBlank()) {
                return true;
            }
            String hay = d.getText() + " " + d.getMetadata().getOrDefault("title", "");
            if (hay.contains(query)) {
                return true;
            }
            // token-ish overlap for Chinese/English mock retrieval
            for (String token : query.split("[\\s，。？?、！!/]+")) {
                if (token.length() >= 2 && hay.contains(token)) {
                    return true;
                }
            }
            // Chinese bigrams (no whitespace tokenization)
            for (int i = 0; i < query.length() - 1; i++) {
                String bi = query.substring(i, i + 2);
                if (bi.codePoints().allMatch(Character::isIdeographic) && hay.contains(bi)) {
                    return true;
                }
            }
            return false;
        }
    }
}

