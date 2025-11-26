package com.callibrity.ai.chatjournal.example.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SseEmitterEventSenderTest {

    private SseEmitter emitter;
    private SseEmitterEventSender sender;

    @BeforeEach
    void setUp() {
        emitter = mock(SseEmitter.class);
        sender = new SseEmitterEventSender(emitter);
    }

    @Test
    void shouldReturnTrueOnSuccessfulSend() {
        boolean result = sender.send("event", "data");

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseAndCompleteWithErrorOnIOException() throws IOException {
        doThrow(new IOException("Connection lost")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        boolean result = sender.send("event", "data");

        assertThat(result).isFalse();
        verify(emitter).completeWithError(any(IOException.class));
    }

    @Test
    void shouldDelegateComplete() {
        sender.complete();

        verify(emitter).complete();
    }

    @Test
    void shouldDelegateCompleteWithError() {
        var error = new RuntimeException("Test error");

        sender.completeWithError(error);

        verify(emitter).completeWithError(error);
    }
}
