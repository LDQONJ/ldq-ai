package work.daqian.ai.advisor;

import work.daqian.ai.chat.message.Message;

import java.util.List;
import java.util.Map;

public interface Advisor {

    /**
     * 在构建最终 Prompt 之前被调用，可以修改消息列表。
     * @param messages 当前的 Message 列表（可修改）
     * @param context  上下文参数（如 conversationId）
     * @return 修改后的 Message 列表
     */
    List<Message> before(List<Message> messages, Map<String, Object> context);

    /**
     * 在收到模型回复之后被调用，可以做一些后置处理。
     * @param response 模型的文本回复
     * @param context  上下文参数
     * @return 可返回处理后的回复，原样返回即可
     */
    default List<Message> after(List<Message> messages, Map<String, Object> context) {
        return messages;
    }
}