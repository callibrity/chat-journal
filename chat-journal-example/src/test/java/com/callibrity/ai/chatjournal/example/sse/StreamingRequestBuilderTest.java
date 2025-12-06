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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamingRequestBuilderTest {

    private static final String CONVERSATION_ID = "conv-123";
    private static final String USER_PROMPT = "What is the weather?";

    private ChatClient chatClient;
    private TaskExecutor executor;
    private StreamingRequestBuilder builder;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);

        // Use SyncTaskExecutor for testing to execute synchronously
        executor = new SyncTaskExecutor();

        builder = new StreamingRequestBuilder(chatClient, executor, CONVERSATION_ID);
    }

    @Nested
    class FluentAPI {

        @Test
        void shouldReturnSelfForChaining() {
            var result = builder
                    .systemPrompt("test")
                    .onStart(convId -> "start")
                    .onChunk(chunk -> "chunk")
                    .onComplete(() -> "done")
                    .timeout(Duration.ofSeconds(30));

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class ConversationId {

        @Test
        void shouldReturnConversationId() {
            assertThat(builder.getConversationId()).isEqualTo(CONVERSATION_ID);
        }
    }

    @Nested
    class SystemPrompt {

        @Test
        void shouldAcceptStaticSystemPrompt() {
            var result = builder.systemPrompt("You are a helpful assistant");

            assertThat(result).isSameAs(builder);
        }

        @Test
        void shouldAcceptDynamicSystemPrompt() {
            var result = builder.systemPrompt(() -> "Dynamic prompt");

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class Handlers {

        @Test
        void shouldAcceptOnStartHandler() {
            var result = builder.onStart(convId -> new Metadata(convId));

            assertThat(result).isSameAs(builder);
        }

        @Test
        void shouldAcceptOnChunkHandler() {
            var result = builder.onChunk(Chunk::new);

            assertThat(result).isSameAs(builder);
        }

        @Test
        void shouldAcceptOnCompleteHandler() {
            var result = builder.onComplete(() -> new Done());

            assertThat(result).isSameAs(builder);
        }

        @Test
        void shouldChainAllHandlers() {
            var result = builder
                    .onStart(convId -> new Metadata(convId))
                    .onChunk(Chunk::new)
                    .onComplete(() -> new Done());

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class Timeout {

        @Test
        void shouldAcceptTimeout() {
            var result = builder.timeout(Duration.ofMinutes(5));

            assertThat(result).isSameAs(builder);
        }

        @Test
        void shouldAcceptZeroTimeout() {
            var result = builder.timeout(Duration.ZERO);

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class Advisors {

        @Test
        void shouldAcceptAdvisorCustomizer() {
            var result = builder.advisors(a -> a.param("custom", "value"));

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class BuilderChaining {

        @Test
        void shouldChainAllMethodsCalls() {
            var result = builder
                    .systemPrompt("You are helpful")
                    .timeout(Duration.ofSeconds(30))
                    .onStart(convId -> new Metadata(convId))
                    .onChunk(Chunk::new)
                    .onComplete(() -> new Done())
                    .advisors(a -> a.param("test", "value"));

            assertThat(result).isSameAs(builder);
        }
    }

    // Test data classes
    private record Metadata(String conversationId) {}
    private record Chunk(String content) {}
    private record Done() {}
}
