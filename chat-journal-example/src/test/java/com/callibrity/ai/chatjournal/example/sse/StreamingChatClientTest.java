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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StreamingChatClientTest {

    private static final String CONVERSATION_ID = "conv-123";

    private ChatClient chatClient;
    private TaskExecutor executor;
    private StreamingChatClient streamingChatClient;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        executor = mock(TaskExecutor.class);
        streamingChatClient = new StreamingChatClient(chatClient, executor);
    }

    @Nested
    class Construction {

        @Test
        void shouldCreateStreamingChatClient() {
            assertThat(streamingChatClient).isNotNull();
        }
    }

    @Nested
    class Stream {

        @Test
        void shouldReturnStreamingRequestBuilder() {
            var builder = streamingChatClient.stream(CONVERSATION_ID);

            assertThat(builder)
                    .isNotNull()
                    .isInstanceOf(StreamingRequestBuilder.class);
        }

        @Test
        void shouldCreateNewBuilderForEachCall() {
            var builder1 = streamingChatClient.stream(CONVERSATION_ID);
            var builder2 = streamingChatClient.stream(CONVERSATION_ID);

            assertThat(builder1).isNotSameAs(builder2);
        }

        @Test
        void shouldSupportDifferentConversationIds() {
            var builder1 = streamingChatClient.stream("conv-1");
            var builder2 = streamingChatClient.stream("conv-2");

            assertThat(builder1)
                    .isNotNull()
                    .isNotSameAs(builder2);
            assertThat(builder2).isNotNull();
        }

        @Test
        void shouldUseProvidedConversationId() {
            var builder = streamingChatClient.stream("my-conversation");

            assertThat(builder.getConversationId()).isEqualTo("my-conversation");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        void shouldGenerateUuidWhenConversationIdIsNullOrBlank(String conversationId) {
            var builder = streamingChatClient.stream(conversationId);

            assertThat(builder.getConversationId())
                    .isNotNull()
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        void shouldGenerateDifferentUuidsForEachNullConversationId() {
            var builder1 = streamingChatClient.stream(null);
            var builder2 = streamingChatClient.stream(null);

            assertThat(builder1.getConversationId()).isNotEqualTo(builder2.getConversationId());
        }
    }
}
