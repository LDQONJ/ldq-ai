package work.daqian.ai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import work.daqian.ai.advisor.Advisor;
import work.daqian.ai.api.dto.StreamResponse;
import work.daqian.ai.api.dto.TextResponse;
import work.daqian.ai.chat.message.*;
import work.daqian.ai.tool.FunctionCallback;
import work.daqian.ai.tool.ToolCall;
import work.daqian.ai.tool.ToolFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 封装请求调用模型
 * @author LDQ
 */
public class ChatClient {
    private static final Executor taskExecutor;

    static {
        taskExecutor = Executors.newCachedThreadPool();
    }

    private final ChatModel chatModel;
    private final String defaultSystem;
    private final List<Advisor> defaultAdvisors;
    private final List<FunctionCallback> defaultTools;
    private final Map<String, Object> defaultToolContext;

    private ChatClient(Builder builder) {
        this.chatModel = builder.chatModel;
        this.defaultSystem = builder.defaultSystem;
        this.defaultAdvisors = builder.defaultAdvisors;
        this.defaultTools = builder.defaultTools;
        this.defaultToolContext = builder.defaultToolContext;
    }

    // 每次调用返回一个新的 PromptSpec
    public PromptSpec prompt() {
        return new PromptSpec(chatModel, defaultSystem, defaultAdvisors, defaultTools, defaultToolContext);
    }

    public Builder mutate() {
        return new Builder(chatModel).defaultSystem(defaultSystem);
    }

    /**
     * ChatClient 构造器
     */
    public static class Builder {
        private final ChatModel chatModel;
        /**
         * 默认系统提示词
         */
        private String defaultSystem;
        /**
         * 默认增强器
         */
        private final List<Advisor> defaultAdvisors = new ArrayList<>();
        /**
         * 默认工具
         */
        private final List<FunctionCallback> defaultTools = new ArrayList<>();
        /**
         * 默认工具上下文
         */
        private final Map<String, Object> defaultToolContext = new HashMap<>();

        public Builder(ChatModel chatModel) {
            this.chatModel = chatModel;
        }

        /**
         * 当前 Client 默认的系统提示词
         */
        public Builder defaultSystem(String system) {
            this.defaultSystem = system;
            return this;
        }

        /**
         * 当前 Client 默认的增强功能
         */
        public Builder defaultAdvisors(Advisor... advisors) {
            this.defaultAdvisors.addAll(Arrays.asList(advisors));
            return this;
        }

        /**
         * 当前 Client 默认的可用工具
         */
        public Builder defaultTools(Object... toolObjs) {
            List<FunctionCallback> tools = Arrays.stream(toolObjs)
                    .map(ToolFactory::create)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            this.defaultTools.addAll(tools);
            return this;
        }

        /**
         * 当前 Client 默认的可用工具
         */
        public Builder defaultTools(FunctionCallback... tools) {
            this.defaultTools.addAll(Arrays.asList(tools));
            return this;
        }

        public Builder defaultToolContext(Map<String, Object> toolContext) {
            this.defaultToolContext.putAll(toolContext);
            return this;
        }

        public ChatClient build() {
            return new ChatClient(this);
        }
    }

    /**
     * 使用模型创建 ChatClient 构造器
     * @param chatModel 模型
     * @return 构造器
     */
    public static Builder builder(ChatModel chatModel) {
        return new Builder(chatModel);
    }

    public static class PromptSpec {
        private final ChatModel chatModel;
        private final String defaultSystem;
        private final List<Advisor> defaultAdvisors;
        private final List<Advisor> requestAdvisors = new ArrayList<>();
        private final Map<String, Object> advisorContext = new HashMap<>();
        private final List<FunctionCallback> defaultTools;
        private final List<FunctionCallback> requestTools = new ArrayList<>();
        private final Map<String, Object> defaultToolContext;
        private final Map<String, Object> requestToolContext = new HashMap<>();
        private String system;
        private String user;
        private final List<Message> messages = new ArrayList<>();
        private boolean enableThinking = false;
        private boolean withThinkingContent = false;

        PromptSpec(ChatModel chatModel, String defaultSystem, List<Advisor> defaultAdvisors, List<FunctionCallback> defaultTools, Map<String, Object> defaultToolContext) {
            this.chatModel = chatModel;
            this.defaultSystem = defaultSystem;
            this.defaultAdvisors = defaultAdvisors;
            this.defaultTools = defaultTools;
            this.defaultToolContext = defaultToolContext;
        }

        public PromptSpec system(String text) {
            this.system = text;
            return this;
        }

        public PromptSpec user(String text) {
            this.user = text;
            return this;
        }

        public PromptSpec advisors(Advisor... advisors) {
            requestAdvisors.addAll(Arrays.asList(advisors));
            return this;
        }

        // 向 Advisor 上下文添加参数（例如 conversationId、userMessage、topK）
        public PromptSpec advisorParam(String key, Object value) {
            advisorContext.put(key, value);
            return this;
        }

        public PromptSpec tools(Object... toolObjs) {
            List<FunctionCallback> tools = Arrays.stream(toolObjs)
                    .map(ToolFactory::create)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            requestTools.addAll(tools);
            return this;
        }

