package com.enterprise.csai.observability;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.modelgateway.ModelRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("csai")
public class CsaiHealthIndicator implements HealthIndicator {

    private final CsaiProperties properties;
    private final ModelRegistry modelRegistry;

    public CsaiHealthIndicator(CsaiProperties properties, ModelRegistry modelRegistry) {
        this.properties = properties;
        this.modelRegistry = modelRegistry;
    }

    @Override
    public Health health() {
        int models = modelRegistry.listModels().size();
        Health.Builder b = models > 0 ? Health.up() : Health.down();
        return b
                .withDetail("modelSource", properties.getModelSource())
                .withDetail("knowledgeProvider", properties.getKnowledge().getProvider())
                .withDetail("registeredModels", models)
                .withDetail("securityEnabled", properties.getSecurity().isEnabled())
                .build();
    }
}
