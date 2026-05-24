package work.daqian.ai.chat.message;

import java.util.Objects;

/**
 * 工具调用消息（结果）
 */
public class ToolMessage extends Message {

    private String toolCallId;

    public ToolMessage() {}

    public ToolMessage(String content, String toolCallId) {
        super.role = "tool";
        super.content = content;
        this.toolCallId = toolCallId;
    }

    @Override
    public String getRole() {
        return "tool";
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ToolMessage that = (ToolMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public String toString() {
        return "ToolMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}
