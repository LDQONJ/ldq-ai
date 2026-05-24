package work.daqian.ai.rag.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.InitializingBean;
import work.daqian.ai.properties.LdqAiProperties;

/**
 * 初始化 ES 索引库，不存在时创建
 * @author LDQ
 */
public class EsIndexInitializer implements InitializingBean {

    private final ElasticsearchClient esClient;

    private final LdqAiProperties ldqAiProperties;

    public EsIndexInitializer(ElasticsearchClient esClient, LdqAiProperties ldqAiProperties) {
        this.esClient = esClient;
        this.ldqAiProperties = ldqAiProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String index = ldqAiProperties.getRag().getIndex();
        boolean exists = esClient.indices()
                .exists(e -> e.index(index))
                .value();
        if (!exists) {
            esClient.indices().create(c -> c
                    .index(index)
                    .mappings(m -> m
                            .properties("content", p -> p.text(t -> t))
                            .properties("embedding",
                                    p -> p.denseVector(
                                            d -> d.dims(1024).index(true).similarity("cosine")))
                            .properties("metadata", p -> p.object(o -> o))
                    )
            );
        }
    }
}