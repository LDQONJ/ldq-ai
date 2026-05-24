package work.daqian.ai.autoconfigure.history;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import work.daqian.ai.autoconfigure.LdqAiAutoConfiguration;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.history.mongo.HistoryRepository;
import work.daqian.ai.history.mongo.MongoChatHistory;

@Configuration
@EnableMongoAuditing
@ConditionalOnClass({MongoChatHistory.class})
@ConditionalOnMissingBean({JdbcHistoryAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "work.daqian.ai.history.mongo")
@AutoConfigureBefore({LdqAiAutoConfiguration.class, RedisHistoryAutoConfiguration.class})
public class MongoHistoryAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ChatHistory.class)
    public ChatHistory chatHistory(HistoryRepository historyRepository) {
        return new MongoChatHistory(historyRepository);
    }
}
