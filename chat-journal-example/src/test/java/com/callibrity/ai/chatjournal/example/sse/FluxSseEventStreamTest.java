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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluxSseEventStreamTest {

    @Mock
    private SseEventSender sender;

    @Mock
    private AsyncTaskExecutor taskExecutor;

    private FluxSseEventStream eventStream;

    @BeforeEach
    void setUp() {
        // Execute tasks synchronously for predictable test execution
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        eventStream = new FluxSseEventStream(taskExecutor, emitter -> sender);
    }

    @Nested
    class StreamContent {

        @BeforeEach
        void setUp() {
            lenient().when(sender.send(eq("metadata"), any())).thenReturn(true);
            lenient().when(sender.send(eq("chunk"), any())).thenReturn(true);
            lenient().when(sender.send(eq("done"), any())).thenReturn(true);
        }

        @Test
        void shouldSendMetadataFirst() {
            var metadata = new TestMetadata("test-id");

            eventStream.stream(metadata, Flux.<String>empty());

            verify(sender).send("metadata", metadata);
        }

        @Test
        void shouldSendChunksInOrder() {
            eventStream.stream("metadata", Flux.just("chunk1", "chunk2", "chunk3"));

            InOrder inOrder = inOrder(sender);
            inOrder.verify(sender).send("metadata", "metadata");
            inOrder.verify(sender).send("chunk", new Chunk("chunk1"));
            inOrder.verify(sender).send("chunk", new Chunk("chunk2"));
            inOrder.verify(sender).send("chunk", new Chunk("chunk3"));
            inOrder.verify(sender).send("done", "");
            inOrder.verify(sender).complete();
        }

        @Test
        void shouldSendDoneAndCompleteAfterAllChunks() {
            when(sender.send(eq("chunk"), any(Chunk.class))).thenReturn(true);
            eventStream.stream("metadata", Flux.just("chunk"));

            InOrder inOrder = inOrder(sender);
            inOrder.verify(sender).send("done", "");
            inOrder.verify(sender).complete();
        }

        @Test
        void shouldApplyChunkMapper() {
            eventStream.stream(
                    "metadata",
                    Flux.just(1, 2, 3),
                    num -> "number-" + num
            );

            verify(sender).send("chunk", "number-1");
            verify(sender).send("chunk", "number-2");
            verify(sender).send("chunk", "number-3");
        }

        @Test
        void shouldUseCustomMapperWithNonStringFlux() {
            var customObject = new TestChunk("data");

            eventStream.stream("metadata", Flux.just(customObject), item -> item);

            verify(sender).send("chunk", customObject);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void shouldStopIfMetadataFails() {
            when(sender.send(eq("metadata"), any())).thenReturn(false);

            eventStream.stream("metadata", Flux.just("chunk"));

            verify(sender, never()).send(eq("chunk"), any());
            verify(sender, never()).complete();
        }

        @Test
        void shouldCompleteWithErrorOnFluxError() {
            when(sender.send(eq("metadata"), any())).thenReturn(true);
            var error = new RuntimeException("Flux error");

            eventStream.stream("metadata", Flux.<String>error(error));

            verify(sender).completeWithError(error);
        }
    }

    @Nested
    class DefaultConstructor {

        @Test
        void shouldCreateWithDefaultSenderFactory() {
            var stream = new FluxSseEventStream(taskExecutor);
            SseEmitter emitter = stream.stream("metadata", Flux.empty());

            assertThat(emitter).isNotNull();
        }
    }

    record TestMetadata(String id) {}
    record TestChunk(String data) {}
}
