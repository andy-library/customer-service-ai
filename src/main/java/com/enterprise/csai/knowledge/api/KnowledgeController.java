package com.enterprise.csai.knowledge.api;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.DocumentEntity;
import com.enterprise.csai.knowledge.DocumentIngestService;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.ObjectProvider;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final ObjectProvider<DocumentIngestService> documentIngestService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final CsaiProperties properties;

    public KnowledgeController(
            ObjectProvider<DocumentIngestService> documentIngestService,
            KnowledgeSearchService knowledgeSearchService,
            CsaiProperties properties) {
        this.documentIngestService = documentIngestService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.properties = properties;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", knowledgeSearchService.activeProvider());
        m.put("modelSource", properties.getModelSource());
        m.put("localIngestEnabled", documentIngestService.getIfAvailable() != null);
        if (properties.getKnowledge().isDify()) {
            m.put("difyBaseUrl", properties.getKnowledge().getDify().getBaseUrl());
            m.put("difyDatasetConfigured",
                    properties.getKnowledge().getDify().getDatasetId() != null
                            && !properties.getKnowledge().getDify().getDatasetId().isBlank());
        }
        return m;
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentEntity upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title) {
        return requireLocalIngest().ingest(file, title);
    }

    @GetMapping("/documents")
    public List<DocumentEntity> list() {
        return requireLocalIngest().list();
    }

    @GetMapping("/documents/{id}")
    public DocumentEntity get(@PathVariable UUID id) {
        return requireLocalIngest().get(id);
    }

    @DeleteMapping("/documents/{id}")
    public void delete(@PathVariable UUID id) {
        requireLocalIngest().delete(id);
    }

    @PostMapping("/search")
    public List<KnowledgeChunk> search(@Valid @RequestBody SearchRequestBody body) {
        return knowledgeSearchService.search(body.query(), body.topK());
    }

    private DocumentIngestService requireLocalIngest() {
        DocumentIngestService svc = documentIngestService.getIfAvailable();
        if (svc == null) {
            throw new IllegalStateException(
                    "Local document ingest is disabled (csai.knowledge.provider="
                            + properties.getKnowledge().getProvider()
                            + "). Manage documents in Dify when provider=dify.");
        }
        return svc;
    }

    public record SearchRequestBody(@NotBlank String query, Integer topK) {
    }
}
