package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.microservice.framework.web.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIngestService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    private final DocumentRepository documentRepository;
    private final TextExtractionService textExtractionService;
    private final VectorStore vectorStore;
    private final CsaiProperties properties;

    public DocumentIngestService(
            DocumentRepository documentRepository,
            TextExtractionService textExtractionService,
            VectorStore vectorStore,
            CsaiProperties properties) {
        this.documentRepository = documentRepository;
        this.textExtractionService = textExtractionService;
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public DocumentEntity ingest(MultipartFile file, String title) {
        validate(file);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        String resolvedTitle = (title == null || title.isBlank()) ? filename : title.trim();

        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        entity.setTitle(resolvedTitle);
        entity.setFilename(filename);
        entity.setContentType(file.getContentType());
        entity.setStatus(DocumentStatus.PENDING);
        entity.setChunkCount(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        documentRepository.insert(entity);

        try {
            byte[] bytes = file.getBytes();
            String text = textExtractionService.extract(filename, file.getContentType(), bytes);
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(properties.getRag().getChunkSize())
                    .withMinChunkSizeChars(Math.max(1, properties.getRag().getChunkOverlap()))
                    .withKeepSeparator(true)
                    .build();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", id.toString());
            metadata.put("title", resolvedTitle);
            metadata.put("filename", filename);
            Document source = new Document(text, metadata);
            List<Document> chunks = splitter.apply(List.of(source));
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                chunk.getMetadata().put("documentId", id.toString());
                chunk.getMetadata().put("title", resolvedTitle);
                chunk.getMetadata().put("filename", filename);
                chunk.getMetadata().put("chunkIndex", i);
            }
            if (!chunks.isEmpty()) {
                vectorStore.add(chunks);
            }
            documentRepository.updateStatus(id, DocumentStatus.INDEXED, chunks.size(), null);
            entity.setStatus(DocumentStatus.INDEXED);
            entity.setChunkCount(chunks.size());
            entity.setErrorMessage(null);
            return entity;
        } catch (BusinessException ex) {
            documentRepository.updateStatus(id, DocumentStatus.FAILED, 0, ex.getMessage());
            entity.setStatus(DocumentStatus.FAILED);
            entity.setErrorMessage(ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("document ingest failed id={}", id, ex);
            String message = ex.getMessage() != null ? ex.getMessage() : "ingest failed";
            documentRepository.updateStatus(id, DocumentStatus.FAILED, 0, message);
            entity.setStatus(DocumentStatus.FAILED);
            entity.setErrorMessage(message);
            throw new BusinessException(CsaiErrorCodes.DOCUMENT_INGEST_FAILED, message, ex);
        }
    }

    public List<DocumentEntity> list() {
        return documentRepository.findAll();
    }

    public DocumentEntity get(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CsaiErrorCodes.DOCUMENT_NOT_FOUND,
                        "document not found: " + id));
    }

    public void delete(UUID id) {
        DocumentEntity existing = get(id);
        try {
            var filter = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
                    .eq("documentId", existing.getId().toString())
                    .build();
            vectorStore.delete(filter);
        } catch (Exception ex) {
            log.warn("vector delete failed for document {}, continuing with metadata delete",
                    existing.getId(), ex);
        }
        documentRepository.deleteById(id);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CsaiErrorCodes.DOCUMENT_UNSUPPORTED, "file is empty");
        }
        if (file.getSize() > properties.getUpload().getMaxBytes()) {
            throw new BusinessException(CsaiErrorCodes.DOCUMENT_TOO_LARGE,
                    "file exceeds max size " + properties.getUpload().getMaxBytes());
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = TextExtractionService.extension(filename);
        List<String> allowed = properties.getUpload().getAllowedExtensions().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        if (!allowed.contains(ext)) {
            throw new BusinessException(CsaiErrorCodes.DOCUMENT_UNSUPPORTED,
                    "extension not allowed: " + ext);
        }
    }
}
