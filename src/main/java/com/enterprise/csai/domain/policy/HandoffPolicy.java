package com.enterprise.csai.domain.policy;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.router.IntentType;
import com.enterprise.csai.router.RoutingDecision;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HandoffPolicy {

    private final CsaiProperties properties;

    public HandoffPolicy(CsaiProperties properties) {
        this.properties = properties;
    }

    public HandoffDecision evaluate(RoutingDecision decision, String userMessage) {
        if (userRequestsHuman(userMessage)) {
            return HandoffDecision.yes("USER_REQUEST");
        }
        // Classifier outage is degraded, not an immediate handoff — still try RAG answer.
        if ("classifier_failed".equals(decision.reason())) {
            return HandoffDecision.no();
        }
        double threshold = properties.getRouter().getConfidenceThreshold();
        if (decision.confidence() < threshold) {
            return HandoffDecision.yes("LOW_CONFIDENCE");
        }
        if (decision.intent() == IntentType.UNKNOWN
                && properties.getRouter().isHandoffOnUnknown()) {
            return HandoffDecision.yes("UNKNOWN_INTENT");
        }
        return HandoffDecision.no();
    }

    private static boolean userRequestsHuman(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase(Locale.ROOT);
        return m.contains("转人工")
                || m.contains("人工客服")
                || m.contains("真人")
                || m.contains("human agent")
                || m.contains("talk to human");
    }

    public record HandoffDecision(boolean handoff, String reason) {
        public static HandoffDecision yes(String reason) {
            return new HandoffDecision(true, reason);
        }

        public static HandoffDecision no() {
            return new HandoffDecision(false, null);
        }
    }
}
