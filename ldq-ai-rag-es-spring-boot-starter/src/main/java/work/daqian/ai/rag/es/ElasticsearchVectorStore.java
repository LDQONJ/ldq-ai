package work.daqian.ai.rag.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.rag.Document;
import work.daqian.ai.rag.EmbeddingModel;
import work.daqian.ai.rag.VectorStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量存储 Elasticsearch 实现
 * @author LDQ
 */
public class ElasticsearchVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchVectorStore.class);

    private final ElasticsearchClient esClient;
    private final LdqAiProperties ldqAiProperties;
    private final EmbeddingModel embeddingModel;

    public ElasticsearchVectorStore(ElasticsearchClient esClient, EmbeddingModel embeddingModel, LdqAiProperties ldqAiProperties) {
        this.esClient = esClient;
        this.embeddingModel = embeddingModel;
        this.ldqAiProperties = ldqAiProperties;
    }

    @Override
    public void add(List<Document> documents) {
        try {
            List<List<Float>> embeddings = embeddingModel.embed(documents.stream().map(Document::getContent).collect(Collectors.toList()));
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                List<Float> embedding = embeddings.get(i);
                Map<String, Object> doc = new HashMap<>();
                doc.put("content", document.getContent());
                doc.put("embedding", embedding);
                doc.put("metadata", document.getMetadata());
                esClient.index(idx -> idx
                        .index(ldqAiProperties.getRag().getIndex())
                        //.id(document.getId())
                        .document(doc)
                );
            }
        } catch (IOException e) {
            log.error("保存文档到向量库时出现异常");
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Document> similaritySearch(Object query, long topK) {
        try {
            List<Float> vector = embeddingModel.embed(query);
            SearchResponse<Map> response = esClient.search(s -> s
                            .index(ldqAiProperties.getRag().getIndex())
                            .knn(k -> k
                                    .field("embedding")
                                    .queryVector(vector)
                                    .k(topK)
                                    .numCandidates(topK * 10L))
                            .source(sc -> sc.filter(b -> b.includes("content", "metadata")))
                            .size((int) topK),
                    Map.class);
            return response.hits().hits().stream()
                    .map(hit -> {
                        Map source = hit.source();
                        return new Document(hit.id(), (String) source.get("content"), (Map<String, Object>) source.get("metadata"));
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("RAG 搜索出现异常");
            throw new RuntimeException(e);
        }
    }
}
