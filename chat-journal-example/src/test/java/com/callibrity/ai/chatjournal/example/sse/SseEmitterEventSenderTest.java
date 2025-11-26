/*
 * Copyright © 2025 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
