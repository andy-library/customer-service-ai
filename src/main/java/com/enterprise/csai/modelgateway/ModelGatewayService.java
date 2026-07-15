package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.enterprise.csai.domain.port.ModelPort;
import com.enterprise.csai.observability.CsaiMetrics;
import com.microservice.framework.web.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ModelGatewayService implements ModelGateway, ModelPort {

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayService.class);

    private final ModelRegistry modelRegistry;
    private final CsaiProperties properties;
    private final CsaiMetrics metrics;

    public ModelGatewayService(
            ModelRegistry modelRegistry,
            CsaiProperties properties,
            CsaiMetrics metrics) {
        this.modelRegistry = modelRegistry;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public String chat(String modelId, List<Message> messages) {
        Duration timeout = Duration.ofMillis(properties.getResilience().getAnswerTimeoutMs());
        if (modelId != null && modelId.toLowerCase().contains("classifier")) {
            timeout = Duration.ofMillis(properties.getResilience().getClassifierTimeoutMs());
        }
        return chat(modelId, messages, timeout);
    }

    @Override
    public String chat(String modelId, List<Message> messages, Duration timeout) {
        ChatModel model = modelRegistry.requireChatModel(modelId);
        long ms = timeout == null ? properties.getResilience().getAnswerTimeoutMs() : timeout.toMillis();
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> invoke(model, modelId, messages));
            String text = future.get(ms, TimeUnit.MILLISECONDS);
            metrics.recordModel(modelId, "ok");
            return text;
        } catch (TimeoutException ex) {
            metrics.recordModel(modelId, "timeout");
            log.warn("model timeout modelId={} timeoutMs={}", modelId, ms);
            throw new BusinessException(CsaiErrorCodes.MODEL_TIMEOUT,
                    "model timeout after " + ms + "ms: " + modelId, ex);
        } catch (ExecutionException ex) {
            metrics.recordModel(modelId, "error");
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof BusinessException be) {
                throw be;
            }
            log.warn("model invoke failed modelId={}", modelId, cause);
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "model invoke failed: " + modelId + " — " + cause.getMessage(), cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.recordModel(modelId, "interrupted");
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "model invoke interrupted: " + modelId, ex);
        } catch (BusinessException ex) {
            metrics.recordModel(modelId, "error");
            throw ex;
        } catch (Exception ex) {
            metrics.recordModel(modelId, "error");
            log.warn("model invoke failed modelId={}", modelId, ex);
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "model invoke failed: " + modelId + " — " + ex.getMessage(), ex);
        }
    }

    private String invoke(ChatModel model, String modelId, List<Message> messages) {
        ChatResponse response = model.call(new Prompt(messages));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BusinessException(CsaiErrorCodes.MODEL_INVOKE_FAILED,
                    "empty response from model: " + modelId);
        }
        String text = response.getResult().getOutput().getText();
        return text != null ? text : "";
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
                    .doOnComplete(() -> metrics.recordModel(modelId, "stream_ok"))
                    .doOnError(err -> metrics.recordModel(modelId, "stream_error"))
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

    @Override
    public List<ModelView> listModels() {
        return modelRegistry.listModels();
    }
}
