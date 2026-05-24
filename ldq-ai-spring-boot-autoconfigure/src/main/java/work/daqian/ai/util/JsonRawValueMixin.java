package work.daqian.ai.util;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public abstract class JsonRawValueMixin {
    @JsonSerialize(using = SmartJsonSerializer.class)
    abstract String getContent();
    @JsonSerialize(using = SmartJsonSerializer.class)
    abstract String getParameters();
    @JsonSerialize(using = SmartJsonSerializer.class)
    abstract String getArguments();
}