package work.daqian.ai.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 模型供应商
 */
public enum Provider {
    OLLAMA(1, "本地Ollama模型"),
    RESTRICT(2, "限制模型"),
    ALIBABA(3, "阿里云模型api"),
    GOOGLE(4, "谷歌模型api"),
    OPENAI(5, "OpenAI兼容api")
    ;

    private final int value;
    @JsonValue
    private final String desc;

    Provider(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    public static Provider fromValue(int value) {
        for (Provider provider : Provider.values()) {
            if (provider.getValue() == value)
                return provider;
        }
        throw new RuntimeException("未知模型提供商");
    }

    public static Provider fromStr(String str) {
        for (Provider provider : Provider.values()) {
            if (provider.getDesc().equals(str))
                return provider;
        }
        throw new RuntimeException("未知模型提供商");
    }
}
