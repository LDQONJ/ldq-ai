package work.daqian.ai.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import work.daqian.ai.enums.Provider;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ldq-ai")
public class LdqAiProperties {

    private Chat chat;

    private OpenAI openAi;

    private Embedding embedding;

    private ApiKey apiKey;

    private Rag rag;

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public OpenAI getOpenAi() {
        return openAi;
    }

    public void setOpenAi(OpenAI openAi) {
        this.openAi = openAi;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public Rag getRag() {
        return rag;
    }

    public void setRag(Rag rag) {
        this.rag = rag;
    }

    public static class Chat {
        /**
         * 模型名称
         */
        private String model = "qwen3.5:9b";
        /**
         * 模型供应商
         */
        private Provider provider = Provider.OLLAMA;
        /**
         * 历史上下文窗口大小
         */
        private int lastN = 20;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public int getLastN() {
            return lastN;
        }

        public void setLastN(int lastN) {
            this.lastN = lastN;
        }


    }

    public static class OpenAI {
        /**
         * OpenAI 兼容接口 URL
         */
        private String baseUrl = "";

        private List<Header> headers = new ArrayList<>();

        public static class Header {
            private String headerName;

            private String headerValue;

            public String getHeaderName() {
                return headerName;
            }

            public void setHeaderName(String headerName) {
                this.headerName = headerName;
            }

            public String getHeaderValue() {
                return headerValue;
            }

            public void setHeaderValue(String headerValue) {
                this.headerValue = headerValue;
            }
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public List<Header> getHeaders() {
            return headers;
        }

        public void setHeaders(List<Header> headers) {
            this.headers = headers;
        }
    }

    public static class Embedding {
        /**
         * 模型名称
         */
        private String model = "qwen3-embedding:4b-q8_0";
        /**
         * 模型供应商
         */
        private Provider provider = Provider.OLLAMA;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    public static class ApiKey {
        /**
         * 阿里巴巴 API Key
         */
        private String alibaba = "alibaba";
        /**
         * DeepSeek API Key
         */
        private String deepseek = "deepseek";
        /**
         * 谷歌 API Key
         */
        private String google = "google";
        /**
         * Ollama API Key
         */
        private String ollama = "ollama";

        public String getAlibaba() {
            return alibaba;
        }

        public void setAlibaba(String alibaba) {
            this.alibaba = alibaba;
        }

        public String getGoogle() {
            return google;
        }

        public void setGoogle(String google) {
            this.google = google;
        }

        public String getOllama() {
            return ollama;
        }

        public void setOllama(String ollama) {
            this.ollama = ollama;
        }

        public String getDeepseek() {
            return deepseek;
        }

        public void setDeepseek(String deepseek) {
            this.deepseek = deepseek;
        }
    }

    public static class Rag {
        /**
         * 索引库名称
         */
        private String index = "ldq-ai-docs";
        /**
         * 检索默认 Top K
         */
        private long topK = 5;
        /**
         * 向量维度
         */
        private int dimensions = 1024;

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public long getTopK() {
            return topK;
        }

        public void setTopK(long topK) {
            this.topK = topK;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }
}
