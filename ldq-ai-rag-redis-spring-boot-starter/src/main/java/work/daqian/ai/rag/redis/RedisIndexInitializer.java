package work.daqian.ai.rag.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;
import work.daqian.ai.properties.LdqAiProperties;

import static work.daqian.ai.rag.redis.RedisVectorStore.DOCUMENT_KEY_PREFIX;

/**
 * 启动应用时自动创建索引库，不会覆盖原有索引库
 * @author LDQ
 */
public class RedisIndexInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RedisIndexInitializer.class);
    private final Jedis jedis;
    private final LdqAiProperties ldqAiProperties;

    public RedisIndexInitializer(Jedis jedis, LdqAiProperties ldqAiProperties) {
        this.jedis = jedis;
        this.ldqAiProperties = ldqAiProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String indexName = ldqAiProperties.getRag().getIndex();
        try {
            jedis.sendCommand(FT_CREATE_COMMAND,
                    SafeEncoder.encode(indexName),
                    SafeEncoder.encode("ON"), SafeEncoder.encode("HASH"),
                    SafeEncoder.encode("PREFIX"), SafeEncoder.encode("1"), SafeEncoder.encode(DOCUMENT_KEY_PREFIX),
                    SafeEncoder.encode("SCHEMA"),
                    SafeEncoder.encode("content"), SafeEncoder.encode("TEXT"),
                    SafeEncoder.encode("metadata"), SafeEncoder.encode("TEXT"),
                    SafeEncoder.encode("embedding"), SafeEncoder.encode("VECTOR"), SafeEncoder.encode("FLAT"),
                    SafeEncoder.encode("6"),
                    SafeEncoder.encode("TYPE"), SafeEncoder.encode("FLOAT32"),
                    SafeEncoder.encode("DIM"), SafeEncoder.encode(String.valueOf(ldqAiProperties.getRag().getDimensions())),
                    SafeEncoder.encode("DISTANCE_METRIC"), SafeEncoder.encode("COSINE")
            );
        } catch (Exception e) {
            log.info("Redis 索引库 {} 已存在，无需创建", ldqAiProperties.getRag().getIndex());
        }
    }

    // 自定义协议命令（避免硬编码）
    private static final ProtocolCommand FT_CREATE_COMMAND = () -> SafeEncoder.encode("FT.CREATE");
}
