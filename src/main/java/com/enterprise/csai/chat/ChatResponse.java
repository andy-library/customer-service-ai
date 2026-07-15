package com.enterprise.csai.chat;

import com.enterprise.csai.router.RoutingDecision;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID sessionId,
        String answer,
        RoutingDecision route,
        List<SourceDto> sources
) {
}
