package work.daqian.ai.api;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.ai.api.dto.ChatResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.enums.Provider;
import work.daqian.ai.tool.FunctionCallback;

import java.util.List;
import java.util.Map;

/**
 * 模型接口：统一不同模型供应商模型的调用
 */
public interface ModelApi {

    /**
     * 适配器支持的平台
     */
    Provider getProvider();

    /**
     * 构建 WebClient
     */
    WebClient buildWebClient();

    /**
     * 获取请求 URI，可能包含动态参数（模型名称等）
     */
    String getChatUri(String modelName);

    /**
     * 获取向量模型请求 URI，可能包含动态参数（模型名称等）
     */
    String getEmbeddingUri(String modelName);

    /**
     * 构建请求体
     */
    Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> tools, boolean stream, boolean enableThinking, boolean enableSearch);

    /**
     * 解析流式 chunk，返回给前端的事件流（同步操作，内部通过回调收集 token）
     */
    Flux<ChatResponse> parseChunk(String chunk);

    Class<? extends TextResponse> getTextResponseClass();
}
