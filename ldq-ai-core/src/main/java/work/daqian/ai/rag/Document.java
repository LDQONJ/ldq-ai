package work.daqian.ai.rag;

import java.util.Map;

/**
 * RAG 向量数据库文档
 */
public class Document {
    /**
     * 文档 id
     */
    private String id;
    /**
     * 文档内容
     */
    private final String content;
    /**
     * 文档元数据
     */
    private final Map<String, Object> metadata;

    public Document(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public Document(String id, String content, Map<String, Object> metadata) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Document{id='" + id + "', content='" + content + "'}";
    }
}