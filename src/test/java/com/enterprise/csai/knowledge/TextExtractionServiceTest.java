package com.enterprise.csai.knowledge;

import com.microservice.framework.web.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    private final TextExtractionService extraction = new TextExtractionService();

    @Test
    void extractsMarkdownBytes() {
        byte[] bytes = "# Hello\nWorld".getBytes(StandardCharsets.UTF_8);
        String text = extraction.extract("x.md", "text/markdown", bytes);
        assertThat(text).contains("Hello");
    }

    @Test
    void extractsTxtBytes() {
        byte[] bytes = "plain text".getBytes(StandardCharsets.UTF_8);
        String text = extraction.extract("note.txt", "text/plain", bytes);
        assertThat(text).isEqualTo("plain text");
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThatThrownBy(() -> extraction.extract("x.docx", "application/msword", new byte[]{1}))
                .isInstanceOf(BusinessException.class);
    }
}
