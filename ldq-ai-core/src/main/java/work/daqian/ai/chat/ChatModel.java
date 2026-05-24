package work.daqian.ai.chat;

import work.daqian.ai.api.dto.StreamResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.tool.FunctionCallback;

import java.util.List;

public interface ChatModel {

    StreamResponse stream(List<Message> messages, List<FunctionCallback> tools, boolean enableThinking, boolean withThinkingContent);

    TextResponse call(List<Message> messages, List<FunctionCallback> tools, boolean think);
}
