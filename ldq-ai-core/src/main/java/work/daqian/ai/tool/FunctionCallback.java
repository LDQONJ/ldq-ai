package work.daqian.ai.tool;

import java.util.Map;

/**
 * 函数调用接口，可自定义工具实现类
 * @author LDQ
 */
public interface FunctionCallback {

    /**
     * 工具名称
     */
    String getName();

    /**
     * 工具描述
     */
    String getDescription();

    /**
     * 工具的输入规范
     * <p>格式规范:<p/>
     * <pre>{
     *     "name": "get_current_weather",
     *     "description": "当你想查询指定城市的天气时非常有用。",
     *     "parameters": {
     *         "type": "object",
     *         "properties": {
     *             "location": {
     *                 "type": "string",
     *                 "description": "城市或县区，比如北京市、余杭区等。"
     *             }
     *         },
     *         "required": ["location"]
     *     }
     * }<pre/>
     * */
    String getParameters();

    /**
     * 执行工具的逻辑
     * @param argumentsJson 工具调用参数
     * @param toolContext 工具上下文参数
     * @return 工具调用结果
     */
    String call(String argumentsJson, Map<String, Object> toolContext);
}