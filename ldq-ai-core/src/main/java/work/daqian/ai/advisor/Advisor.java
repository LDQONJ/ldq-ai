package work.daqian.ai.advisor;

import work.daqian.ai.chat.message.Message;

import java.util.List;
import java.util.Map;

/**
 * 增强器接口
 * @author LDQ
 */
public interface Advisor {

    /**
     * 在构建最终 Prompt 之前被调用，可以修改消息列表。
     * @param messages 当前的 Message 列表（可修改）
     * @param context  增强器上下文参数
     * @return 修改后的 Message 列表
     */
    List<Message> before(List<Message> messages, Map<String, Object> context);

    /**
     * 在收到模型回复之后被调用，可以做一些后置处理。
     * @param messages 模型回复后的完整上下文
     * @param context  增强器上下文参数
     * @return 处理后的回复
     */
    default List<Message> after(List<Message> messages, Map<String, Object> context) {
        return messages;
    }
}