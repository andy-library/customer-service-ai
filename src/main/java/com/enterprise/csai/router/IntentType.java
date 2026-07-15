package com.enterprise.csai.router;

import java.util.Locale;

/**
 * Customer-service intent taxonomy for LLM classification.
 */
public enum IntentType {
    PRODUCT(true),
    BILLING(true),
    TECH_SUPPORT(true),
    POLICY(true),
    CHITCHAT(false),
    UNKNOWN(true);

    private final boolean requiresKnowledge;

    IntentType(boolean requiresKnowledge) {
        this.requiresKnowledge = requiresKnowledge;
    }

    public boolean requiresKnowledge() {
        return requiresKnowledge;
    }

    public static IntentType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return IntentType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
