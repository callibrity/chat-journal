package com.callibrity.ai.chatjournal.example.sse;

public interface SseEventSender {

    boolean send(String eventName, Object data);

    void complete();

    void completeWithError(Throwable error);
}
