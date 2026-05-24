package work.daqian.ai.autoconfigure.rag;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponseInterceptor;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import work.daqian.ai.autoconfigure.LdqAiAutoConfiguration;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.rag.EmbeddingModel;
import work.daqian.ai.rag.VectorStore;
import work.daqian.ai.rag.es.ElasticsearchVectorStore;
import work.daqian.ai.rag.es.EsIndexInitializer;

@Configuration
@AutoConfigureBefore(LdqAiAutoConfiguration.class)
@ConditionalOnClass(ElasticsearchVectorStore.class)
@ConditionalOnMissingBean(RagRedisAutoConfiguration.class)
public class RagEsAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ElasticsearchClient.class)
    public ElasticsearchClient elasticsearchClient(ElasticsearchProperties properties) {
        RestClient restClient = RestClient
                .builder(HttpHost.create(properties.getUris().get(0)))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                            response.addHeader("X-Elastic-Product", "Elasticsearch");
                        }))
                .build();
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore VectorStore(ElasticsearchClient esClient, EmbeddingModel embeddingModel, LdqAiProperties ldqAiProperties) {
        return new ElasticsearchVectorStore(esClient, embeddingModel, ldqAiProperties);
    }

    @Bean
    public EsIndexInitializer esIndexInitializer(ElasticsearchClient esClient, LdqAiProperties ldqAiProperties) {
        return new EsIndexInitializer(esClient, ldqAiProperties);
    }
}
