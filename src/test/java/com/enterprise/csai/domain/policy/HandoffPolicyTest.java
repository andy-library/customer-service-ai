package com.enterprise.csai.domain.policy;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.router.IntentType;
import com.enterprise.csai.router.RoutingDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HandoffPolicyTest {

    @Test
    void userRequestTriggersHandoff() {
        CsaiProperties props = new CsaiProperties();
        HandoffPolicy policy = new HandoffPolicy(props);
        RoutingDecision d = new RoutingDecision(
                IntentType.BILLING, 0.99, "ok", "c", "a", true);
        assertThat(policy.evaluate(d, "请转人工").handoff()).isTrue();
        assertThat(policy.evaluate(d, "请转人工").reason()).isEqualTo("USER_REQUEST");
    }

    @Test
    void lowConfidenceTriggersHandoff() {
        CsaiProperties props = new CsaiProperties();
        props.getRouter().setConfidenceThreshold(0.55);
        HandoffPolicy policy = new HandoffPolicy(props);
        RoutingDecision d = new RoutingDecision(
                IntentType.PRODUCT, 0.2, "weak", "c", "a", true);
        assertThat(policy.evaluate(d, "什么").handoff()).isTrue();
        assertThat(policy.evaluate(d, "什么").reason()).isEqualTo("LOW_CONFIDENCE");
    }
}
