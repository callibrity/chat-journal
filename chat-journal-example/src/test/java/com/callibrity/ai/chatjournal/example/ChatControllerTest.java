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

import com.callibrity.ai.chatjournal.example.sse.StreamingChatClient;
import com.callibrity.ai.chatjournal.example.sse.StreamingRequestBuilder;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsage;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsageProvider;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private StreamingChatClient streamingChatClient;

    @Mock
    private ChatMemoryUsageProvider memoryUsageProvider;

    @Mock
    private ChatJournalEntryRepository entryRepository;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(streamingChatClient, memoryUsageProvider, entryRepository);
    }

    @Nested
    class Stream {

        @Test
        void shouldDelegateToStreamingChatClient() {
            var mockBuilder = mock(StreamingRequestBuilder.class);
            when(mockBuilder.onStart(any())).thenReturn(mockBuilder);
            when(mockBuilder.onChunk(any())).thenReturn(mockBuilder);
            when(mockBuilder.onComplete(any())).thenReturn(mockBuilder);
            when(mockBuilder.execute(anyString())).thenReturn(mock(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class));
            when(streamingChatClient.stream(anyString())).thenReturn(mockBuilder);

            controller.stream("What is AI?", "conv-123");

            verify(streamingChatClient).stream("conv-123");
        }

        @Test
        void shouldPassNullConversationIdToStreamingChatClient() {
            var mockBuilder = mock(StreamingRequestBuilder.class);
            when(mockBuilder.onStart(any())).thenReturn(mockBuilder);
            when(mockBuilder.onChunk(any())).thenReturn(mockBuilder);
            when(mockBuilder.onComplete(any())).thenReturn(mockBuilder);
            when(mockBuilder.execute(anyString())).thenReturn(mock(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class));
            when(streamingChatClient.stream(any())).thenReturn(mockBuilder);

            controller.stream("What is AI?", null);

            verify(streamingChatClient).stream(null);
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

    @Nested
    class DoneRecord {

        @Test
        void shouldCreateDoneWithMemoryUsageStats() {
            var done = new ChatController.Done(500, 1000, 50.0);
            assertThat(done.currentTokens()).isEqualTo(500);
            assertThat(done.maxTokens()).isEqualTo(1000);
            assertThat(done.percentageUsed()).isEqualTo(50.0);
        }
    }

    @Nested
    class History {

        @Test
        void shouldReturnHistoryResponse() {
            var entries = List.of(
                    new ChatJournalEntry(2, "ASSISTANT", "Hello!", 10),
                    new ChatJournalEntry(1, "USER", "Hi", 5)
            );
            when(entryRepository.findVisibleEntries("conv-123", 0, 50)).thenReturn(entries);
            when(entryRepository.countVisibleEntries("conv-123")).thenReturn(2);
            when(memoryUsageProvider.getMemoryUsage("conv-123"))
                    .thenReturn(new ChatMemoryUsage(500, 1000));

            var response = controller.history("conv-123", 0, 50);

            assertThat(response.messages()).hasSize(2);
            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.currentTokens()).isEqualTo(500);
            assertThat(response.maxTokens()).isEqualTo(1000);
            assertThat(response.percentageUsed()).isEqualTo(50.0);
        }

        @Test
        void shouldMapEntriesToMessages() {
            var entries = List.of(
                    new ChatJournalEntry(1, "USER", "Hello", 5),
                    new ChatJournalEntry(2, "ASSISTANT", "Hi there!", 10)
            );
            when(entryRepository.findVisibleEntries("conv-123", 0, 50)).thenReturn(entries);
            when(entryRepository.countVisibleEntries("conv-123")).thenReturn(2);
            when(memoryUsageProvider.getMemoryUsage("conv-123"))
                    .thenReturn(new ChatMemoryUsage(100, 1000));

            var response = controller.history("conv-123", 0, 50);

            assertThat(response.messages().get(0).type()).isEqualTo("USER");
            assertThat(response.messages().get(0).content()).isEqualTo("Hello");
            assertThat(response.messages().get(1).type()).isEqualTo("ASSISTANT");
            assertThat(response.messages().get(1).content()).isEqualTo("Hi there!");
        }

        @Test
        void shouldPassOffsetAndLimitToRepository() {
            when(entryRepository.findVisibleEntries("conv-123", 10, 25)).thenReturn(List.of());
            when(entryRepository.countVisibleEntries("conv-123")).thenReturn(0);
            when(memoryUsageProvider.getMemoryUsage("conv-123"))
                    .thenReturn(new ChatMemoryUsage(0, 1000));

            controller.history("conv-123", 10, 25);

            verify(entryRepository).findVisibleEntries("conv-123", 10, 25);
        }

        @Test
        void shouldReturnEmptyListWhenNoMessages() {
            when(entryRepository.findVisibleEntries("conv-123", 0, 50)).thenReturn(List.of());
            when(entryRepository.countVisibleEntries("conv-123")).thenReturn(0);
            when(memoryUsageProvider.getMemoryUsage("conv-123"))
                    .thenReturn(new ChatMemoryUsage(0, 1000));

            var response = controller.history("conv-123", 0, 50);

            assertThat(response.messages()).isEmpty();
            assertThat(response.totalCount()).isZero();
        }
    }

    @Nested
    class HistoryResponseRecord {

        @Test
        void shouldCreateHistoryResponseWithAllFields() {
            var messages = List.of(new ChatController.HistoryMessage("USER", "Hello"));
            var response = new ChatController.HistoryResponse(messages, 10, 500, 1000, 50.0);

            assertThat(response.messages()).isEqualTo(messages);
            assertThat(response.totalCount()).isEqualTo(10);
            assertThat(response.currentTokens()).isEqualTo(500);
            assertThat(response.maxTokens()).isEqualTo(1000);
            assertThat(response.percentageUsed()).isEqualTo(50.0);
        }
    }

    @Nested
    class HistoryMessageRecord {

        @Test
        void shouldCreateHistoryMessageWithTypeAndContent() {
            var message = new ChatController.HistoryMessage("ASSISTANT", "Hello there!");

            assertThat(message.type()).isEqualTo("ASSISTANT");
            assertThat(message.content()).isEqualTo("Hello there!");
        }
    }
}
