package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Optional local RAG using Spring AI VectorStore + local embedding model.
 */
@Component
@ConditionalOnProperty(prefix = "csai.knowledge", name = "provider", havingValue = "local")
public class LocalVectorKnowledgeRetriever implements KnowledgeRetriever {

    private final VectorStore vectorStore;
    private final CsaiProperties properties;

    public LocalVectorKnowledgeRetriever(VectorStore vectorStore, CsaiProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public List<KnowledgeChunk> search(String query, int topK) {
        int k = topK > 0 ? topK : properties.getRag().getTopK();
        SearchRequest request = SearchRequest.builder()
                .query(query == null ? "" : query)
                .topK(k)
                .similarityThreshold(properties.getRag().getSimilarityThreshold())
                .build();
        List<Document> docs = vectorStore.similaritySearch(request);
        return docs.stream().map(this::toChunk).toList();
    }

    private KnowledgeChunk toChunk(Document doc) {
        Object documentId = doc.getMetadata().get("documentId");
        Object title = doc.getMetadata().get("title");
        double score = doc.getScore() != null ? doc.getScore() : 0.0;
        return new KnowledgeChunk(
                documentId != null ? documentId.toString() : "",
                title != null ? title.toString() : "",
                doc.getText() != null ? doc.getText() : "",
                score
        );
    }
}
