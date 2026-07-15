package com.enterprise.csai.chat;

public record ChatOptions(
        Boolean forceRag,
        String overrideAnswerModelId
) {
}
