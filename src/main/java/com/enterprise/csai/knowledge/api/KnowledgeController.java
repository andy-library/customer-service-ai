package com.enterprise.csai.knowledge.api;

import com.enterprise.csai.knowledge.DocumentEntity;
import com.enterprise.csai.knowledge.DocumentIngestService;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final DocumentIngestService documentIngestService;
    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeController(
            DocumentIngestService documentIngestService,
            KnowledgeSearchService knowledgeSearchService) {
        this.documentIngestService = documentIngestService;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentEntity upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        return documentIngestService.ingest(file, title);
    }

    @GetMapping("/documents")
    public List<DocumentEntity> list() {
        return documentIngestService.list();
    }

    @GetMapping("/documents/{id}")
    public DocumentEntity get(@PathVariable UUID id) {
        return documentIngestService.get(id);
    }

    @DeleteMapping("/documents/{id}")
    public void delete(@PathVariable UUID id) {
        documentIngestService.delete(id);
    }

    @PostMapping("/search")
    public List<KnowledgeChunk> search(@Valid @RequestBody SearchRequestBody body) {
        return knowledgeSearchService.search(body.query(), body.topK());
    }

    public record SearchRequestBody(@NotBlank String query, Integer topK) {
    }
}
