package com.enterprise.csai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CsaiMetrics {

    private final MeterRegistry registry;

    public CsaiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordChat(
            String intent,
            String status,
            boolean degraded,
            boolean handoff,
            long latencyMs) {
        Counter.builder("csai.chat.requests")
                .tag("intent", safe(intent))
                .tag("status", safe(status))
                .tag("degraded", String.valueOf(degraded))
                .tag("handoff", String.valueOf(handoff))
                .register(registry)
                .increment();
        Timer.builder("csai.chat.latency")
                .tag("intent", safe(intent))
                .register(registry)
                .record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordKnowledge(String provider, String outcome, boolean fallback) {
        Counter.builder("csai.knowledge.search")
                .tag("provider", safe(provider))
                .tag("outcome", safe(outcome))
                .tag("fallback", String.valueOf(fallback))
                .register(registry)
                .increment();
    }

    public void recordModel(String modelId, String outcome) {
        Counter.builder("csai.model.invoke")
                .tag("modelId", safe(modelId))
                .tag("outcome", safe(outcome))
                .register(registry)
                .increment();
    }

    private static String safe(String v) {
        return v == null || v.isBlank() ? "unknown" : v;
    }
}
