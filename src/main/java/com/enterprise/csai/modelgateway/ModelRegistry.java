package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.microservice.framework.web.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelRegistry {

    private final Map<String, ChatModel> chatModels = new ConcurrentHashMap<>();
    private final Map<String, CsaiProperties.ModelConfig> definitions = new ConcurrentHashMap<>();

    public void register(CsaiProperties.ModelConfig definition, ChatModel chatModel) {
        definitions.put(definition.getId(), definition);
        chatModels.put(definition.getId(), chatModel);
    }

    public Optional<ChatModel> getChatModel(String id) {
        return Optional.ofNullable(chatModels.get(id));
    }

    public ChatModel requireChatModel(String id) {
        return getChatModel(id).orElseThrow(() ->
                new BusinessException(CsaiErrorCodes.MODEL_NOT_FOUND, "model not found: " + id));
    }

    public Optional<CsaiProperties.ModelConfig> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public List<ModelView> listModels() {
        return definitions.values().stream()
                .map(d -> new ModelView(
                        d.getId(),
                        d.getDisplayName() != null ? d.getDisplayName() : d.getId(),
                        d.getModelName(),
                        d.getRole(),
                        d.isEnabled()))
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();
    }

    public int size() {
        return chatModels.size();
    }
}
