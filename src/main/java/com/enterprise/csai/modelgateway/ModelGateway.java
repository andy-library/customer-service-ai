package com.enterprise.csai.modelgateway;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Unified multi-model invocation API.
 */
public interface ModelGateway {

    String chat(String modelId, List<Message> messages);

    Flux<String> stream(String modelId, List<Message> messages);
}
