package work.daqian.ai.chat;

import work.daqian.ai.api.dto.StreamResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.tool.FunctionCallback;

import java.util.Collections;
import java.util.List;

/**
 * 聊天模型接口
 * @author LDQ
 */
public interface ChatModel {

    /**
     * 流式调用模型
     * @param messages 消息列表
     * @param tools 工具列表
     * @param enableThinking 是否开启思考模式
     * @param withThinkingContent 流式响应是否包含思考内容
     * @return 流式响应包装
     */
    StreamResponse stream(List<Message> messages, List<FunctionCallback> tools, boolean enableThinking, boolean withThinkingContent);

    /**
     * 流式调用模型
     * @param messages 消息列表
     * @param enableThinking 是否开启思考模式
     * @return 流式响应包装
     */
    default StreamResponse stream(List<Message> messages, boolean enableThinking) {
        return stream(messages, Collections.emptyList(), enableThinking, true);
    }

    /**
     * 非流式调用模型
     * @param messages 消息列表
     * @param tools 工具列表
     * @param enableThinking 是否开启思考模式
     * @return 文本响应
     */
    TextResponse call(List<Message> messages, List<FunctionCallback> tools, boolean enableThinking);

    /**
     * 非流式调用模型
     * @param messages 消息列表
     * @param enableThinking 是否开启思考模式
     * @return 文本响应
     */
    default TextResponse call(List<Message> messages, boolean enableThinking) {
        return call(messages, Collections.emptyList(), enableThinking);
    }
}
