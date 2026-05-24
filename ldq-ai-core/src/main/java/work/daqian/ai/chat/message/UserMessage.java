package work.daqian.ai.chat.message;

import java.util.Objects;

/**
 * 用户消息
 */
public class UserMessage extends Message {

    public UserMessage() {}

    public UserMessage(String content) {
        super.role = "user";
        super.content = content;
    }

    @Override
    public String getRole() {
        return "user";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserMessage that = (UserMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public String toString() {
        return "UserMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}
