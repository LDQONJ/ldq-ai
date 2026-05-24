package work.daqian.ai.api.dto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class StreamResponse {
    /**
     * 实时流式输出
     */
    private final Flux<ChatResponse> chatResponseFlux;
    /**
     * 完整回复内容 Mono，可订阅添加回调
     */
    private final Mono<String> contentMono;
    /**
     * 完整思考内容 Mono，可订阅添加回调
     */
    private final Mono<String> thinkingMono;

    public StreamResponse(Flux<ChatResponse> chatResponseFlux, Mono<String> contentMono, Mono<String> thinkingMono) {
        this.chatResponseFlux = chatResponseFlux;
        this.contentMono = contentMono;
        this.thinkingMono = thinkingMono;
    }

    public Flux<ChatResponse> getChatResponseFlux() {
        return chatResponseFlux;
    }

    public Mono<String> getContentMono() {
        return contentMono;
    }

    public Mono<String> getThinkingMono() {
        return thinkingMono;
    }

    /**
     * 阻塞获取完整思考内容
     * <p>建议在其他线程中调用，否则会阻塞流式输出</p>
     * @return 思考内容，无内容时返回 null
     */
    public String getThinking() {
        return getMonoResult(this.thinkingMono);
    }

    /**
     * 阻塞获取完整回复内容
     * <p>建议在其他线程中调用，否则会阻塞流式输出</p>
     * @return 回复内容，无内容时返回 null
     */
    public String getContent() {
        return getMonoResult(this.contentMono);
    }

    private String getMonoResult(Mono<String> mono) {
        List<String> results = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mono.subscribe(result -> {
                    if (!result.isEmpty())
                        results.add(result);
                    countDownLatch.countDown();
                });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (results.isEmpty()) return null;
        return results.get(0);
    }
}
