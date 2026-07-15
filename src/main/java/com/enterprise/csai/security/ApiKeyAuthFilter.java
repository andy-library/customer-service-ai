package com.enterprise.csai.security;

import com.enterprise.csai.common.config.CsaiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final CsaiProperties properties;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(CsaiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.getSecurity().isEnabled()) {
            return true;
        }
        String path = path(request);
        return isPublic(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            String rawKey = extractKey(request);
            if (rawKey == null || rawKey.isBlank()) {
                unauthorized(response, "Missing API key (X-API-Key or Authorization: Bearer)");
                return;
            }
            Optional<CsaiProperties.ApiKeyConfig> match = properties.getSecurity().getApiKeys().stream()
                    .filter(k -> rawKey.equals(k.getKey()))
                    .findFirst();
            if (match.isEmpty()) {
                unauthorized(response, "Invalid API key");
                return;
            }
            CsaiProperties.ApiKeyConfig cfg = match.get();
            Set<String> roles = cfg.getRoles() == null ? Set.of("CLIENT")
                    : cfg.getRoles().stream().map(r -> r.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            ApiKeyPrincipal principal = new ApiKeyPrincipal(cfg.getId(), roles);
            if (path(request).startsWith("/admin") && !principal.isAdmin()) {
                forbidden(response, "Admin role required");
                return;
            }
            CsaiPrincipalHolder.set(principal);
            filterChain.doFilter(request, response);
        } finally {
            CsaiPrincipalHolder.clear();
        }
    }

    private static String extractKey(HttpServletRequest request) {
        String header = request.getHeader("X-API-Key");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private static boolean isPublic(String path) {
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.equals("/api/v1/ping")
                || path.equals("/error");
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    private void forbidden(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, message);
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status == 401 ? 401 : 403);
        body.put("message", message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
