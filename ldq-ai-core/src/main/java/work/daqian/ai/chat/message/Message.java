package work.daqian.ai.chat.message;

/**
 * 通用消息
 */
public class Message {
    /**
     * 消息角色
     */
    String role;
    /**
     * 消息内容
     */
    String content;

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setContent(String content) {
        this.content = content;
    }
}