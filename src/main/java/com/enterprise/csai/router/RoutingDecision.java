package com.enterprise.csai.router;

public record RoutingDecision(
        IntentType intent,
        double confidence,
        String reason,
        String classifierModelId,
        String answerModelId,
        boolean ragEnabled
) {
}
