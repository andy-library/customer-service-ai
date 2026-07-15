package com.enterprise.csai.domain.policy;

import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.KnowledgeChunk;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Component
public class GuardrailPolicy {

    private static final String DEFAULT_SYSTEM = """
            You are an enterprise customer-service assistant.
            Prefer answers grounded in the provided knowledge snippets.
            If knowledge is required but snippets are empty or insufficient, clearly say
            that the knowledge base does not contain enough evidence. Do not invent
            internal policy numbers, prices, or ticket IDs that are not present in the snippets.
            Answer in the same language as the user question when possible.
            """;

    private final CsaiProperties properties;
    private final String systemPrompt;

    public GuardrailPolicy(CsaiProperties properties) {
        this.properties = properties;
        this.systemPrompt = loadSystemPrompt(properties);
    }

    public String systemPrompt() {
        String override = properties.getGuardrail().getSystemPrompt();
        return override != null && !override.isBlank() ? override : systemPrompt;
    }

    public boolean looksLikeInjection(String userMessage) {
        if (!properties.getGuardrail().isBlockInjectionPatterns()) {
            return false;
        }
        if (userMessage == null) {
            return false;
        }
        String m = userMessage.toLowerCase(Locale.ROOT);
        return m.contains("ignore previous instructions")
                || m.contains("ignore all instructions")
                || m.contains("系统提示")
                || m.contains("忽略以上规则")
                || m.contains("jailbreak");
    }

    public String evidenceInsufficientAnswer() {
        return "知识库中暂无足够依据回答该问题。您可以补充更多细节，或输入「转人工」联系人工客服。";
    }

    public String injectionBlockedAnswer() {
        return "请求无法处理：检测到可能的提示注入内容。请重新描述您的业务问题。";
    }

    public String handoffAnswer(String reason) {
        return "已为您标记转人工处理（原因：" + reason + "）。请稍候，或留下您的联系方式以便回访。";
    }

    public boolean shouldForceEvidenceDisclaimer(boolean ragEnabled, List<KnowledgeChunk> chunks) {
        return properties.getGuardrail().isRequireEvidence()
                && ragEnabled
                && (chunks == null || chunks.isEmpty());
    }

    private static String loadSystemPrompt(CsaiProperties properties) {
        String path = properties.getGuardrail().getSystemPromptClasspath();
        if (path == null || path.isBlank()) {
            path = "prompts/system-cs.md";
        }
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).strip();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return DEFAULT_SYSTEM;
    }
}
