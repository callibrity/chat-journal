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

import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpoint;
import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpointRepository;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
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
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatJournalChatMemoryTest {

    private static final String CONVERSATION_ID = "test-conversation";
    private static final int MAX_ENTRIES = 100;

    @Mock
    private ChatJournalEntryRepository entryRepository;

    @Mock
    private ChatJournalCheckpointRepository checkpointRepository;

    @Mock
    private ChatJournalEntryMapper entryMapper;

    @Mock
    private ChatJournalCheckpointer checkpointer;

    @Captor
    private ArgumentCaptor<List<ChatJournalEntry>> entriesCaptor;

    private TaskExecutor taskExecutor;
    private ChatJournalChatMemory chatMemory;

    @BeforeEach
    void setUp() {
        taskExecutor = new SyncTaskExecutor();
        chatMemory = new ChatJournalChatMemory(
                entryRepository,
                checkpointRepository,
                entryMapper,
                checkpointer,
                taskExecutor,
                MAX_ENTRIES
        );
    }

    @Nested
    class Add {

        @Test
        void shouldSaveEntriesToRepository() {
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(List.of());
            when(checkpointer.requiresCheckpoint(CONVERSATION_ID)).thenReturn(false);

            List<Message> messages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi there!")
            );

            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(0, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(0, MessageType.ASSISTANT.name(), "Hi there!", 15)
            );
            when(entryMapper.toEntries(messages)).thenReturn(entries);

            chatMemory.add(CONVERSATION_ID, messages);

            verify(entryRepository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            List<ChatJournalEntry> savedEntries = entriesCaptor.getValue();

            assertThat(savedEntries).hasSize(2);
            assertThat(savedEntries.get(0).messageType()).isEqualTo(MessageType.USER.name());
            assertThat(savedEntries.get(0).content()).isEqualTo("Hello");
            assertThat(savedEntries.get(1).messageType()).isEqualTo(MessageType.ASSISTANT.name());
            assertThat(savedEntries.get(1).content()).isEqualTo("Hi there!");
        }

        @Test
        void shouldTriggerCheckpointingWhenRequired() {
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(List.of());
            when(checkpointer.requiresCheckpoint(CONVERSATION_ID)).thenReturn(true);
            when(entryMapper.toEntries(any())).thenReturn(List.of());

            chatMemory.add(CONVERSATION_ID, List.of(new UserMessage("Hello")));

            verify(checkpointer).checkpoint(CONVERSATION_ID);
        }

        @Test
        void shouldRejectMessagesWhenMaxEntriesExceeded() {
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(
                    List.of(new ChatJournalEntry(1, "USER", "Existing", 10))
            );

            ChatJournalChatMemory smallMemory = new ChatJournalChatMemory(
                    entryRepository,
                    checkpointRepository,
                    entryMapper,
                    checkpointer,
                    taskExecutor,
                    2  // maxEntries = 2
            );

            assertThatIllegalStateException()
                    .isThrownBy(() -> smallMemory.add(CONVERSATION_ID, List.of(
                            new UserMessage("New 1"),
                            new AssistantMessage("New 2")
                    )))
                    .withMessageContaining("Cannot add 2 messages")
                    .withMessageContaining("would exceed maximum of 2");
        }
    }

    @Nested
    class Get {

        @Test
        void shouldRetrieveMessagesWithoutCheckpoint() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(2, MessageType.ASSISTANT.name(), "Hi!", 10)
            );
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(entries);

            List<Message> expectedMessages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi!")
            );
            when(entryMapper.toMessages(entries)).thenReturn(expectedMessages);

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(0).getText()).isEqualTo("Hello");
            assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
            assertThat(messages.get(1).getText()).isEqualTo("Hi!");
        }

        @Test
        void shouldIncludeCheckpointSummaryAsSystemMessage() {
            ChatJournalCheckpoint checkpoint = new ChatJournalCheckpoint(10, "Previous conversation summary", 50);
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.of(checkpoint));

            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(11, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(12, MessageType.ASSISTANT.name(), "Hi!", 10)
            );
            when(entryRepository.findEntriesAfterIndex(CONVERSATION_ID, 10)).thenReturn(entries);

            List<Message> expectedMessages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi!")
            );
            when(entryMapper.toMessages(entries)).thenReturn(expectedMessages);

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).hasSize(3);
            assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
            assertThat(messages.get(0).getText()).contains("Previous conversation summary");
            assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
        }

        @Test
        void shouldReturnEmptyListWhenNoMessages() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(List.of());
            when(entryMapper.toMessages(List.of())).thenReturn(List.of());

            List<Message> messages = chatMemory.get(CONVERSATION_ID);

            assertThat(messages).isEmpty();
        }
    }

    @Nested
    class Clear {

        @Test
        void shouldDeleteCheckpointAndEntries() {
            chatMemory.clear(CONVERSATION_ID);

            verify(checkpointRepository).deleteCheckpoint(CONVERSATION_ID);
            verify(entryRepository).deleteAll(CONVERSATION_ID);
        }
    }

    @Nested
    class ToolResponseMessageSerialization {

        @Test
        void shouldSerializeToolResponseMessageWhenAdding() {
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(List.of());
            when(checkpointer.requiresCheckpoint(CONVERSATION_ID)).thenReturn(false);

            ToolResponseMessage toolMessage = ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponse("tool-1", "tool-name", "Tool result data")
                    ))
                    .build();

            ChatJournalEntry toolEntry = new ChatJournalEntry(0, MessageType.TOOL.name(),
                    "[{\"id\":\"tool-1\",\"name\":\"tool-name\",\"responseData\":\"Tool result data\"}]", 50);
            when(entryMapper.toEntries(List.of(toolMessage))).thenReturn(List.of(toolEntry));

            chatMemory.add(CONVERSATION_ID, List.of(toolMessage));

            verify(entryRepository).save(eq(CONVERSATION_ID), entriesCaptor.capture());
            ChatJournalEntry savedEntry = entriesCaptor.getValue().getFirst();

            assertThat(savedEntry.messageType()).isEqualTo(MessageType.TOOL.name());
            assertThat(savedEntry.content()).contains("tool-1");
        }

        @Test
        void shouldDeserializeToolResponseMessageWhenGetting() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());

            ChatJournalEntry toolEntry = new ChatJournalEntry(1, MessageType.TOOL.name(),
                    "[{\"id\":\"tool-1\",\"name\":\"calculator\",\"responseData\":\"42\"}]", 50);
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(List.of(toolEntry));

            ToolResponseMessage expectedToolMessage = ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponse("tool-1", "calculator", "42")))
                    .build();
            when(entryMapper.toMessages(List.of(toolEntry))).thenReturn(List.of(expectedToolMessage));

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
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullEntryRepository() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            null,
                            checkpointRepository,
                            entryMapper,
                            checkpointer,
                            taskExecutor,
                            MAX_ENTRIES
                    ))
                    .withMessage("entryRepository must not be null");
        }

        @Test
        void shouldRejectNullCheckpointRepository() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            null,
                            entryMapper,
                            checkpointer,
                            taskExecutor,
                            MAX_ENTRIES
                    ))
                    .withMessage("checkpointRepository must not be null");
        }

        @Test
        void shouldRejectNullEntryMapper() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            checkpointRepository,
                            null,
                            checkpointer,
                            taskExecutor,
                            MAX_ENTRIES
                    ))
                    .withMessage("entryMapper must not be null");
        }

        @Test
        void shouldRejectNullCheckpointer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            checkpointRepository,
                            entryMapper,
                            null,
                            taskExecutor,
                            MAX_ENTRIES
                    ))
                    .withMessage("checkpointer must not be null");
        }

        @Test
        void shouldRejectNullTaskExecutor() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            checkpointRepository,
                            entryMapper,
                            checkpointer,
                            null,
                            MAX_ENTRIES
                    ))
                    .withMessage("taskExecutor must not be null");
        }

        @Test
        void shouldRejectZeroMaxEntries() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            checkpointRepository,
                            entryMapper,
                            checkpointer,
                            taskExecutor,
                            0
                    ))
                    .withMessage("maxEntries must be positive");
        }

        @Test
        void shouldRejectNegativeMaxEntries() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalChatMemory(
                            entryRepository,
                            checkpointRepository,
                            entryMapper,
                            checkpointer,
                            taskExecutor,
                            -100
                    ))
                    .withMessage("maxEntries must be positive");
        }
    }

    @Nested
    class MethodParameterValidation {

        @Test
        void addShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> chatMemory.add(null, List.of(new UserMessage("Hello"))))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void addShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> chatMemory.add("", List.of(new UserMessage("Hello"))))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void addShouldRejectNullMessages() {
            assertThatNullPointerException()
                    .isThrownBy(() -> chatMemory.add(CONVERSATION_ID, (List<Message>) null))
                    .withMessage("messages must not be null");
        }

        @Test
        void getShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> chatMemory.get(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void getShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> chatMemory.get(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void clearShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> chatMemory.clear(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void clearShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> chatMemory.clear(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void getMemoryUsageShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> chatMemory.getMemoryUsage(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void getMemoryUsageShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> chatMemory.getMemoryUsage(""))
                    .withMessage("conversationId must not be empty");
        }
    }

    @Nested
    class GetMemoryUsage {

        @Test
        void shouldReturnCurrentTokensFromCheckpointer() {
            when(checkpointer.getTotalTokens(CONVERSATION_ID)).thenReturn(500);
            when(checkpointer.maxTokens()).thenReturn(1000);

            ChatMemoryUsage usage = chatMemory.getMemoryUsage(CONVERSATION_ID);

            assertThat(usage.currentTokens()).isEqualTo(500);
        }

        @Test
        void shouldReturnMaxTokensFromCheckpointer() {
            when(checkpointer.getTotalTokens(CONVERSATION_ID)).thenReturn(500);
            when(checkpointer.maxTokens()).thenReturn(1000);

            ChatMemoryUsage usage = chatMemory.getMemoryUsage(CONVERSATION_ID);

            assertThat(usage.maxTokens()).isEqualTo(1000);
        }
    }
}
