package work.daqian.ai.history.mongo;

import org.springframework.transaction.annotation.Transactional;
import work.daqian.ai.chat.message.AssistantMessage;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.chat.message.ToolMessage;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.tool.ToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoChatHistory implements ChatHistory  {

    private final HistoryRepository historyRepository;

    public MongoChatHistory(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMessage> chatMessages = historyRepository.findTopByConversationIdOrderByCreateTimeDesc(conversationId, lastN);
        if (chatMessages == null || chatMessages.isEmpty()) return Collections.emptyList();
        Collections.reverse(chatMessages);
        return chatMessages.stream()
                .map(chatMessage -> {
                    if (chatMessage.getRole().equals("assistant")) {
                        List<ToolCall> toolCalls = chatMessage.getToolCalls();
                        if (toolCalls != null && !toolCalls.isEmpty())
                            return new AssistantMessage(chatMessage.getReasoningContent(), toolCalls);
                        else
                            return new AssistantMessage(chatMessage.getContent());
                    } else if (chatMessage.getRole().equals("tool")) {
                        return new ToolMessage(chatMessage.getContent(), chatMessage.getToolCallId());
                    }
                    return new Message(chatMessage.getRole(), chatMessage.getContent());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        List<ChatMessage> chatMessages = new ArrayList<>(messages.size());
        for (Message message : messages) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(conversationId);
            chatMessage.setRole(message.getRole());
            chatMessage.setContent(message.getContent());
            if (message.getRole().equals("assistant")) {
                chatMessage.setToolCalls(((AssistantMessage) message).getToolCalls());
                chatMessage.setReasoningContent(((AssistantMessage) message).getReasoningContent());
            }
            else if (message.getRole().equals("tool"))
                chatMessage.setToolCallId(((ToolMessage) message).getToolCallId());
            chatMessages.add(chatMessage);
        }
        historyRepository.saveAll(chatMessages);
    }

    @Override
    public void clear(String conversationId) {
        historyRepository.removeAllByConversationId(conversationId);
    }
}
