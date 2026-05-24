package work.daqian.ai.history.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import work.daqian.ai.chat.message.AssistantMessage;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.chat.message.ToolMessage;
import work.daqian.ai.history.ChatHistory;
import work.daqian.ai.tool.ToolCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 聊天历史 JDBC 实现
 * @author LDQ
 */
public class JdbcChatHistory implements ChatHistory {

    private static final Logger log = LoggerFactory.getLogger(JdbcChatHistory.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public JdbcChatHistory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String sql = "SELECT role, content FROM chat_message " +
                "WHERE conversation_id = ? ORDER BY id DESC LIMIT ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, conversationId, lastN);
        Collections.reverse(rows);
        List<Message> messages = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String role = ((String) row.get("role")).toLowerCase();
            String content = (String) row.get("content");
            if (role.equals("assistant")) {
                String toolCallsJson = (String) row.get("tool_calls");
                List<ToolCall> toolCalls = Collections.emptyList();
                try {
                    if (toolCallsJson != null && !toolCallsJson.isEmpty()) {
                        toolCalls = mapper.readValue(toolCallsJson, new TypeReference<List<ToolCall>>() {});
                        messages.add(new AssistantMessage(content, toolCalls));
                    } else {
                        messages.add(new AssistantMessage(content));
                    }
                } catch (JsonProcessingException e) {
                    log.error("反序列化 toolCallsJson 失败");
                    messages.add(new AssistantMessage(content, toolCalls));
                }
            } else if (role.equals("tool")) {
                String toolCallId = (String) row.get("tool_call_id");
                messages.add(new ToolMessage(content, toolCallId));
            } else {
                messages.add(new Message(role, content));
            }
        }
        return messages;
    }

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        // 获取当前对话最大消息 id
        String maxSql = "SELECT COALESCE(MAX(id), -1) FROM chat_message WHERE conversation_id = ?";
        Integer currentMax = jdbcTemplate.queryForObject(maxSql, Integer.class, conversationId);
        int nextIndex = currentMax + 1;
        String insertSql = "INSERT INTO chat_message (conversation_id, id, role, content, reasoning_content, tool_calls, tool_call_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        for (Message message : messages) {
            String role = message.getRole();
            if (role.equals("assistant")) {
                List<ToolCall> toolCalls = ((AssistantMessage) message).getToolCalls();
                String toolCallsJson = null;
                try {
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        toolCallsJson = mapper.writeValueAsString(toolCalls);
                    }
                } catch (JsonProcessingException e) {
                    log.error("序列化 toolCalls 失败");
                    toolCallsJson = "序列化 toolCalls 失败";
                } finally {
                    batchArgs.add(new Object[]{conversationId, nextIndex, role, message.getContent(), ((AssistantMessage) message).getReasoningContent(), toolCallsJson, null});
                }
            } else if (role.equals("tool")) {
                String toolCallId = ((ToolMessage) message).getToolCallId();
                batchArgs.add(new Object[]{conversationId, nextIndex, role, message.getContent(), null, null, toolCallId});
            } else {
                batchArgs.add(new Object[]{conversationId, nextIndex, role, message.getContent(), null, null, null});
            }
            nextIndex++;
        }
        jdbcTemplate.batchUpdate(insertSql, batchArgs);

    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        String sql = "DELETE FROM chat_message WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }
}
