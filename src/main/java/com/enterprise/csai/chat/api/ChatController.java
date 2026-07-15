package com.enterprise.csai.chat.api;

import com.enterprise.csai.chat.ChatOrchestrator;
import com.enterprise.csai.chat.ChatRequest;
import com.enterprise.csai.chat.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatOrchestrator chatOrchestrator;

    public ChatController(ChatOrchestrator chatOrchestrator) {
        this.chatOrchestrator = chatOrchestrator;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatOrchestrator.chat(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        return chatOrchestrator.stream(request);
    }
}
