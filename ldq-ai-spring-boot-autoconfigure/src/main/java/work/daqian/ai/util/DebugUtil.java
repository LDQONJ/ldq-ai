package work.daqian.ai.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.daqian.ai.chat.message.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DebugUtil {

    public static final Logger log = LoggerFactory.getLogger(DebugUtil.class);

    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.addMixIn(Message.class, JsonRawValueMixin.class);
    }

    /**
     * 打印 JSON 格式请求体
     * @param request 请求体对象
     */
    public static void debugRequest(Map<String, Object> request) {
        String json;
        try {
            json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.err.println(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) +
                "\n>>>>>>>>>>>>>>>>>>>本次请求体内容\n" +
                json +
                "\n>>>>>>>>>>>>>>>>>>请求体内容结束"
        );
    }
}
