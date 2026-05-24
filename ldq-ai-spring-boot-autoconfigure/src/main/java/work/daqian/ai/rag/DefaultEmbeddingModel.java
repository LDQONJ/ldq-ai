package work.daqian.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.daqian.ai.api.ModelApi;
import work.daqian.ai.properties.LdqAiProperties;
import work.daqian.ai.enums.Provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmbeddingModel.class);

    private final LdqAiProperties ldqAiProperties;

    private final Map<Provider, ModelApi> modelAdapterMap;

    public DefaultEmbeddingModel(List<ModelApi> modelApis, LdqAiProperties ldqAiProperties) {
        this.ldqAiProperties = ldqAiProperties;
        this.modelAdapterMap = modelApis.stream()
                .collect(Collectors.toMap(ModelApi::getProvider, modelApi -> modelApi));
    }

    @Override
    public List<List<Float>> embed(List<String> texts) {
        List<Map<String, Object>> datas = doEmbed(texts);
        return datas.stream()
                .map(data -> {
                    List<Number> embedding = (List<Number>) data.get("embedding");
                    return embedding.stream()
                            .map(Number::floatValue)
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Float> embed(Object text) {
        List<Map<String, Object>> data = doEmbed(text);
        return ((List<Number>) data.get(0).get("embedding")).stream()
                .map(Number::floatValue)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> doEmbed(Object input) {
        String embeddingModel = ldqAiProperties.getEmbedding().getModel();
        Provider embeddingProvider = ldqAiProperties.getEmbedding().getProvider();
        ModelApi modelApi = modelAdapterMap.get(embeddingProvider);
        HashMap<Object, Object> request = new HashMap<>();
        request.put("model", embeddingModel);
        request.put("input", input);
        request.put("dimensions", ldqAiProperties.getRag().getDimensions());
        request.put("encoding_format", "float");

        Map<String, Object> resp = modelApi.buildWebClient().post()
                .uri(modelApi.getEmbeddingUri(embeddingModel))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> datas = (List<Map<String, Object>>) resp.get("data");
        return datas;
    }
}