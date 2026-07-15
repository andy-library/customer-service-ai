package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeSearchService {

    private final VectorStore vectorStore;
    private final CsaiProperties properties;

    public KnowledgeSearchService(VectorStore vectorStore, CsaiProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public List<KnowledgeChunk> search(String query, Integer topK) {
        int k = topK != null && topK > 0 ? topK : properties.getRag().getTopK();
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
