package work.daqian.ai.advisor;

import work.daqian.ai.chat.message.Message;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.properties.LdqAiProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatHistoryAdvisor implements Advisor {

    private final ChatHistory chatHistory;
    private final LdqAiProperties ldqAiProperties;

    // 默认的会话 ID 在上下文中使用的 key
    public static final String CONVERSATION_ID_KEY = "conversationId";

    public ChatHistoryAdvisor(ChatHistory chatHistory, LdqAiProperties ldqAiProperties) {
        this.chatHistory = chatHistory;
        this.ldqAiProperties = ldqAiProperties;
    }

    @Override
    public List<Message> before(List<Message> messages, Map<String, Object> context) {
        String conversationId = (String) context.get(CONVERSATION_ID_KEY);
        if (conversationId != null) {
            // 加载历史消息并放在最前面
            List<Message> history = chatHistory.get(conversationId, ldqAiProperties.getChat().getLastN());
            List<Message> merged = new ArrayList<>(history);
            merged.addAll(messages);
            return merged;
        }
        return messages;
    }

    @Override
    public List<Message> after(List<Message> messages, Map<String, Object> context) {
        String conversationId = (String) context.get(CONVERSATION_ID_KEY);
        if (conversationId == null) return messages;
        int i;
        int size = messages.size();
        for (i = size - 1; i > 0; i--) {
            Message message = messages.get(i);
            if (message.getRole().equals("user")) break;
        }
        chatHistory.add(conversationId, messages.subList(i, size));
        return messages;
    }
}
