package work.daqian.ai.rag;

import java.util.List;

/**
 * 向量存储接口
 * @author LDQ
 */
public interface VectorStore {
    /**
     * 将文档添加到向量数据库
     * @param documents 文档集合
     */
    void add(List<Document> documents);
    /**
     * 相似度检索
     * @param query 检索内容
     * @param topK 检索结果数量
     * @return 文档集合
     */
    List<Document> similaritySearch(Object query, long topK);
}