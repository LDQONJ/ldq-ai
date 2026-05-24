package work.daqian.ai.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import work.daqian.ai.advisor.ChatHistoryAdvisor;
import work.daqian.ai.advisor.RagAdvisor;
import work.daqian.ai.api.*;
import work.daqian.ai.chat.ChatModel;
import work.daqian.ai.chat.DefaultChatModel;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.history.InMemoryChatHistory;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.rag.*;

import java.util.List;

@Configuration
@ConditionalOnClass(ModelApi.class)
@EnableConfigurationProperties(LdqAiProperties.class)
public class LdqAiAutoConfiguration {

    public LdqAiAutoConfiguration(JacksonProperties jacksonProperties) {
        jacksonProperties.setPropertyNamingStrategy("SNAKE_CASE");
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel(List<ModelApi> modelApis, LdqAiProperties ldqAiProperties) {
        return new DefaultChatModel(modelApis, ldqAiProperties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel(List<ModelApi> modelApis, LdqAiProperties ldqAiProperties) {
        return new DefaultEmbeddingModel(modelApis, ldqAiProperties);
    }

    @Bean
    public TextSplitter textSplitter() {
        return new TextSplitter();
    }

    @Bean
    public ModelApi ollamaApiAdapter(WebClient.Builder builder) {
        return new OllamaApiAdapter(builder);
    }

    @Bean
    @ConditionalOnProperty("ldq-ai.api-key.alibaba")
    public ModelApi alibabaApiAdapter(WebClient.Builder builder, LdqAiProperties ldqAiProperties) {
        return new AlibabaApiAdapter(builder, ldqAiProperties.getApiKey().getAlibaba());
    }

    @Bean
    @ConditionalOnProperty("ldq-ai.api-key.google")
    public ModelApi googleApiAdapter(WebClient.Builder builder, LdqAiProperties ldqAiProperties) {
        return new GoogleApiAdapter(builder, ldqAiProperties.getApiKey().getGoogle());
    }

    @Bean
    public ModelApi openAIApiAdapter(WebClient.Builder builder, LdqAiProperties ldqAiProperties) {
        return new OpenAIApiAdapter(builder, ldqAiProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ChatHistory.class)
    public ChatHistory chatHistory() {
        return new InMemoryChatHistory();
    }

    @Bean
    public ChatHistoryAdvisor chatHistoryAdvisor(ChatHistory chatHistory, LdqAiProperties ldqAiProperties) {
        return new ChatHistoryAdvisor(chatHistory, ldqAiProperties);
    }

    @Bean
    @ConditionalOnBean(VectorStore.class)
    public RagAdvisor ragAdvisor(VectorStore vectorStore, LdqAiProperties ldqAiProperties) {
        return new RagAdvisor(vectorStore, ldqAiProperties.getRag().getTopK());
    }
}
