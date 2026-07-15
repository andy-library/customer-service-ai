package com.enterprise.csai.domain.port;

import com.enterprise.csai.modelgateway.ModelView;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * Port for multi-model chat invocation.
 */
public interface ModelPort {

    String chat(String modelId, List<Message> messages);

    String chat(String modelId, List<Message> messages, Duration timeout);

    Flux<String> stream(String modelId, List<Message> messages);

    List<ModelView> listModels();
}
