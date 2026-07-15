package com.enterprise.csai.modelgateway.api;

import com.enterprise.csai.modelgateway.ModelRegistry;
import com.enterprise.csai.modelgateway.ModelView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final ModelRegistry modelRegistry;

    public ModelController(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @GetMapping
    public List<ModelView> list() {
        return modelRegistry.listModels();
    }
}
