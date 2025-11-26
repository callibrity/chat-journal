package com.callibrity.ai.chatjournal.example.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SseEmitterEventSender implements SseEventSender {

    private final SseEmitter emitter;

    public SseEmitterEventSender(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public boolean send(String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException e) {
            emitter.completeWithError(e);
            return false;
        }
    }

    @Override
    public void complete() {
        emitter.complete();
    }

    @Override
    public void completeWithError(Throwable error) {
        emitter.completeWithError(error);
    }
}
