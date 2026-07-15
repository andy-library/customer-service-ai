package com.enterprise.csai.chat;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChatRequest(
        UUID sessionId,
        @NotBlank String message,
        ChatOptions options
) {
}
