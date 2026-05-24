package work.daqian.ai.api.dto;

import work.daqian.ai.tool.ToolCall;

import java.util.Collections;
import java.util.List;

public interface TextResponse {
    String getContent();
    default String getThinking() {
        return "";
    }
    default List<ToolCall> getToolCalls() {
        return Collections.emptyList();
    }
}
