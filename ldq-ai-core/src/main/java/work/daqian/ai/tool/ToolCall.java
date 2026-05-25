package work.daqian.ai.tool;

/**
 * 工具调用请求（模型生成）
 * @author LDQ
 */
public class ToolCall {
    /**
     * 本次调用唯一标识
     */
    private String id;
    /**
     * 工具类型
     */
    private String type;
    /**
     * 工具调用顺序（模型可能生成 ToolCall 数组）
     */
    private int index;
    /**
     * 工具调用方法签名
     */
    private Function function;

    public static class Function {
        /**
         * 工具名称
         */
        private String name;
        /**
         * 工具参数
         */
        private String arguments;

        public Function() {}

        public Function(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }

    public ToolCall() {}

    public ToolCall(String id, String type, int index, Function function) {
        this.id = id;
        this.type = type;
        this.index = index;
        this.function = function;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }
}