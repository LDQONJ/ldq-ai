package work.daqian.ai.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class OllamaApiAdapter implements ModelApi {

    private static final String BASE_URL = "http://127.0.0.1:11434";
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient.Builder builder;

    public OllamaApiAdapter(WebClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public Provider getProvider() {
        return Provider.OLLAMA;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getChatUri(String modelName) {
        return "/api/chat";
    }

    @Override
    public String getEmbeddingUri(String modelName) {
        return "/v1/embeddings";
    }

    @Override
    public Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> tools, boolean stream, boolean enableThinking, boolean enableSearch) {
        Map<String, Object> request = new HashMap<>(4);
        request.put("model", modelName);
        request.put("messages", messages);
        request.put("stream", stream);
        request.put("think", enableThinking);
        return request;
    }

    @Override
    public Flux<ChatResponse> parseChunk(String chunk) {
        try {
            String[] lines = chunk.split("\n");
            List<ChatResponse> result = new ArrayList<>();
            // synchronized (contentBuilder) {
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                JsonNode node = mapper.readTree(line);
                JsonNode message = node.path("message");

                String thinking = message.path("thinking").asText(null);
                if (thinking != null && !thinking.isEmpty()) {
                    result.add(new ChatResponse("thinking", thinking));
                }
                String content = message.path("content").asText(null);
                if (content != null && !content.isEmpty()) {
                    result.add(new ChatResponse("content", content));
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            return Flux.empty();
        }
    }

    @Override
    public Class<? extends TextResponse> getTextResponseClass() {
        return OllamaTextResponse.class;
    }

    static class OllamaTextResponse implements TextResponse {
        private Message message;

        public OllamaTextResponse() {}

        public OllamaTextResponse(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        @Override
        public String getContent() {
            return message.getContent();
        }
    }
}
