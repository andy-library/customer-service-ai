package com.enterprise.csai.router;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.modelgateway.ModelGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    static final String CLASSIFIER_SYSTEM_PROMPT = """
            You are an intent classifier for enterprise customer support.
            Classify the user message into exactly one intent:
            PRODUCT, BILLING, TECH_SUPPORT, POLICY, CHITCHAT, UNKNOWN.
            Reply with ONLY compact JSON (no markdown):
            {"intent":"...","confidence":0.0,"reason":"..."}
            """;

    private final ModelGateway modelGateway;
    private final CsaiProperties properties;

    public RoutingService(ModelGateway modelGateway, CsaiProperties properties) {
        this.modelGateway = modelGateway;
        this.properties = properties;
    }

    public RoutingDecision route(String userQuery) {
        String classifierId = properties.getRouter().getClassifierModelId();
        String defaultAnswerId = properties.getRouter().getDefaultAnswerModelId();
        try {
            String raw = modelGateway.chat(classifierId, List.of(
                    new SystemMessage(CLASSIFIER_SYSTEM_PROMPT),
                    new UserMessage(userQuery == null ? "" : userQuery)
            ));
            ClassificationResult cr = IntentJsonParser.parse(raw);
            String answerId = resolveAnswerModel(cr.intent());
            boolean rag = properties.getRouter().isForceRag() || cr.intent().requiresKnowledge();
            return new RoutingDecision(
                    cr.intent(),
                    cr.confidence(),
                    cr.reason(),
                    classifierId,
                    answerId,
                    rag
            );
        } catch (Exception ex) {
            log.warn("classifier failed, degrading to UNKNOWN classifierId={}", classifierId, ex);
            return new RoutingDecision(
                    IntentType.UNKNOWN,
                    0.0,
                    "classifier_failed",
                    classifierId,
                    defaultAnswerId,
                    true
            );
        }
    }

    private String resolveAnswerModel(IntentType intent) {
        Map<String, String> mapping = properties.getRouter().getIntentModelMapping();
        if (mapping != null && mapping.containsKey(intent.name())) {
            return mapping.get(intent.name());
        }
        return properties.getRouter().getDefaultAnswerModelId();
    }
}
