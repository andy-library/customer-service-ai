package com.enterprise.csai.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiResponseContractTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("csai")
            .withUsername("csai")
            .withPassword("csai");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.ai.openai.api-key", () -> "test-key");
        registry.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:9");
        registry.add("spring.ai.vectorstore.pgvector.initialize-schema", () -> "true");
        registry.add("spring.ai.vectorstore.pgvector.dimensions", () -> "1536");
        // Avoid outbound embedding calls during context load if auto-config probes
        registry.add("csai.embedding.api-key", () -> "test-key");
        registry.add("csai.embedding.base-url", () -> "http://127.0.0.1:9");
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void pingIsWrappedAsApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.message").value("pong"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}

