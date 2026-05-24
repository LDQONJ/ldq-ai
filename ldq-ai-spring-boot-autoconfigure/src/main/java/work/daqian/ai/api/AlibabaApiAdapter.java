package work.daqian.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import work.daqian.ai.api.dto.ChatResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.enums.Provider;
import work.daqian.ai.tool.FunctionCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlibabaApiAdapter implements ModelApi {

    private static final Logger log = LoggerFactory.getLogger(AlibabaApiAdapter.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient.Builder builder;
    private final String apiKey;

    public AlibabaApiAdapter(WebClient.Builder builder, String apiKey) {
        this.builder = builder;
        this.apiKey = apiKey;
    }

    @Override
    public Provider getProvider() {
        return Provider.ALIBABA;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl("https://dashscope.aliyuncs.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("X-DashScope-SSE", "enable")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getChatUri(String modelName) {
        return "/compatible-mode/v1/chat/completions";
    }

    public String getMultiUri() {
        return "/api/v1/services/aigc/multimodal-generation/generation";
    }

    @Override
    public String getEmbeddingUri(String modelName) {
        return "/compatible-mode/v1/embeddings";
    }

    @Override
    public Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> tools, boolean stream, boolean enableThinking, boolean enableSearch) {
        Map<String, Object> requestMap = new HashMap<>(10);
        requestMap.put("model", modelName);
        requestMap.put("messages", messages);
        requestMap.put("reasoning_effort", "high");
        requestMap.put("stream", stream);
        if (stream) {
            Map<String, Object> streamOptions = new HashMap<>();
            streamOptions.put("include_usage", true);
            requestMap.put("stream_options", streamOptions);
        }
        requestMap.put("enable_thinking", enableThinking);
        return requestMap;
    }

    @Override
    public Flux<ChatResponse> parseChunk(String chunk) {
        try {
            List<ChatResponse> result = new ArrayList<>();
            String[] lines = chunk.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if ("[DONE]".equals(line)) continue;

                JsonNode node = mapper.readTree(line);

                JsonNode choices = node.path("choices");
                if (choices.isEmpty()) {
                    JsonNode usageJson = node.path("usage");
                    int promptTokens = usageJson.path("prompt_tokens").asInt();
                    int completionTokens = usageJson.path("completion_tokens").asInt();
                    int totalTokens = usageJson.path("total_tokens").asInt();
                    int reasoningTokens = 0;
                    JsonNode completionTokensDetails = usageJson.path("completion_tokens_details");
                    if (completionTokensDetails != null)
                        reasoningTokens = completionTokensDetails.path("reasoning_tokens").asInt();
                    int cachedTokens = 0;
                    JsonNode promptTokensDetails = usageJson.path("prompt_tokens_details");
                    if (promptTokensDetails != null)
                        cachedTokens = promptTokensDetails.path("cached_tokens").asInt();
                    String modelName = node.path("model").asText();
                    ChatResponse.Usage usage = new ChatResponse.Usage(modelName, promptTokens, completionTokens, totalTokens, reasoningTokens, cachedTokens);
                    result.add(new ChatResponse("usage", usage.toString()));
                    continue;
                }

                JsonNode delta = choices.get(0).path("delta");

                String reasoning = delta.path("reasoning_content").asText(null);
                if (reasoning != null && !reasoning.isEmpty()) {
                    result.add(new ChatResponse("thinking", reasoning));
                    // thinkingBuilder.append(reasoning);
                }

                String content = delta.path("content").asText(null);
                if (content != null && !content.isEmpty()) {
                    log.debug("当前 content chunk：“{}”", content);
                    result.add(new ChatResponse("content", content));
                    // contentBuilder.append(content);
                }
            }

            return Flux.fromIterable(result);
        } catch (Exception e) {
            log.warn("解析阿里云 Api chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    public Flux<ChatResponse> parseAudioChunk(String chunk, Long userId, String sessionId) {
        try {
            List<ChatResponse> result = new ArrayList<>();
            String[] lines = chunk.split("\n");

            for (String line : lines) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("id") || line.startsWith("event") || line.startsWith(":"))
                    continue;

                JsonNode node = mapper.readTree(line);

                JsonNode output = node.path("output");

                JsonNode audio = output.path("audio");

                JsonNode data = audio.path("data");

                String audioPart = data.asText(null);

                if (audioPart != null && !audioPart.isEmpty()) {
                    result.add(new ChatResponse("tts", audioPart));
                }
            }

            return Flux.fromIterable(result);
        } catch (Exception e) {
            log.warn("解析阿里云 TTS Api chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Class<? extends TextResponse> getTextResponseClass() {
        return AlibabaTextResponse.class;
    }

    static class AlibabaTextResponse implements TextResponse {
        private List<Choice> choices;

        public AlibabaTextResponse() {}

        public AlibabaTextResponse(List<Choice> choices) {
            this.choices = choices;
        }

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        @Override
        public String getContent() {
            return choices.get(0).getMessage().getContent();
        }

        public static class Choice {
            private Message message;

            public Choice() {}

            public Choice(Message message) {
                this.message = message;
            }

            public Message getMessage() {
                return message;
            }

            public void setMessage(Message message) {
                this.message = message;
            }
        }

        public static class Message {
            private String content;

            public Message() {}

            public Message(String content) {
                this.content = content;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }
        }
    }
}
