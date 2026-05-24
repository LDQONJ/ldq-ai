package work.daqian.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分割器
 */
public class TextSplitter {

    /**
     * 分割文档
     * @param document 原文档
     * @param chunkSize 分割后每一块的字符数
     * @param overlap 重叠的字符数
     * @return 分割后的文档列表
     */
    public List<Document> split(Document document, int chunkSize, int overlap) {
        String text = document.getContent();
        List<Document> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunkText = text.substring(start, end);
            Document chunk = new Document(
                    chunkText,
                    document.getMetadata()  // 复制元数据
            );
            chunks.add(chunk);
            start += (chunkSize - overlap);
        }
        return chunks;
    }
}