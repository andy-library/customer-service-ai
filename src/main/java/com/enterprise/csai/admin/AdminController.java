package com.enterprise.csai.admin;

import com.enterprise.csai.chat.ChatOrchestrator;
import com.enterprise.csai.chat.ChatRequest;
import com.enterprise.csai.chat.ChatResponse;
import com.enterprise.csai.common.config.CsaiProperties;
import com.enterprise.csai.knowledge.DocumentEntity;
import com.enterprise.csai.knowledge.DocumentIngestService;
import com.enterprise.csai.modelgateway.ModelRegistry;
import com.enterprise.csai.router.IntentType;
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

    private final DocumentIngestService documentIngestService;
    private final ChatOrchestrator chatOrchestrator;
    private final ModelRegistry modelRegistry;
    private final CsaiProperties properties;

    public AdminController(
            DocumentIngestService documentIngestService,
            ChatOrchestrator chatOrchestrator,
            ModelRegistry modelRegistry,
            CsaiProperties properties) {
        this.documentIngestService = documentIngestService;
        this.chatOrchestrator = chatOrchestrator;
        this.modelRegistry = modelRegistry;
        this.properties = properties;
    }

    @GetMapping
    public String index() {
        return "admin/index";
    }

    @GetMapping("/knowledge")
    public String knowledge(Model model) {
        model.addAttribute("documents", documentIngestService.list());
        return "admin/knowledge";
    }

    @PostMapping("/knowledge/upload")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            RedirectAttributes redirectAttributes) {
        try {
            DocumentEntity entity = documentIngestService.ingest(file, title);
            redirectAttributes.addFlashAttribute("message",
                    "上传成功: " + entity.getTitle() + " [" + entity.getStatus() + "]");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/knowledge";
    }

    @PostMapping("/knowledge/delete")
    public String delete(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
        try {
            documentIngestService.delete(id);
            redirectAttributes.addFlashAttribute("message", "已删除: " + id);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/knowledge";
    }

    @GetMapping("/chat")
    public String chatPage(Model model) {
        if (!model.containsAttribute("messageText")) {
            model.addAttribute("messageText", "");
        }
        return "admin/chat";
    }

    @PostMapping("/chat")
    public String chatSubmit(@RequestParam("message") String message, Model model) {
        model.addAttribute("messageText", message);
        try {
            ChatResponse resp = chatOrchestrator.chat(new ChatRequest(null, message, null));
            model.addAttribute("resp", resp);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "admin/chat";
    }

    @GetMapping("/models")
    public String models(Model model) {
        model.addAttribute("models", modelRegistry.listModels());
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
