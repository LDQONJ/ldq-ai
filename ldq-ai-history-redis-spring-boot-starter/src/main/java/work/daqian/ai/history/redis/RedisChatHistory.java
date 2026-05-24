package work.daqian.ai.history.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import work.daqian.ai.chat.message.AssistantMessage;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.chat.message.ToolMessage;
import work.daqian.ai.history.ChatHistory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RedisChatHistory implements ChatHistory {

    public static final Logger log = LoggerFactory.getLogger(RedisChatHistory.class);

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    public static final String HISTORY_KEY_PREFIX = "chat:history:";

    public RedisChatHistory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<String> jsonMessages = redisTemplate.opsForList().range(HISTORY_KEY_PREFIX + conversationId, -lastN, -1);
        if (jsonMessages == null || jsonMessages.isEmpty()) return Collections.emptyList();
        return jsonMessages.stream().map(json -> {
            try {
                if (json.startsWith("{\"role\":\"assistant\",\"content\":\""))
                    return mapper.readValue(json, AssistantMessage.class);
                else if (json.startsWith("{\"role\":\"tool\",\"content\":\"")) {
                    return mapper.readValue(json, ToolMessage.class);
                }
                return mapper.readValue(json, Message.class);
            } catch (JsonProcessingException e) {
                log.error("反序列化 Redis 聊天历史消息时出现异常");
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String key = HISTORY_KEY_PREFIX + conversationId;
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Nullable
            @Override
            public Object execute(@NonNull RedisOperations operations) throws DataAccessException {
                ListOperations<String, String> ops = operations.opsForList();
                for (Message message : messages) {
                    String json;
                    try {
                        json = mapper.writeValueAsString(message);
                        ops.rightPush(key, json);
                    } catch (JsonProcessingException e) {
                        log.error("序列化 Redis 聊天历史消息时出现异常");
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        });
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(HISTORY_KEY_PREFIX + conversationId);
    }
}
