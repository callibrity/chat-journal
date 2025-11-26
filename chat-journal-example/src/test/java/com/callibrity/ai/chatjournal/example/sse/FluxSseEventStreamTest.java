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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class FluxSseEventStreamTest {

    @Mock
    private AsyncTaskExecutor taskExecutor;

    private FluxSseEventStream eventStream;

    @BeforeEach
    void setUp() {
        // Execute tasks synchronously for predictable test execution
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        eventStream = new FluxSseEventStream(taskExecutor);
    }

    @Nested
    class Stream {

        @Test
        void shouldReturnSseEmitter() {
            SseEmitter emitter = eventStream.stream("metadata", Flux.empty());

            assertThat(emitter).isNotNull();
        }

        @Test
        void shouldStreamWithDefaultChunkMapper() {
            SseEmitter emitter = eventStream.stream("metadata", Flux.just("chunk1", "chunk2"));

            assertThat(emitter).isNotNull();
        }

        @Test
        void shouldStreamWithCustomChunkMapper() {
            SseEmitter emitter = eventStream.stream(
                    "metadata",
                    Flux.just(1, 2, 3),
                    num -> "number-" + num
            );

            assertThat(emitter).isNotNull();
        }

        @Test
        void shouldHandleEmptyFlux() {
            SseEmitter emitter = eventStream.stream("metadata", Flux.<String>empty());

            assertThat(emitter).isNotNull();
        }

        @Test
        void shouldHandleFluxError() {
            SseEmitter emitter = eventStream.stream(
                    "metadata",
                    Flux.<String>error(new RuntimeException("Test error"))
            );

            assertThat(emitter).isNotNull();
        }
    }
}
