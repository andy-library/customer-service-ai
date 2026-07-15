package com.enterprise.csai.web;

import com.enterprise.csai.common.api.PingController;
import com.enterprise.csai.security.ApiKeyAuthFilter;
import com.enterprise.csai.security.RateLimitFilter;
import com.microservice.framework.web.WebProperties;
import com.microservice.framework.web.autoconfigure.WebAutoConfiguration;
import com.microservice.framework.web.context.RequestIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Docker-free contract: framework ResponseBodyAdvice wraps controller return values.
 */
@WebMvcTest(
        controllers = PingController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {ApiKeyAuthFilter.class, RateLimitFilter.class}))
@Import(WebAutoConfiguration.class)
class PingWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WebProperties webProperties;

    @AfterEach
    void clearRequestId() {
        RequestIdContext.remove();
    }

    @Test
    void pingIsWrappedAsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/ping").header("X-Request-ID", "test-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(webProperties.getResponse().getSuccessCode()))
                .andExpect(jsonPath("$.data.message").value("pong"))
                .andExpect(jsonPath("$.message").value("success"));
    }
}