        public PromptSpec tools(FunctionCallback... tools) {
            requestTools.addAll(Arrays.asList(tools));
            return this;
        }

        public PromptSpec toolContext(Map<String, Object> toolContext) {
            requestToolContext.putAll(toolContext);
            return this;
        }


        /**
         * 开启思考模式（默认关闭）
         * @param withThinkingContent 是否将思考内容添加到流式响应中
         * @return PromptSpec
         */
        public PromptSpec enableThinking(boolean withThinkingContent) {
            this.enableThinking = true;
            this.withThinkingContent = withThinkingContent;
            return this;
        }

        public PromptSpec messages(Message... messages) {
            this.messages.addAll(Arrays.asList(messages));
            return this;
        }

        /**
         * 阻塞调用模型获取文本响应
         * @return AI 消息
         */
        public String call() {
            List<Advisor> allAdvisors = new ArrayList<>();
            allAdvisors.addAll(defaultAdvisors);
            allAdvisors.addAll(requestAdvisors);
            List<Message> messages = buildFinalMessages(allAdvisors);
            // 调用模型
            TextResponse response = chatModel.call(messages, null, enableThinking);
            String content = response.getContent();
            messages.add(new AssistantMessage(content));
            // 执行 after 链
            CompletableFuture.runAsync(() -> {
                for (Advisor advisor : allAdvisors) {
                    advisor.after(messages, advisorContext);
                }
            }, taskExecutor);
            return content;
        }

        /**
         * 实时获取流式响应
         * @return 流式响应对象（流式 Flux + 拼接 Mono）
         */
        public StreamResponse stream() {
            List<Advisor> allAdvisors = new ArrayList<>();
            allAdvisors.addAll(defaultAdvisors);
            allAdvisors.addAll(requestAdvisors);
            List<Message> messages = buildFinalMessages(allAdvisors);
            // 调用模型
            StreamResponse streamResponse = chatModel.stream(messages, null, enableThinking, withThinkingContent);
            // 执行 after 链（异步，不阻塞流式响应）
            CompletableFuture.runAsync(() -> {
                String content = streamResponse.getContent();
                messages.add(new AssistantMessage(content));
                for (Advisor advisor : allAdvisors) {
                    advisor.after(messages, advisorContext);
                }
            }, taskExecutor);
            return streamResponse;
        }

        /**
         * 构建最终请求消息列表，会执行所有的增强器的 before 方法并调用工具
         * @param allAdvisors 增强器列表
         * @return 最终消息列表
         */
        private List<Message> buildFinalMessages(List<Advisor> allAdvisors) {
            List<Message> finalMessages = new ArrayList<>();
            if (messages.isEmpty()) {
                if (defaultSystem != null && system == null) {
                    finalMessages.add(new SystemMessage(defaultSystem));
                }
                if (system != null) {
                    finalMessages.add(new SystemMessage(system));
                }
                if (user != null) {
                    finalMessages.add(new UserMessage(user));
                }
            } else {
                finalMessages.addAll(messages);
            }
            // 执行 before 链
            for (Advisor advisor : allAdvisors) {
                finalMessages = advisor.before(finalMessages, advisorContext);
            }
            List<FunctionCallback> allTools = new ArrayList<>();
            allTools.addAll(defaultTools);
            allTools.addAll(requestTools);
            Map<String, Object> allToolContext = new HashMap<>();
            allToolContext.putAll(defaultToolContext);
            allToolContext.putAll(requestToolContext);
            if (!allTools.isEmpty()) {
                doToolCall(finalMessages, allTools, allToolContext);
            }
            return finalMessages;
        }

        private final ObjectMapper mapper = new ObjectMapper();

        private void doToolCall(List<Message> finalMessages, List<FunctionCallback> tools, Map<String, Object> toolContext) {
            Map<String, FunctionCallback> toolMap = tools.stream()
                    .collect(Collectors.toMap(FunctionCallback::getName, tool -> tool));
            List<Message> toolCallMessages = new ArrayList<>(finalMessages);
            for (int i = 0; i < 5; i++) {
                // 模型可能返回 ToolCall, List<ToolCall>, 自然语言
                TextResponse response = chatModel.call(toolCallMessages, tools, false);
                List<ToolCall> toolCalls = response.getToolCalls();
                if (toolCalls.isEmpty()) return;
                AssistantMessage assistantRequest = new AssistantMessage(response.getThinking(), toolCalls);
                toolCallMessages.add(assistantRequest);
                List<ToolMessage> toolMessages = toolCalls.stream().map(toolCall -> {
                    ToolCall.Function function = toolCall.getFunction();
                    FunctionCallback tool = toolMap.get(function.getName());
                    String toolResult = tool.call(function.getArguments(), toolContext);
                    return new ToolMessage(toolResult, toolCall.getId());
                }).collect(Collectors.toList());
                if (toolMessages.isEmpty()) {
                    // 工具响应为空
                    toolCallMessages.add(new ToolMessage("工具暂时不可用", null));
                    continue;
                }
                toolCallMessages.addAll(toolMessages);
                finalMessages.add(assistantRequest);
                finalMessages.addAll(toolMessages);
            }
        }
    }
}