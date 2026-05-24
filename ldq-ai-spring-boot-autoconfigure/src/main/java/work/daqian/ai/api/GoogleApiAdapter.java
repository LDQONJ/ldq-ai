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
import work.daqian.ai.chat.message.SystemMessage;
import work.daqian.ai.enums.Provider;
import work.daqian.ai.tool.FunctionCallback;

import java.util.*;

public class GoogleApiAdapter implements ModelApi {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiAdapter.class);

    private final WebClient.Builder builder;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public GoogleApiAdapter(WebClient.Builder builder, String apiKey) {
        this.builder = builder;
        this.apiKey = apiKey;
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        return builder.baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public String getChatUri(String modelName) {
        return "/v1beta/models/" + modelName + ":streamGenerateContent?alt=sse";
    }

    @Override
    public String getEmbeddingUri(String modelName) {
        throw new RuntimeException("未实现");
    }

    @Override
    public Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> tools, boolean stream, boolean enableThinking, boolean enableSearch) {
        boolean lite = modelName.contains("lite");
        if (lite) enableThinking = false;
        List<Map<String, String>> systemPrompts = new ArrayList<>(5);
        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("role", "system");
        systemInstruction.put("parts", systemPrompts);
        List<Map<String, Object>> contents = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message instanceof SystemMessage) {
                systemPrompts.add(Collections.singletonMap("text", message.getContent()));
            } else {
                Map<String, Object> content = new HashMap<>(2);
                content.put("role", message.getRole().equals("assistant") ? "model" : message.getRole());
                content.put("parts", Collections.singletonList(Collections.singletonMap("text", message.getContent())));
                contents.add(content);
            }
        }
        Map<String, Object> thinkingConfig = new HashMap<>(5);
        thinkingConfig.put("includeThoughts", enableThinking);
        if (!modelName.contains("2.5")) thinkingConfig.put("thinkingLevel", "high");
        Map<String, Map<String, Object>> generationConfig = Collections.singletonMap(
                "thinkingConfig", thinkingConfig
        );
        Map<String, Object> requestMap = new HashMap<>(5);
        requestMap.put("contents", contents);

        if (!lite)
            requestMap.put("generationConfig", generationConfig);
        if (!systemPrompts.isEmpty())
            requestMap.put("systemInstruction", systemInstruction);

        // ChatUtil.debugRequestBody(requestMap);
        return requestMap;
    }

    @Override
    public Flux<ChatResponse> parseChunk(String chunk) {
        try {
            List<ChatResponse> result = new ArrayList<>();
            String line = chunk.trim();

            if (line.isEmpty()) return Flux.empty();
            if ("[DONE]".equals(line)) return Flux.empty();

            JsonNode node = mapper.readTree(line);

            JsonNode candidates = node.path("candidates");
            if (candidates.isEmpty()) return Flux.empty();

            JsonNode firstCandidate = candidates.get(0);
            if (firstCandidate == null) return Flux.empty();

            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isEmpty()) return Flux.empty();

            String finishReason = firstCandidate.path("finishReason").asText(null);

            for (JsonNode part : parts) {
                String text = part.path("text").asText(null);
                boolean isThought = part.path("thought").asBoolean(false);
                if (text != null && !text.isEmpty()) {
                    if (isThought) {
                        result.add(new ChatResponse("thinking", text));
                    } else {
                        result.add(new ChatResponse("content", text));
                    }
                }
            }

            if ("STOP".equals(finishReason)) {
                JsonNode usageMeta = node.path("usageMetadata");
                if (!usageMeta.isMissingNode() && !usageMeta.isNull()) {
                    String modelName = node.path("modelVersion").asText();
                    int promptTokens = usageMeta.path("promptTokenCount").asInt();
                    int totalTokens = usageMeta.path("totalTokenCount").asInt();
                    int completionTokens = usageMeta.path("candidatesTokenCount").asInt();
                    int reasoningTokens = usageMeta.path("thoughtsTokenCount").asInt();
                    ChatResponse.Usage usage = new ChatResponse.Usage(modelName, promptTokens, completionTokens, totalTokens, reasoningTokens, 0);
                    result.add(new ChatResponse("usage", usage.toString()));
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            log.warn("解析 Google chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Class<? extends TextResponse> getTextResponseClass() {
        return GoogleTextResponse.class;
    }

    static class GoogleTextResponse implements TextResponse {

        private List<Candidate> candidates;

        public GoogleTextResponse() {
        }

        public GoogleTextResponse(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        public List<Candidate> getCandidates() {
            return candidates;
        }

        public void setCandidates(List<Candidate> candidates) {
            this.candidates = candidates;
        }

        static class Candidate {
            private Content content;

            public Candidate() {
            }

            public Candidate(Content content) {
                this.content = content;
            }

            public Content getContent() {
                return content;
            }

            public void setContent(Content content) {
                this.content = content;
            }
        }

        static class Content {
            private List<Part> parts;

            public Content() {
            }

            public Content(List<Part> parts) {
                this.parts = parts;
            }

            public List<Part> getParts() {
                return parts;
            }

            public void setParts(List<Part> parts) {
                this.parts = parts;
            }
        }

        static class Part {
            private String text;

            public Part() {
            }

            public Part(String text) {
                this.text = text;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }

        @Override
        public String getContent() {
            return candidates.get(0).getContent().getParts().get(0).getText();
        }
    }
}
