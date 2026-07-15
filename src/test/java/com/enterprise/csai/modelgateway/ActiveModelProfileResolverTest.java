package com.enterprise.csai.modelgateway;

import com.enterprise.csai.common.config.CsaiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveModelProfileResolverTest {

    @Test
    void localSourceUsesConfiguredModelsAndEmbedding() {
        CsaiProperties props = new CsaiProperties();
        props.setModelSource("local");
        CsaiProperties.ModelConfig m = new CsaiProperties.ModelConfig();
        m.setId("classifier-default");
        m.setBaseUrl("http://127.0.0.1:18080/v1");
        m.setApiKey("sk-local");
        m.setModelName("local-qwen");
        m.setRole(ModelRole.CLASSIFIER);
        props.setModels(List.of(m));
        props.getEmbedding().setBaseUrl("http://127.0.0.1:18081/v1");
        props.getEmbedding().setModelName("local-bge-m3");
        props.getEmbedding().setDimensions(1024);

        ActiveModelProfileResolver resolver = new ActiveModelProfileResolver(props);

        assertThat(resolver.modelSource()).isEqualTo("local");
        assertThat(resolver.resolveChatModels()).hasSize(1);
        assertThat(resolver.resolveChatModels().getFirst().getModelName()).isEqualTo("local-qwen");
        assertThat(resolver.resolveEmbedding().getBaseUrl()).contains("18081");
        assertThat(resolver.resolveEmbedding().getModelName()).isEqualTo("local-bge-m3");
    }

    @Test
    void cloudSourceBuildsPluggableChatModelsFromCloudBundle() {
        CsaiProperties props = new CsaiProperties();
        props.setModelSource("cloud");
        props.getCloud().setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        props.getCloud().setApiKey("sk-cloud");
        props.getCloud().setClassifierModel("glm-5.1");
        props.getCloud().setAnswerStrongModel("glm-5.1");
        props.getCloud().setAnswerFastModel("qwen-plus");
        props.getCloud().setEmbeddingBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        props.getCloud().setEmbeddingModel("text-embedding-v3");
        props.getCloud().setEmbeddingDimensions(1024);

        ActiveModelProfileResolver resolver = new ActiveModelProfileResolver(props);

        assertThat(resolver.modelSource()).isEqualTo("cloud");
        List<CsaiProperties.ModelConfig> models = resolver.resolveChatModels();
        assertThat(models).hasSize(3);
        assertThat(models).extracting(CsaiProperties.ModelConfig::getId)
                .containsExactly("classifier-default", "answer-strong", "answer-fast");
        assertThat(models).allMatch(c -> c.getBaseUrl().contains("dashscope"));
        assertThat(models).allMatch(c -> "sk-cloud".equals(c.getApiKey()));
        assertThat(models.get(2).getModelName()).isEqualTo("qwen-plus");

        CsaiProperties.EmbeddingConfig emb = resolver.resolveEmbedding();
        assertThat(emb.getModelName()).isEqualTo("text-embedding-v3");
        assertThat(emb.getDimensions()).isEqualTo(1024);
    }
}
