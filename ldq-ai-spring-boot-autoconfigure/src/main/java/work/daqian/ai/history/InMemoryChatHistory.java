package work.daqian.ai.history;

import work.daqian.ai.chat.message.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天历史 JVM 内存实现
 * @author LDQ
 */
public class InMemoryChatHistory implements ChatHistory {

    private final Map<String, List<Message>> chatMemories = new ConcurrentHashMap<>();

    @Override
    public List<Message> get(String conversationId, int lastN) {
        return chatMemories.getOrDefault(conversationId, Collections.emptyList());
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        chatMemories.computeIfAbsent(conversationId, key -> Collections.synchronizedList(new ArrayList<>()))
             .addAll(messages);
    }

    @Override
    public void clear(String conversationId) {
        chatMemories.remove(conversationId);
    }
}