package work.daqian.ai.history.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import work.daqian.ai.tool.ToolCall;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 消息实体类
 */
@Document("chat_message")
public class ChatMessage {
    /**
     * 主键 id
     */
    @Id
    private String id;
    /**
     * 对话 id
     */
    private String conversationId;
    /**
     * 消息角色
     */
    private String role;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 思考内容
     */
    private String reasoningContent;
    /**
     * 工具调用请求
     */
    private List<ToolCall> toolCalls;
    /**
     * 工具调用唯一 id
     */
    private String toolCallId;
    /**
     * 创建时间
     */
    @CreatedDate
    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id) && Objects.equals(conversationId, that.conversationId) && Objects.equals(role, that.role) && Objects.equals(content, that.content) && Objects.equals(createTime, that.createTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, conversationId, role, content, createTime);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
