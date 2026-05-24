package work.daqian.ai.chat.message;

import java.util.Objects;

/**
 * 系统提示词
 */
public class SystemMessage extends Message {

    public SystemMessage() {}

    public SystemMessage(String content) {
        super.role = "system";
        super.content = content;
    }

    @Override
    public String getRole() {
        return "system";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SystemMessage that = (SystemMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(content);
    }

    @Override
    public String toString() {
        return "SystemMessage{" +
                "content='" + content + '\'' +
                '}';
    }
}
