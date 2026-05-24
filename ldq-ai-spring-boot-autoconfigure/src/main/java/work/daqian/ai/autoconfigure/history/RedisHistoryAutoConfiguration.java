package work.daqian.ai.autoconfigure.history;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import work.daqian.ai.autoconfigure.LdqAiAutoConfiguration;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.history.redis.RedisChatHistory;

@Configuration
@AutoConfigureBefore(LdqAiAutoConfiguration.class)
@ConditionalOnClass({StringRedisTemplate.class, RedisChatHistory.class})
@ConditionalOnMissingBean({MongoHistoryAutoConfiguration.class, JdbcHistoryAutoConfiguration.class})
public class RedisHistoryAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ChatHistory.class)
    public ChatHistory chatHistory(StringRedisTemplate redisTemplate) {
        return new RedisChatHistory(redisTemplate);
    }
}
