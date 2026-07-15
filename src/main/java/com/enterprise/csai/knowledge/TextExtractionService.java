package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.error.CsaiErrorCodes;
import com.microservice.framework.web.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class TextExtractionService {

    public String extract(String filename, String contentType, byte[] bytes) {
        String ext = extension(filename);
        return switch (ext) {
            case "txt", "md" -> new String(bytes, StandardCharsets.UTF_8);
            case "pdf" -> extractPdf(bytes);
            default -> throw new BusinessException(CsaiErrorCodes.DOCUMENT_UNSUPPORTED,
                    "unsupported file extension: " + ext);
        };
    }

    private String extractPdf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BusinessException(CsaiErrorCodes.DOCUMENT_UNSUPPORTED,
                        "PDF has no extractable text layer");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(CsaiErrorCodes.DOCUMENT_INGEST_FAILED,
                    "failed to parse PDF: " + ex.getMessage(), ex);
        }
    }

    static String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
