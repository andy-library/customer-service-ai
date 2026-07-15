package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.microservice.framework.web.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ModelGatewayService implements ModelGateway {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayService.class);

    private final ModelRegistry modelRegistry;

    public ModelGatewayService(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public String chat(String modelId, List<Message> messages) {
        ChatModel model = modelRegistry.requireChatModel(modelId);
        try {
            ChatResponse response = model.call(new Prompt(messages));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                        "empty response from model: " + modelId);
            }
            String text = response.getResult().getOutput().getText();
            return text != null ? text : "";
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("model invoke failed modelId={}", modelId, ex);
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "model invoke failed: " + modelId + " — " + ex.getMessage(), ex);
        }
    }

    @Override
    public Flux<String> stream(String modelId, List<Message> messages) {
        ChatModel model = modelRegistry.requireChatModel(modelId);
        try {
            return model.stream(new Prompt(messages))
                    .mapNotNull(response -> {
                        if (response.getResult() == null || response.getResult().getOutput() == null) {
                            return null;
                        }
                        return response.getResult().getOutput().getText();
                    })
                    .filter(text -> text != null && !text.isEmpty())
                    .onErrorMap(ex -> new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                            "model stream failed: " + modelId + " — " + ex.getMessage(), ex));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("model stream failed modelId={}", modelId, ex);
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "model stream failed: " + modelId + " — " + ex.getMessage(), ex);
        }
    }
}
