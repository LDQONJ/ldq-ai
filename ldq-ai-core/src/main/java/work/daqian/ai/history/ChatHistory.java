package work.daqian.ai.history;

import work.daqian.ai.chat.message.Message;

import java.util.List;

/**
 * 聊天历史接口
 * @author LDQ
 */
public interface ChatHistory {

    /**
     * 获取指定会话最近的历史消息，按顺序返回
     * @param conversationId 对话 id
     * @param lastN 消息数量
     * @return 消息列表
     */
    List<Message> get(String conversationId, int lastN);

    /**
     * 向指定会话中添加一组消息
     * @param conversationId 对话 id
     * @param messages 消息列表
     */
    void add(String conversationId, List<Message> messages);

    /**
     * 清空会话
     * @param conversationId 对话 id
     */
    void clear(String conversationId);
}