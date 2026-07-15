package com.enterprise.csai.router;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.modelgateway.ModelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    ModelGateway modelGateway;

    CsaiProperties properties;
    RoutingService routingService;

    @BeforeEach
    void setUp() {
        properties = new CsaiProperties();
        properties.getRouter().setClassifierModelId("classifier-default");
        properties.getRouter().setDefaultAnswerModelId("answer-fast");
        Map<String, String> mapping = new HashMap<>();
        mapping.put("PRODUCT", "answer-strong");
        mapping.put("BILLING", "answer-strong");
        mapping.put("TECH_SUPPORT", "answer-strong");
        mapping.put("POLICY", "answer-strong");
        mapping.put("CHITCHAT", "answer-fast");
        mapping.put("UNKNOWN", "answer-fast");
        properties.getRouter().setIntentModelMapping(mapping);
        routingService = new RoutingService(modelGateway, properties);
    }

    @Test
    void mapsChitchatToFastModelWithoutRag() {
        when(modelGateway.chat(eq("classifier-default"), any()))
                .thenReturn("{\"intent\":\"CHITCHAT\",\"confidence\":0.8,\"reason\":\"hi\"}");

        RoutingDecision d = routingService.route("你好");

        assertThat(d.intent()).isEqualTo(IntentType.CHITCHAT);
        assertThat(d.answerModelId()).isEqualTo("answer-fast");
        assertThat(d.ragEnabled()).isFalse();
    }

    @Test
    void mapsBillingToStrongModelWithRag() {
        when(modelGateway.chat(eq("classifier-default"), any()))
                .thenReturn("{\"intent\":\"BILLING\",\"confidence\":0.95,\"reason\":\"refund\"}");

        RoutingDecision d = routingService.route("如何退款");

        assertThat(d.intent()).isEqualTo(IntentType.BILLING);
        assertThat(d.answerModelId()).isEqualTo("answer-strong");
        assertThat(d.ragEnabled()).isTrue();
    }

    @Test
    void degradesWhenClassifierThrows() {
        when(modelGateway.chat(eq("classifier-default"), any(List.class)))
                .thenThrow(new RuntimeException("upstream down"));

        RoutingDecision d = routingService.route("anything");

        assertThat(d.intent()).isEqualTo(IntentType.UNKNOWN);
        assertThat(d.answerModelId()).isEqualTo("answer-fast");
        assertThat(d.ragEnabled()).isTrue();
        assertThat(d.reason()).isEqualTo("classifier_failed");
    }
}
