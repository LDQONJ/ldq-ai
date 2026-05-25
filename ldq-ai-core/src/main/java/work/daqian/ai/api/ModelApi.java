package work.daqian.ai.api;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import work.daqian.ai.api.dto.ChatResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.Message;
import work.daqian.ai.enums.Provider;
import work.daqian.ai.tool.FunctionCallback;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 模型接口：统一不同模型供应商模型的调用
 * @author LDQ
 */
public interface ModelApi {

    /**
     * 获取 API 供应商
     * @return API 供应商
     */
    Provider getProvider();

    /**
     * 构建 WebClient
     * @return WebClient
     */
    WebClient buildWebClient();

    /**
     * 获取请求 URI
     * @param modelName 模型名称（部分 api 需要）
     * @return 请求 URI
     */
    String getChatUri(String modelName);

    /**
     * 获取向量模型请求 URI
     * @param modelName 模型名称（部分 api 需要）
     * @return 向量模型请求 URI
     */
    String getEmbeddingUri(String modelName);

    /**
     * 构建请求体
     * @param modelName 模型名称
     * @param messages 消息列表
     * @param tools 工具列表
     * @param stream 是否开启流式输出
     * @param enableThinking 是否开启思考模式
     * @param enableSearch 是否开启联网搜索（部分模型支持）
     * @return 请求体
     */
    Map<String, Object> buildRequest(String modelName, List<Message> messages, List<FunctionCallback> tools, boolean stream, boolean enableThinking, boolean enableSearch);

    /**
     * 构建请求体
     * @param modelName 模型名称
     * @param messages 消息列表
     * @param stream 是否开启流式输出
     * @return 请求体
     */
    default Map<String, Object> buildRequest(String modelName, List<Message> messages, boolean stream) {
        return buildRequest(modelName, messages, Collections.emptyList(), stream, false, false);
    }

    /**
     * 解析 API 返回的流式 chunk，生成包装后的流式响应
     * @param chunk API 响应的 chunk
     * @return 包装后的流式响应
     */
    Flux<ChatResponse> parseChunk(String chunk);

    /**
     * 获取接收 API 非流式响应的类型
     * @return 非流式响应类型
     */
    Class<? extends TextResponse> getTextResponseClass();
}
