package work.daqian.ai.chat.message;

import work.daqian.ai.tool.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * AI 消息
 */
public class AssistantMessage extends Message {

    private List<ToolCall> toolCalls;

    /**
     * DeepSeek API 要求工具调用消息必须携带思考内容
     */
    private String reasoningContent;

    public AssistantMessage() {}

    public AssistantMessage(String content) {
        super.role = "assistant";
        this.content = content;
    }

    public AssistantMessage(String reasoningContent, List<ToolCall> toolCalls) {
        super.role = "assistant";
        this.reasoningContent = reasoningContent;
        this.toolCalls = toolCalls;
    }

    @Override
    public String getRole() {
        return "assistant";
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssistantMessage that = (AssistantMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public String toString() {
        return "AssistantMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}
