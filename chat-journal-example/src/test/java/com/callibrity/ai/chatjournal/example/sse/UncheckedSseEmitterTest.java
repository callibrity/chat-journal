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
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UncheckedSseEmitterTest {

    private SseEmitter emitter;
    private UncheckedSseEmitter uncheckedEmitter;

    @BeforeEach
    void setUp() {
        emitter = mock(SseEmitter.class);
        uncheckedEmitter = UncheckedSseEmitter.of(emitter);
    }

    @Test
    void shouldDelegateToEmitterOnSend() throws IOException {
        uncheckedEmitter.send("event", "data");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldCompleteWithErrorAndThrowUncheckedIOExceptionOnIOException() throws IOException {
        var ioException = new IOException("Connection lost");
        doThrow(ioException).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertThatThrownBy(() -> uncheckedEmitter.send("event", "data"))
                .isInstanceOf(UncheckedIOException.class)
                .hasCause(ioException);

        verify(emitter).completeWithError(ioException);
    }

    @Test
    void shouldDelegateComplete() {
        uncheckedEmitter.complete();

        verify(emitter).complete();
    }
}
