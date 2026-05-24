package work.daqian.ai.autoconfigure.rag;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import work.daqian.ai.autoconfigure.LdqAiAutoConfiguration;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.rag.EmbeddingModel;
import work.daqian.ai.rag.VectorStore;
import work.daqian.ai.rag.redis.RedisIndexInitializer;
import work.daqian.ai.rag.redis.RedisVectorStore;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnClass({JedisPool.class, RedisVectorStore.class})
@AutoConfigureBefore({LdqAiAutoConfiguration.class, RagEsAutoConfiguration.class})
public class RagRedisAutoConfiguration {

    private final RedisProperties redisProperties;

    public RagRedisAutoConfiguration(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    @PostConstruct
    public void init() {
        redisProperties.setClientType(RedisProperties.ClientType.JEDIS);
    }

    @Bean
    public JedisPool jedisPool(RedisProperties redisProperties) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        return new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort(), redisProperties.getUsername(), redisProperties.getPassword());
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(JedisPool jedisPool, EmbeddingModel embeddingModel, LdqAiProperties ldqAiProperties) {
        return new RedisVectorStore(jedisPool::getResource, embeddingModel, ldqAiProperties);
    }

    @Bean
    public RedisIndexInitializer redisIndexInitializer(JedisPool jedisPool, LdqAiProperties ldqAiProperties) {
        return new RedisIndexInitializer(jedisPool.getResource(), ldqAiProperties);
    }
}
