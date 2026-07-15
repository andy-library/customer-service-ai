package com.enterprise.csai.admin;

import com.enterprise.csai.chat.ChatOrchestrator;
import com.enterprise.csai.chat.ChatRequest;
import com.enterprise.csai.chat.ChatResponse;
import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.DocumentEntity;
import com.enterprise.csai.knowledge.DocumentIngestService;
import com.enterprise.csai.knowledge.KnowledgeSearchService;
import com.enterprise.csai.modelgateway.ActiveModelProfileResolver;
import com.enterprise.csai.modelgateway.ModelRegistry;
import com.enterprise.csai.router.IntentType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ObjectProvider<DocumentIngestService> documentIngestService;
    private final ChatOrchestrator chatOrchestrator;
    private final ModelRegistry modelRegistry;
    private final CsaiProperties properties;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ActiveModelProfileResolver profileResolver;

    public AdminController(
            ObjectProvider<DocumentIngestService> documentIngestService,
            ChatOrchestrator chatOrchestrator,
            ModelRegistry modelRegistry,
            CsaiProperties properties,
            KnowledgeSearchService knowledgeSearchService,
            ActiveModelProfileResolver profileResolver) {
        this.documentIngestService = documentIngestService;
        this.chatOrchestrator = chatOrchestrator;
        this.modelRegistry = modelRegistry;
        this.properties = properties;
        this.knowledgeSearchService = knowledgeSearchService;
        this.profileResolver = profileResolver;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("modelSource", profileResolver.modelSource());
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        return "admin/index";
    }

    @GetMapping("/knowledge")
    public String knowledge(Model model) {
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        model.addAttribute("modelSource", profileResolver.modelSource());
        DocumentIngestService ingest = documentIngestService.getIfAvailable();
        model.addAttribute("localIngest", ingest != null);
        if (ingest != null) {
            model.addAttribute("documents", ingest.list());
        }
        if (properties.getKnowledge().isDify()) {
            model.addAttribute("difyBaseUrl", properties.getKnowledge().getDify().getBaseUrl());
            model.addAttribute("difyDatasetConfigured",
                    properties.getKnowledge().getDify().getDatasetId() != null
                            && !properties.getKnowledge().getDify().getDatasetId().isBlank());
        }
        return "admin/knowledge";
    }

    @PostMapping("/knowledge/upload")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            RedirectAttributes redirectAttributes) {
        DocumentIngestService ingest = documentIngestService.getIfAvailable();
        if (ingest == null) {
            redirectAttributes.addFlashAttribute("error",
                    "本地入库已关闭。知识库请在 Dify 管理（csai.knowledge.provider=dify）。");
            return "redirect:/admin/knowledge";
        }
        try {
            DocumentEntity entity = ingest.ingest(file, title);
            redirectAttributes.addFlashAttribute("message",
                    "上传成功: " + entity.getTitle() + " [" + entity.getStatus() + "]");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/knowledge";
    }

    @PostMapping("/knowledge/delete")
    public String delete(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
        DocumentIngestService ingest = documentIngestService.getIfAvailable();
        if (ingest == null) {
            redirectAttributes.addFlashAttribute("error", "本地入库已关闭。");
            return "redirect:/admin/knowledge";
        }
        try {
            ingest.delete(id);
            redirectAttributes.addFlashAttribute("message", "已删除: " + id);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/knowledge";
    }

    @PostMapping("/knowledge/search")
    public String search(@RequestParam("query") String query, Model model) {
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        model.addAttribute("searchQuery", query);
        try {
            model.addAttribute("searchHits", knowledgeSearchService.search(query, 5));
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return knowledge(model);
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("modelSource", profileResolver.modelSource());
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        return "admin/chat";
    }

    /**
     * Legacy form POST kept for compatibility; prefer browser streaming UI → {@code /api/v1/chat/stream}.
     */
    @PostMapping("/chat")
    public String chatSubmit(@RequestParam("message") String message, Model model) {
        model.addAttribute("modelSource", profileResolver.modelSource());
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        try {
            ChatResponse resp = chatOrchestrator.chat(new ChatRequest(null, message, null));
            model.addAttribute("resp", resp);
            model.addAttribute("messageText", message);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("messageText", message);
        }
        return "admin/chat";
    }

    @GetMapping("/models")
    public String models(Model model) {
        model.addAttribute("models", modelRegistry.listModels());
        model.addAttribute("modelSource", profileResolver.modelSource());
        model.addAttribute("knowledgeProvider", knowledgeSearchService.activeProvider());
        List<Map<String, Object>> intents = Arrays.stream(IntentType.values())
                .map(intent -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("intent", intent.name());
                    item.put("requiresKnowledge", intent.requiresKnowledge());
                    item.put("answerModelId", properties.getRouter().getIntentModelMapping()
                            .getOrDefault(intent.name(), properties.getRouter().getDefaultAnswerModelId()));
                    return item;
                })
                .toList();
        model.addAttribute("intents", intents);
        model.addAttribute("classifierModelId", properties.getRouter().getClassifierModelId());
        model.addAttribute("defaultAnswerModelId", properties.getRouter().getDefaultAnswerModelId());
        return "admin/models";
    }
}
