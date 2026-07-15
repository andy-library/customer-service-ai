package com.enterprise.csai.router.api;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.router.IntentType;
import com.enterprise.csai.router.RoutingDecision;
import com.enterprise.csai.router.RoutingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/router")
public class RouterController {

    private final RoutingService routingService;
    private final CsaiProperties properties;

    public RouterController(RoutingService routingService, CsaiProperties properties) {
        this.routingService = routingService;
        this.properties = properties;
    }

    @PostMapping("/classify")
    public RoutingDecision classify(@Valid @RequestBody ClassifyRequest request) {
        return routingService.route(request.message());
    }

    @GetMapping("/intents")
    public Map<String, Object> intents() {
        List<Map<String, Object>> intents = Arrays.stream(IntentType.values())
                .map(intent -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("intent", intent.name());
                    item.put("requiresKnowledge", intent.requiresKnowledge());
                    item.put("answerModelId", properties.getRouter().getIntentModelMapping()
                            .getOrDefault(intent.name(), properties.getRouter().getDefaultAnswerModelId()));
                    return item;
                })
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("classifierModelId", properties.getRouter().getClassifierModelId());
        body.put("defaultAnswerModelId", properties.getRouter().getDefaultAnswerModelId());
        body.put("forceRag", properties.getRouter().isForceRag());
        body.put("intents", intents);
        return body;
    }

    public record ClassifyRequest(@NotBlank String message) {
    }
}
