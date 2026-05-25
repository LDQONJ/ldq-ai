package work.daqian.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

/**
 * ldq-ai 模型调用流式响应 chunk 类
 */
public class ChatResponse {
    /**
     * 响应类型
     */
    private String type;
    /**
     * 响应内容
     */
    @JsonRawValue
    private String content;
    /**
     * token 用量
     */

    public ChatResponse() {}

    public ChatResponse(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static class Usage {
        /**
         * 模型名称（id）
         */
        private String modelName;
        /**
         * 输入 token 量
         */
        private int promptTokens;
        /**
         * 输出 token 量
         */
        private int completionTokens;
        /**
         * 总 token 量
         */
        private int totalTokens;
        /**
         * 思考 token 量
         */
        private int reasoningTokens;
        /**
         * 命中缓存 token 量
         */
        private int cachedTokens;

        public Usage() {}

        public Usage(String modelName, int promptTokens, int completionTokens, int totalTokens, int reasoningTokens, int cachedTokens) {
            this.modelName = modelName;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
            this.reasoningTokens = reasoningTokens;
            this.cachedTokens = cachedTokens;
        }

        @Override
        public String toString() {
            return  "{\"modelName\":\"" + modelName +
                    "\",\"promptTokens\":" + promptTokens +
                    ",\"completionTokens\":" + completionTokens +
                    ",\"totalTokens\":" + totalTokens +
                    ",\"reasoningTokens\":" + reasoningTokens +
                    ",\"cachedTokens\":" + cachedTokens + "}";
        }
    }
}
