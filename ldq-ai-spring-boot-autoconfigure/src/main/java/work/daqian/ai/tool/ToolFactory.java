package work.daqian.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import work.daqian.ai.tool.annotation.Tool;
import work.daqian.ai.tool.annotation.ToolParam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具创建工厂
 * @author LDQ
 */
public class ToolFactory {

    private static final Logger log = LoggerFactory.getLogger(ToolFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 工具元数据缓存
     */
    private static final Map<Class<?>, List<ToolMeta>> toolMetaCache = new ConcurrentHashMap<>();
    /**
     * 方法元数据缓存
     */
    private static final Map<Method, MethodMeta> methodMetaCache = new ConcurrentHashMap<>();

    /**
     * 从目标对象中提取所有 @Tool 注解标注的方法，生成工具列表
     * @param target 目标对象
     * @return 工具列表
     */
    public static List<FunctionCallback> create(Object target) {
        List<ToolMeta> toolMetas = getToolMetas(target);
        return toolMetas.stream().map(toolMeta -> new FunctionCallback() {
            @Override
            public String getName() {
                return toolMeta.getName();
            }

            @Override
            public String getDescription() {
                return toolMeta.getDescription();
            }

            @Override
            public String getParameters() {
                return toolMeta.getParameters();
            }

            @Override
            public String call(String argumentsJson, Map<String, Object> toolContext) {
                Method method = toolMeta.getMethod();
                Object[] args = parseArguments(method, argumentsJson, toolContext);
                try {
                    return method.invoke(target, args).toString();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }).collect(Collectors.toList());
    }

    /**
     * 从缓存中获取对象中所有工具的工具元数据列表
     * @param target 目标对象
     * @return 工具元数据列表
     */
    private static List<ToolMeta> getToolMetas(Object target) {
        return toolMetaCache.computeIfAbsent(AopUtils.getTargetClass(target), clazz -> {
            List<ToolMeta> metas = new ArrayList<>();
            for (Method method : clazz.getMethods()) {
                Tool toolAnno = method.getAnnotation(Tool.class);
                if (toolAnno == null) continue;
                String name = toolAnno.name().isEmpty() ? method.getName() : toolAnno.name();
                String description = toolAnno.description();
                String parameters = generateParameters(method);
                metas.add(new ToolMeta(name, description, parameters, method));
            }
            return metas;
        });
    }

    /**
     * 从缓存中获取方法元数据（参数列表、类型列表）
     * @param method 方法
     * @param params 参数数组
     * @return 方法元数据
     */
    private static MethodMeta getMethodMeta(Method method, Parameter[] params) {
        return methodMetaCache.computeIfAbsent(method, m -> {
            ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
            String[] parameterNames = discoverer.getParameterNames(m);
            List<String> paramNames = new ArrayList<>(params.length);
            List<Class<?>> paramTypes = new ArrayList<>(params.length);
            for (int i = 0; i < params.length; i++) {
                Parameter param = params[i];
                ToolParam tp = param.getAnnotation(ToolParam.class);
                String paramName = "";
                if (tp != null)
                    paramName = tp.name();
                if (paramName.isEmpty())
                    paramName = param.getName();
                if (paramName.startsWith("arg")) {
                    if (parameterNames != null) {
                        paramName = parameterNames[i];
                    } else {
                        throw new RuntimeException("无法解析参数名称，考虑使用 @ToolParam(name = \"\") 显示设置参数名称");
                    }
                }
                Class<?> paramType = param.getType();
                paramNames.add(paramName);
                paramTypes.add(paramType);
            }
            return new MethodMeta(paramNames, paramTypes);
        });
    }

    /**
     * 生成 api 要求的工具参数规范
     * @param method 工具方法
     * @return 参数规范
     */
    private static String generateParameters(Method method) {
        StringBuilder parameters = new StringBuilder("{\"type\":\"object\",\"properties\":{");
        StringBuilder required = new StringBuilder("\"required\": [");
        // 构建 properties（key: 参数名称 value: 参数类型、描述）
        Parameter[] params = method.getParameters();
        MethodMeta methodMeta = getMethodMeta(method, params);
        List<String> paramNames = methodMeta.getParamNames();
        List<Class<?>> paramTypes = methodMeta.getParamTypes();
        for (int i = 0; i < params.length; i++) {
            String paramName = paramNames.get(i);
            Class<?> paramType = paramTypes.get(i);
            if (paramName.equals("toolContext") && paramType.equals(Map.class)) {
                continue;
            }
            required.append("\"").append(paramName).append("\"");
            String description = "";
            ToolParam tp = method.getAnnotation(ToolParam.class);
            if (tp != null) description = tp.description();
            parameters.append("\"").append(paramName).append("\":{");
            parameters.append("\"type\":\"").append(getTypeStr(paramType)).append("\"");
            if (!description.isEmpty()) parameters.append(",\"description\":\"").append(description).append("\"");
            parameters.append("}");
            parameters.append(",");
            required.append(",");
        }
        parameters.setLength(parameters.length() - 1);
        required.setLength(required.length() - 1);
        required.append("]");
        parameters.append("},").append(required).append("}");
        return parameters.toString();
    }

    /**
     * 将类型转换成符合 api 要求的字符串
     * @param type 原 Java 类型
     * @return 类型对应的字符串
     */
    private static String getTypeStr(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        return "string";
    }

    /**
     * 将参数 Map 转换为方法的参数数组
     * @param method 方法
     * @param argumentsJson 参数 Map Json
     * @return 参数数组
     */
    private static Object[] parseArguments(Method method, String argumentsJson, Map<String, Object> toolContext) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        Map argumentMap = null;
        try {
            argumentMap = mapper.readValue(argumentsJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        MethodMeta methodMeta = getMethodMeta(method, params);
        List<String> paramNames = methodMeta.getParamNames();
        List<Class<?>> paramTypes = methodMeta.getParamTypes();
        for (int i = 0; i < params.length; i++) {
            String paramName = paramNames.get(i);
            Class<?> paramType = paramTypes.get(i);
            if (paramName.equals("toolContext") && paramType.equals(Map.class)) {
                args[i] = toolContext;
                continue;
            }
            Object value = argumentMap.get(paramName);
            if (value == null) continue;
            // 类型转换
            if (paramType == String.class) args[i] = value.toString();
            else if (paramType == int.class || paramType == Integer.class) args[i] = Integer.valueOf(value.toString());
            else if (paramType == double.class || paramType == Double.class) args[i] = Double.valueOf(value.toString());
            else if (paramType == boolean.class || paramType == Boolean.class)
                args[i] = Boolean.valueOf(value.toString());
            else args[i] = mapper.convertValue(value, paramType);
        }
        return args;
    }

    @Tool(name = "test", description = "测试工具工厂")
    public String toolMethod(@ToolParam(name = "city", description = "城市或者区县名，如北京、深圳、南山等") String city,
                             @ToolParam(name = "version", description = "版本号，可选: v9或v63，其中v9是查询最近7天天气，v63是查询当日天气") String version,
                             Map<String, Object> toolContext) {
        return null;
    }

    public static void main(String[] args) throws Exception {
        ToolFactory toolFactory = new ToolFactory();
        List<FunctionCallback> functionCallbacks = create(toolFactory);
        // System.in.read();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(functionCallbacks));
    }

    public static class ToolMeta {
        /**
         * 工具名称
         */
        private final String name;
        /**
         * 工具描述
         */
        private final String description;
        /**
         * 工具参数规范
         */
        private final String parameters;
        /**
         * 工具对应的方法
         */
        private final Method method;

        public ToolMeta(String name, String description, String parameters, Method method) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.method = method;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getParameters() {
            return parameters;
        }

        public Method getMethod() {
            return method;
        }
    }

    public static class MethodMeta {
        /**
         * 方法参数名称集合
         */
        private final List<String> paramNames;
        /**
         * 方法参数类型集合
         */
        private final List<Class<?>> paramTypes;

        public MethodMeta(List<String> paramNames, List<Class<?>> paramTypes) {
            this.paramNames = paramNames;
            this.paramTypes = paramTypes;
        }

        public List<String> getParamNames() {
            return paramNames;
        }

        public List<Class<?>> getParamTypes() {
            return paramTypes;
        }
    }
}