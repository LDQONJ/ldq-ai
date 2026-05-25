package work.daqian.ai.api.dto;

import work.daqian.ai.tool.ToolCall;

import java.util.Collections;
import java.util.List;

/**
 * 普通文本响应
 * @author LDQ
 */
public interface TextResponse {
    String getContent();
    default String getThinking() {
        return "";
    }
    default List<ToolCall> getToolCalls() {
        return Collections.emptyList();
    }
}
