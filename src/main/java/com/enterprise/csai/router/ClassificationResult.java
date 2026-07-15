package com.enterprise.csai.router;

public record ClassificationResult(
        IntentType intent,
        double confidence,
        String reason
) {
}
