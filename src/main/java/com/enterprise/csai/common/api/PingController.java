package com.enterprise.csai.common.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness probe for API contract checks.
 * <p>
 * Returns a JSON object (not a bare String) so framework {@code ResponseBodyAdvice}
 * wraps it as {@code ApiResponse}. Bare String responses skip wrapping by design.
 */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("pong");
    }

    public record PingResponse(String message) {
    }
}
