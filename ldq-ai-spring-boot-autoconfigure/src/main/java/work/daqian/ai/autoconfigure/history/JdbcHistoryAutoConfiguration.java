package work.daqian.ai.autoconfigure.history;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.history.jdbc.JdbcChatHistory;

@Configuration
@AutoConfigureBefore(MongoHistoryAutoConfiguration.class)
@ConditionalOnClass({JdbcTemplate.class, JdbcChatHistory.class})
@ConditionalOnProperty(name = "spring.datasource.url")
public class JdbcHistoryAutoConfiguration {

    public JdbcHistoryAutoConfiguration(SqlInitializationProperties sqlInitProperties) {
        sqlInitProperties.setMode(DatabaseInitializationMode.ALWAYS);
        sqlInitProperties.setContinueOnError(true);
    }

    @Bean
    @ConditionalOnMissingBean(ChatHistory.class)
    public ChatHistory chatHistory(JdbcTemplate jdbcTemplate) {
        return new JdbcChatHistory(jdbcTemplate);
    }
}
