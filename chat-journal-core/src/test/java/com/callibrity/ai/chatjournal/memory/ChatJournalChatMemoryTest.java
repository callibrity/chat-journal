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
package com.callibrity.ai.chatjournal.memory;

import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.summary.MessageSummarizer;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatJournalChatMemoryTest {

    private static final String CONVERSATION_ID = "test-conversation";
    private static final int MAX_TOKENS = 1000;

    @Mock
    private ChatJournalEntryRepository repository;

    @Mock
    private TokenUsageCalculator tokenUsageCalculator;

    @Mock
    private MessageSummarizer summarizer;

    @Mock
    private AsyncTaskExecutor taskExecutor;

    @Captor
    private ArgumentCaptor<List<ChatJournalEntry>> entriesCaptor;

    @Captor
    private ArgumentCaptor<ChatJournalEntry> summaryEntryCaptor;

    private ChatJournalChatMemory chatMemory;

    @BeforeEach
    void setUp() {
        // Configure mock executor to run tasks synchronously for predictable test behavior
        // Lenient because not all tests trigger compaction
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).submit(any(Runnable.class));

        chatMemory = new ChatJournalChatMemory(
                repository,
                tokenUsageCalculator,
                new ObjectMapper(),
                summarizer,
                MAX_TOKENS,
                taskExecutor
        );
    }

    @Nested
    class Add {

        @Test
        void shouldSaveEntriesToRepository() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(500);

            List<Message> messages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi there!")
            );

            chatMemory.add(CONVERSATION_ID, messages);

            verify(repository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            List<ChatJournalEntry> savedEntries = entriesCaptor.getValue();

            assertThat(savedEntries).hasSize(2);
            assertThat(savedEntries.get(0).messageType()).isEqualTo(MessageType.USER.name());
            assertThat(savedEntries.get(0).content()).isEqualTo("Hello");
            assertThat(savedEntries.get(1).messageType()).isEqualTo(MessageType.ASSISTANT.name());
            assertThat(savedEntries.get(1).content()).isEqualTo("Hi there!");
        }

        @Test
        void shouldNotTriggerCompactionWhenTokensBelowMax() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(500);

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Hello")));

            verify(repository, never()).findEntriesForCompaction(anyString());
        }

        @Test
        void shouldTriggerCompactionWhenTokensExceedMax() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(MAX_TOKENS + 1);
            when(repository.findEntriesForCompaction(CONVERSATION_ID)).thenReturn(List.of(
                    new ChatJournalEntry(1, MessageType.USER.name(), "Old message", 50)
            ));
            when(summarizer.summarize(anyList())).thenReturn("Summary of old messages");

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Hello")));

            verify(repository).findEntriesForCompaction(CONVERSATION_ID);
            verify(summarizer).summarize(anyList());
            verify(repository).replaceEntriesWithSummary(eq(CONVERSATION_ID), any(ChatJournalEntry.class));
        }
    }

    @Nested
    class Get {

        @Test
        void shouldRetrieveAndConvertMessages() {
            when(repository.findAll(CONVERSATION_ID)).thenReturn(List.of(
                    new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(2, MessageType.ASSISTANT.name(), "Hi!", 10),
                    new ChatJournalEntry(3, MessageType.SYSTEM.name(), "System prompt", 20)
            ));

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).hasSize(3);
            assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(0).getText()).isEqualTo("Hello");
            assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
            assertThat(messages.get(1).getText()).isEqualTo("Hi!");
            assertThat(messages.get(2)).isInstanceOf(SystemMessage.class);
            assertThat(messages.get(2).getText()).isEqualTo("System prompt");
        }

        @Test
        void shouldReturnEmptyListWhenNoMessages() {
            when(repository.findAll(CONVERSATION_ID)).thenReturn(List.of());

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).isEmpty();
        }
    }

    @Nested
    class Clear {

        @Test
        void shouldDelegateToRepository() {
            chatMemory.clear(CONVERSATION_ID);

            verify(repository).deleteAll(CONVERSATION_ID);
        }
    }

    @Nested
    class Compaction {

        @BeforeEach
        void setUp() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(MAX_TOKENS + 1);
        }

        @Test
        void shouldCreateSummaryEntryWithCorrectMessageIndex() {
            when(repository.findEntriesForCompaction(CONVERSATION_ID)).thenReturn(List.of(
                    new ChatJournalEntry(5, MessageType.USER.name(), "Message 1", 50),
                    new ChatJournalEntry(3, MessageType.ASSISTANT.name(), "Message 2", 50)
            ));
            when(summarizer.summarize(anyList())).thenReturn("Summarized content");

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Trigger")));

            verify(repository).replaceEntriesWithSummary(eq(CONVERSATION_ID), summaryEntryCaptor.capture());
            ChatJournalEntry summaryEntry = summaryEntryCaptor.getValue();

            assertThat(summaryEntry.messageIndex()).isEqualTo(5);
            assertThat(summaryEntry.messageType()).isEqualTo(MessageType.SYSTEM.name());
            assertThat(summaryEntry.content()).startsWith("Summary of previous conversation:");
        }

        @Test
        void shouldNotCompactWhenNoEntriesAvailable() {
            when(repository.findEntriesForCompaction(CONVERSATION_ID)).thenReturn(List.of());

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Trigger")));

            verify(repository, never()).replaceEntriesWithSummary(anyString(), any());
            verify(summarizer, never()).summarize(anyList());
        }

        @Test
        void shouldPassMessagesInCorrectOrderToSummarizer() {
            when(repository.findEntriesForCompaction(CONVERSATION_ID)).thenReturn(List.of(
                    new ChatJournalEntry(3, MessageType.ASSISTANT.name(), "Response", 50),
                    new ChatJournalEntry(2, MessageType.USER.name(), "Question", 50)
            ));

            ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.captor();
            when(summarizer.summarize(messagesCaptor.capture())).thenReturn("Summary");

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Trigger")));

            List<Message> passedMessages = messagesCaptor.getValue();
            assertThat(passedMessages).hasSize(2);
            // Should be reversed (chronological order)
            assertThat(passedMessages.get(0).getText()).isEqualTo("Question");
            assertThat(passedMessages.get(1).getText()).isEqualTo("Response");
        }
    }

    @Nested
    class ToolResponseMessageSerialization {

        @Test
        void shouldSerializeToolResponseMessageWhenAdding() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(500);

            ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponse("tool-1", "tool-name", "Tool result data")
                    ))
                    .build();

            chatMemory.add(CONVERSATION_ID, List.of(toolMessage));

            verify(repository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            ChatJournalEntry savedEntry = entriesCaptor.getValue().getFirst();

            assertThat(savedEntry.messageType()).isEqualTo(MessageType.TOOL.name());
            assertThat(savedEntry.content()).contains("tool-1");
            assertThat(savedEntry.content()).contains("tool-name");
            assertThat(savedEntry.content()).contains("Tool result data");
        }

        @Test
        void shouldDeserializeToolResponseMessageWhenGetting() {
            String serializedToolResponse = """
                    [{"id":"tool-1","name":"calculator","responseData":"42"}]
                    """;

            when(repository.findAll(CONVERSATION_ID)).thenReturn(List.of(
                    new ChatJournalEntry(1, MessageType.TOOL.name(), serializedToolResponse, 50)
            ));

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).hasSize(1);
            assertThat(messages.getFirst()).isInstanceOf(ToolResponseMessage.class);

            ToolResponseMessage toolMessage = (ToolResponseMessage) messages.getFirst();
            List<ToolResponse> responses = toolMessage.getResponses();

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().id()).isEqualTo("tool-1");
            assertThat(responses.getFirst().name()).isEqualTo("calculator");
            assertThat(responses.getFirst().responseData()).isEqualTo("42");
        }

        @Test
        void shouldHandleMultipleToolResponses() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(500);

            ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponse("tool-1", "search", "Result 1"),
                            new ToolResponse("tool-2", "calculator", "42")
                    ))
                    .build();

            chatMemory.add(CONVERSATION_ID, List.of(toolMessage));

            verify(repository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            String content = entriesCaptor.getValue().getFirst().content();

            assertThat(content).contains("tool-1");
            assertThat(content).contains("tool-2");
            assertThat(content).contains("search");
            assertThat(content).contains("calculator");
        }

        @Test
        void shouldRoundTripToolResponseMessage() {
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
            when(repository.getTotalTokens(CONVERSATION_ID)).thenReturn(500);

            ToolResponseMessage originalMessage = ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponse("abc-123", "weather", "Sunny, 72°F")
                    ))
                    .build();

            // Add the message
            chatMemory.add(CONVERSATION_ID, List.of(originalMessage));

            // Capture what was saved
            verify(repository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            ChatJournalEntry savedEntry = entriesCaptor.getValue().getFirst();

            // Mock repository to return the saved entry
            when(repository.findAll(CONVERSATION_ID)).thenReturn(List.of(savedEntry));

            // Get it back
            List<Message> retrievedMessages = chatMemory.get(CONVERSATION_ID);

            assertThat(retrievedMessages).hasSize(1);
            ToolResponseMessage retrievedMessage = (ToolResponseMessage) retrievedMessages.getFirst();
            ToolResponse response = retrievedMessage.getResponses().getFirst();

            assertThat(response.id()).isEqualTo("abc-123");
            assertThat(response.name()).isEqualTo("weather");
            assertThat(response.responseData()).isEqualTo("Sunny, 72°F");
        }
    }
}
