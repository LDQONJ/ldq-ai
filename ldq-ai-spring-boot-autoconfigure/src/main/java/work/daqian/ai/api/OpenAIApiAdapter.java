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
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.tool.FunctionCallback;
import work.daqian.ai.tool.ToolCall;

import java.util.*;
import java.util.stream.Collectors;

public class OpenAIApiAdapter implements ModelApi {

    private static final Logger log = LoggerFactory.getLogger(OpenAIApiAdapter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final LdqAiProperties ldqAiProperties;
    private final WebClient.Builder builder;

    public OpenAIApiAdapter(WebClient.Builder builder, LdqAiProperties ldqAiProperties) {
        this.ldqAiProperties = ldqAiProperties;
        this.builder = builder;
    }

    @Override
    public Provider getProvider() {
        return Provider.OPENAI;
    }

    @Override
    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.newConnection();
        WebClient.Builder finalBuilder = builder.baseUrl(ldqAiProperties.getOpenAi().getBaseUrl());
        for (LdqAiProperties.OpenAI.Header header : ldqAiProperties.getOpenAi().getHeaders()) {
            finalBuilder = finalBuilder.defaultHeader(header.getHeaderName(), header.getHeaderValue());
        }
        finalBuilder = finalBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
        return finalBuilder.build();
    }

    public String getChatUri(String modelName) {
        return "/chat/completions";
    }

    @Override
    public String getEmbeddingUri(String modelName) {
        return "/embeddings";
    }

    @Override
    public Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> functions, boolean stream, boolean enableThinking, boolean enableSearch) {
        Map<String, Object> requestMap = new HashMap<>(10);
        requestMap.put("model", modelName);
        requestMap.put("messages", messages);
        if (functions != null && !functions.isEmpty()) {
            List<Map<String, Object>> tools = functions.stream()
                    .map(function -> {
                        Map<String, Object> tool = new HashMap<>();
                        tool.put("type", "function");
                        tool.put("function", function);
                        return tool;
                    }).collect(Collectors.toList());
            requestMap.put("tools", tools);
        }
        requestMap.put("stream", stream);
        if (stream) {
            requestMap.put("stream_options", Collections.singletonMap("include_usage", true));
        }
        Map<String, Object> extraBody = new HashMap<>();
        if (ldqAiProperties.getOpenAi().getBaseUrl().contains("deepseek")) {
            extraBody.put("thinking", Collections.singletonMap("type", enableThinking ? "enabled" : "disabled"));
            requestMap.put("reasoning_effort", "high");
        } else {
            extraBody.put("enable_thinking", enableThinking);
        }
        if (enableSearch)
            extraBody.put("enable_search", true);
        requestMap.put("extra_body", extraBody);
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
                    result.add(new ChatResponse("content", content));
                    // contentBuilder.append(content);
                }
            }
            return Flux.fromIterable(result);
        } catch (Exception e) {
            log.warn("解析 OpenAI Api chunk 失败: {}", e.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public Class<? extends TextResponse> getTextResponseClass() {
        return OpenAITextResponse.class;
    }

    public static class OpenAITextResponse implements TextResponse {
        private List<Choice> choices;

        public OpenAITextResponse() {
        }

        public OpenAITextResponse(List<Choice> choices) {
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
            Choice choice = choices.get(0);
            Message message = choice.getMessage();
            return message.getContent();
        }

        @Override
        public String getThinking() {
            Choice choice = choices.get(0);
            Message message = choice.getMessage();
            return message.getReasoningContent();
        }

        @Override
        public List<ToolCall> getToolCalls() {
            Choice choice = choices.get(0);
            Message message = choice.getMessage();
            return message.getToolCalls();
        }

        public static class Choice {
            private Message message;
            private String finishReason;

            public Choice() {
            }

            public Choice(Message message, String finishReason) {
                this.message = message;
                this.finishReason = finishReason;
            }

            public Message getMessage() {
                return message;
            }

            public void setMessage(Message message) {
                this.message = message;
            }

            public String getFinishReason() {
                return finishReason;
            }

            public void setFinishReason(String finishReason) {
                this.finishReason = finishReason;
            }
        }

        public static class Message {

            private String content;

            private String reasoningContent;

            private List<ToolCall> toolCalls;

            public Message() {
            }

            public Message(String content, String reasoningContent, List<ToolCall> toolCalls) {
                this.content = content;
                this.toolCalls = toolCalls;
                this.reasoningContent = reasoningContent;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }

            public String getReasoningContent() {
                return reasoningContent;
            }

            public void setReasoningContent(String reasoningContent) {
                this.reasoningContent = reasoningContent;
            }

            public List<ToolCall> getToolCalls() {
                return toolCalls;
            }

            public void setToolCalls(List<ToolCall> toolCalls) {
                this.toolCalls = toolCalls;
            }
        }
    }
}