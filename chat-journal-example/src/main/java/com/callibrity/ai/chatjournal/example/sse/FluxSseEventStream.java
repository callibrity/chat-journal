package com.callibrity.ai.chatjournal.example.sse;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class FluxSseEventStream {

    private static final String METADATA_EVENT = "metadata";
    private static final String CHUNK_EVENT = "chunk";
    private static final String DONE_EVENT = "done";

    private final AsyncTaskExecutor executor;
    private final Function<SseEmitter, SseEventSender> senderFactory;

    public FluxSseEventStream(AsyncTaskExecutor executor) {
        this(executor, SseEmitterEventSender::new);
    }

    public FluxSseEventStream(AsyncTaskExecutor executor, Function<SseEmitter, SseEventSender> senderFactory) {
        this.executor = executor;
        this.senderFactory = senderFactory;
    }

    public <M> SseEmitter stream(M metadata, Flux<String> flux) {
        return stream(metadata, flux, Chunk::new);
    }

    public <M, T> SseEmitter stream(M metadata, Flux<T> flux, Function<T, Object> chunkMapper) {
        var emitter = new SseEmitter(0L);
        var sender = senderFactory.apply(emitter);

        executor.execute(() -> streamContent(sender, metadata, flux, chunkMapper));

        return emitter;
    }

    <M, T> void streamContent(SseEventSender sender, M metadata, Flux<T> flux, Function<T, Object> chunkMapper) {
        if (!sender.send(METADATA_EVENT, metadata)) {
            return;
        }

        flux.subscribe(
                item -> sender.send(CHUNK_EVENT, chunkMapper.apply(item)),
                sender::completeWithError,
                () -> {
                    sender.send(DONE_EVENT, "");
                    sender.complete();
                }
        );
    }
}
