package work.daqian.ai.rag;

import java.util.List;

public interface EmbeddingModel {
    /**
     * 批量将文本向量化
     * @param texts 文本集合（数量不超过 10，单个文本 token 不超过 8192）
     * @return 所有文本的向量数组集合
     */
    List<List<Float>> embed(List<String> texts);

    /**
     * 将文本向量化
     * @param text 文本（token 不超过 8192）
     * @return 向量数组
     */
    List<Float> embed(Object text);
}
