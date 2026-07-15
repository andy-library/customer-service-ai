package com.enterprise.csai.knowledge;

import com.enterprise.csai.common.config.CsaiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DifyKnowledgeRetrieverParseTest {

    @Test
    void parsesDifyRetrievePayload() throws Exception {
        CsaiProperties props = new CsaiProperties();
        props.getKnowledge().setProvider("dify");
        DifyKnowledgeRetriever retriever = new DifyKnowledgeRetriever(props, new ObjectMapper());
        String raw = """
                {
                  "query": "退款",
                  "records": [
                    {
                      "score": 0.91,
                      "segment": {
                        "content": "7日内未激活可全额退款",
                        "document": { "id": "d1", "name": "退款政策" }
                      }
                    }
                  ]
                }
                """;
        @SuppressWarnings("unchecked")
        List<KnowledgeChunk> chunks = (List<KnowledgeChunk>) ReflectionTestUtils.invokeMethod(
                retriever, "parseRecords", raw);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().title()).isEqualTo("退款政策");
        assertThat(chunks.getFirst().content()).contains("全额退款");
        assertThat(chunks.getFirst().score()).isEqualTo(0.91);
    }
}
