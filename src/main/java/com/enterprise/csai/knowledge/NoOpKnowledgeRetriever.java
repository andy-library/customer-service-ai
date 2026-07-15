package com.enterprise.csai.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "csai.knowledge", name = "provider", havingValue = "none")
public class NoOpKnowledgeRetriever implements KnowledgeRetriever {

    @Override
    public String provider() {
        return "none";
    }

    @Override
    public List<KnowledgeChunk> search(String query, int topK) {
        return List.of();
    }
}
