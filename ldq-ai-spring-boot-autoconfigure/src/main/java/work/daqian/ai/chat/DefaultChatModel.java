package work.daqian.ai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import work.daqian.ai.api.ModelApi;
import work.daqian.ai.api.dto.ChatResponse;
import work.daqian.ai.api.dto.StreamResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.enums.Provider;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.tool.FunctionCallback;
import work.daqian.ai.util.DebugUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(DefaultChatModel.class);

    private final LdqAiProperties ldqAiProperties;
    private final Map<Provider, ModelApi> modelAdapterMap;

    public DefaultChatModel(List<ModelApi> modelApis, LdqAiProperties ldqAiProperties) {
        this.ldqAiProperties = ldqAiProperties;
        this.modelAdapterMap = modelApis.stream()
                .collect(Collectors.toMap(ModelApi::getProvider, adapter -> adapter));
    }

    public StreamResponse stream(List<Message> messages, List<FunctionCallback> tools, boolean enableThinking, boolean withThinkingContent) {
        String model = ldqAiProperties.getChat().getModel();
        Provider provider = ldqAiProperties.getChat().getProvider();
        ModelApi modelApi = modelAdapterMap.get(provider);
        Map<String, Object> request = modelApi.buildRequest(
                model,
                messages,
                tools,
                true,
                enableThinking,
                false
        );
        DebugUtil.debugRequest(request);
        // 缓存原始流中所有历史元素，不会出现一个订阅者过滤操作影响其他订阅者的结果
        Flux<ChatResponse> stream = modelApi.buildWebClient()
                .post().uri(modelApi.getChatUri(model))
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request).retrieve()
                .bodyToFlux(String.class)
                .flatMap(modelApi::parseChunk)
                .filter(response -> response.getContent() != null && !response.getContent().isEmpty())
                .cache();
        // 最终流式响应 Flux
        Flux<ChatResponse> chatFlux = stream
                .filter(response -> !"thinking".equals(response.getType()) || withThinkingContent)
                .concatWith(Flux.just(new ChatResponse("done", null)));
        // 完整思考内容 Mono
        Mono<String> thinkingMono = stream
                .filter(chatResponse -> "thinking".equals(chatResponse.getType()))
                .map(ChatResponse::getContent)
                .reduce("", String::concat)
                .cache();
        // 完整回复内容 Mono
        Mono<String> contentMono = stream
                .filter(chatResponse -> "content".equals(chatResponse.getType()))
                .map(ChatResponse::getContent)
                .reduce("", String::concat)
                .cache();
        return new StreamResponse(chatFlux, contentMono, thinkingMono);
    }

    public TextResponse call(List<Message> messages, List<FunctionCallback> tools, boolean think) {
        String model = ldqAiProperties.getChat().getModel();
        Provider provider = ldqAiProperties.getChat().getProvider();
        ModelApi modelApi = modelAdapterMap.get(provider);
        Map<String, Object> request = modelApi.buildRequest(
                model,
                messages,
                tools,
                false,
                think,
                false
        );
        DebugUtil.debugRequest(request);
        return modelApi.buildWebClient()
                .post().uri(modelApi.getChatUri(model))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(modelApi.getTextResponseClass())
                .block();
    }
}
