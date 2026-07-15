package com.enterprise.csai.security;

import com.enterprise.csai.common.config.CsaiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthFilterTest {

    CsaiProperties properties;
    ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        properties = new CsaiProperties();
        properties.getSecurity().setEnabled(true);
        CsaiProperties.ApiKeyConfig client = new CsaiProperties.ApiKeyConfig();
        client.setId("client-1");
        client.setKey("key-client");
        client.setRoles(List.of("CLIENT"));
        CsaiProperties.ApiKeyConfig admin = new CsaiProperties.ApiKeyConfig();
        admin.setId("admin-1");
        admin.setKey("key-admin");
        admin.setRoles(List.of("ADMIN", "CLIENT"));
        properties.getSecurity().setApiKeys(List.of(client, admin));
        filter = new ApiKeyAuthFilter(properties, new ObjectMapper());
    }

    @Test
    void rejectsMissingKey() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/chat");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void acceptsValidKey() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/chat");
        req.addHeader("X-API-Key", "key-client");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsClientOnAdmin() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin");
        req.addHeader("X-API-Key", "key-client");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        assertThat(resp.getStatus()).isEqualTo(403);
    }
}
