package work.daqian.ai.rag.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.rag.Document;
import work.daqian.ai.rag.EmbeddingModel;
import work.daqian.ai.rag.VectorStore;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 向量存储 Redis 实现，需要 Redis Server 包含 RediSearch 模块。<br/>
 * 推荐使用 Redis Stack 作为 Redis Server
 * @author LDQ
 */
public class RedisVectorStore implements VectorStore {

    private final JedisSupplier jedisSupplier;
    private final LdqAiProperties ldqAiProperties;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper mapper = new ObjectMapper();

    public static final String DOCUMENT_KEY_PREFIX = "doc:";

    @FunctionalInterface
    public interface JedisSupplier {
        Jedis get();
    }

    public RedisVectorStore(JedisSupplier jedisSupplier, EmbeddingModel embeddingModel, LdqAiProperties ldqAiProperties) {
        this.jedisSupplier = jedisSupplier;
        this.embeddingModel = embeddingModel;
        this.ldqAiProperties = ldqAiProperties;
    }

    @Override
    public void add(List<Document> documents) {
        try (Jedis jedis = jedisSupplier.get()) {
            for (Document doc : documents) {
                String key = DOCUMENT_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
                List<Float> vector = embeddingModel.embed(doc.getContent());
                byte[] vectorBytes = floatsToBytes(vector);

                Map<byte[], byte[]> hash = new HashMap<>();
                hash.put(SafeEncoder.encode("content"), SafeEncoder.encode(doc.getContent()));
                try {
                    hash.put(SafeEncoder.encode("metadata"),
                            SafeEncoder.encode(mapper.writeValueAsString(doc.getMetadata())));
                } catch (Exception e) {
                    hash.put(SafeEncoder.encode("metadata"), SafeEncoder.encode("{}"));
                }
                // 存储向量（二进制）
                hash.put(SafeEncoder.encode("embedding"), vectorBytes);

                jedis.hset(SafeEncoder.encode(key), hash);
            }
        }
    }

    @Override
    public List<Document> similaritySearch(Object query, long topK) {
        try (Jedis jedis = jedisSupplier.get()) {
            List<Float> qVec = embeddingModel.embed(query);
            byte[] queryBytes = floatsToBytes(qVec);

            // 组装 FT.SEARCH 参数
            byte[][] args = new byte[][]{
                    SafeEncoder.encode(ldqAiProperties.getRag().getIndex()),
                    SafeEncoder.encode("*=>[KNN " + topK + " @embedding $BLOB AS dist]"),
                    SafeEncoder.encode("PARAMS"), SafeEncoder.encode("2"),
                    SafeEncoder.encode("BLOB"), queryBytes,
                    SafeEncoder.encode("RETURN"), SafeEncoder.encode("3"),
                    SafeEncoder.encode("content"), SafeEncoder.encode("metadata"), SafeEncoder.encode("dist"),
                    SafeEncoder.encode("SORTBY"), SafeEncoder.encode("dist"),
                    SafeEncoder.encode("ASC"),
                    SafeEncoder.encode("LIMIT"), SafeEncoder.encode("0"), SafeEncoder.encode(String.valueOf(topK)),
                    SafeEncoder.encode("DIALECT"), SafeEncoder.encode("2")
            };

            // 发送命令（使用自定义 ProtocolCommand）
            Object response = jedis.sendCommand(FT_SEARCH_COMMAND, args);
            return parseSearchResult(response);
        }
    }

    /**
     * 解析搜索的响应
     * @param response 响应
     * @return 文档列表
     */
    @SuppressWarnings("unchecked")
    private List<Document> parseSearchResult(Object response) {
        if (!(response instanceof List)) return Collections.emptyList();
        List<Object> outer = (List<Object>) response;
        if (outer.size() < 2) return Collections.emptyList();

        List<Document> docs = new ArrayList<>();
        // 第一个元素是总数（TopK, Long），跳过
        // 后续每两个元素为一组：key（byte[]） 和 字段列表（List<byte[]>）
        for (int i = 1; i < outer.size(); i += 2) {
            // 文档 key
            byte[] keyBytes = (byte[]) outer.get(i);
            String key = new String(keyBytes);
            String id = key.startsWith(DOCUMENT_KEY_PREFIX) ? key.substring(DOCUMENT_KEY_PREFIX.length()) : key;

            // 字段 Map
            Map<String, String> fieldMap = getFieldMap(outer, i + 1);

            String content = fieldMap.get("content");
            String metaStr = fieldMap.get("metadata");
            Map<String, Object> metadata;
            try {
                metadata = mapper.readValue(metaStr, Map.class);
            } catch (Exception e) {
                metadata = Collections.emptyMap();
            }
            docs.add(new Document(id, content, metadata));
        }
        return docs;
    }

    /**
     * 从数组的第 i 个元素解析 key 对应的 value（map）
     * @param fieldMapList 字段 map 的集合
     * @param index 索引
     * @return 字段 map
     */
    @NonNull
    private Map<String, String> getFieldMap(List<Object> fieldMapList, int index) {
        // List 内容为 field, value, field, value ...
        List<Object> fieldsList = (List<Object>) fieldMapList.get(index);
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (int j = 0; j < fieldsList.size(); j += 2) {
            String fieldName = new String((byte[]) fieldsList.get(j));
            // 字段值可能是 String byte[] 或 Long（比如 dist 可能为数字，但通常 RediSearch 会将所有字段值以字节数组返回）
            byte[] valueBytes;
            Object valueObj = fieldsList.get(j + 1);
            if (valueObj instanceof byte[]) {
                valueBytes = (byte[]) valueObj;
            } else {
                valueBytes = String.valueOf(valueObj).getBytes();
            }
            fieldMap.put(fieldName, new String(valueBytes));
        }
        return fieldMap;
    }

    // 将 float 列表转为 FLOAT32 小端字节数组
    private byte[] floatsToBytes(List<Float> values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.size() * Float.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (Float v : values) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    private static final ProtocolCommand FT_SEARCH_COMMAND = () -> SafeEncoder.encode("FT.SEARCH");
}
