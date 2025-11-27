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
package com.callibrity.ai.chatjournal.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.ChatClientRequestSpec userSpec;

    @Mock
    private ChatClient.AdvisorSpec advisorSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    @Captor
    private ArgumentCaptor<String> userMessageCaptor;

    @Captor
    private ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorConsumerCaptor;

    private final TaskExecutor executor = new SyncTaskExecutor();

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatClient, executor);
    }

    @Nested
    class Stream {

        @BeforeEach
        void setUpMocks() {
            when(chatClient.prompt()).thenReturn(requestSpec);
            when(requestSpec.user(anyString())).thenReturn(userSpec);
            when(userSpec.advisors(any(Consumer.class))).thenReturn(userSpec);
            when(userSpec.stream()).thenReturn(streamResponseSpec);
            when(streamResponseSpec.content()).thenReturn(Flux.just("Response"));
        }

        @Test
        void shouldReturnSseEmitter() {
            SseEmitter emitter = controller.stream("What is AI?", null);

            assertThat(emitter).isNotNull();
        }

        @Test
        void shouldPassUserQuestionToChatClient() {
            controller.stream("What is AI?", null);

            verify(requestSpec).user(userMessageCaptor.capture());
            assertThat(userMessageCaptor.getValue()).isEqualTo("What is AI?");
        }

        @Test
        void shouldUseProvidedConversationId() {
            controller.stream("Question", "my-conversation-id");

            verify(userSpec).advisors(advisorConsumerCaptor.capture());
            advisorConsumerCaptor.getValue().accept(advisorSpec);
            verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, "my-conversation-id");
        }

        @Test
        void shouldGenerateConversationIdWhenNotProvided() {
            controller.stream("Question", null);

            verify(userSpec).advisors(advisorConsumerCaptor.capture());
            advisorConsumerCaptor.getValue().accept(advisorSpec);

            ArgumentCaptor<String> conversationIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(advisorSpec).param(any(), conversationIdCaptor.capture());

            assertThat(conversationIdCaptor.getValue()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }
    }

    @Nested
    class MetadataRecord {

        @Test
        void shouldCreateMetadataWithConversationId() {
            var metadata = new ChatController.Metadata("test-id");
            assertThat(metadata.conversationId()).isEqualTo("test-id");
        }
    }

    @Nested
    class ChunkRecord {

        @Test
        void shouldCreateChunkWithContent() {
            var chunk = new ChatController.Chunk("test content");
            assertThat(chunk.content()).isEqualTo("test content");
        }
    }
}
