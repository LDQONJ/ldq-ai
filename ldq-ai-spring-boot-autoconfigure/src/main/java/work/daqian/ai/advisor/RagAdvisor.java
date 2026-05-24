package work.daqian.ai.advisor;

import work.daqian.ai.chat.message.Message;
import work.daqian.ai.chat.message.SystemMessage;
import work.daqian.ai.chat.message.UserMessage;
import work.daqian.ai.rag.Document;
import work.daqian.ai.rag.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagAdvisor implements Advisor{

    private final VectorStore vectorStore;
    private final long topK;
    private final String contextPrompt;   // 可以自定义前置提示词

    public static final String TOP_KEY = "topK";

    public RagAdvisor(VectorStore vectorStore, long topK) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.contextPrompt = "请根据以下参考资料回答问题：\n";
    }

    @Override
    public List<Message> before(List<Message> messages, Map<String, Object> context) {
        // 找到最后一条用户消息用于检索
        Object query = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                query = msg.getContent();
                break;
            }
        }
        if (query == null) return messages;

        // 检索相关文档
        List<Document> docs;
        if (context.containsKey(TOP_KEY)) {
            docs = vectorStore.similaritySearch(query, (Long) context.get(TOP_KEY));
        } else {
            docs = vectorStore.similaritySearch(query, topK);
        }

        if (docs.isEmpty()) return messages;

        // 构建上下文文本
        String docContext = docs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String augmentedSystemMessage = contextPrompt + docContext;

        // 将上下文作为 SystemMessage 插入到消息列表最前面
        List<Message> augmented = new ArrayList<>();
        augmented.add(new SystemMessage(augmentedSystemMessage));
        augmented.addAll(messages);
        return augmented;
    }
}
