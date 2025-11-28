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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatJournalCheckpointerTest {

    private static final String CONVERSATION_ID = "test-conversation";

    @Mock
    private ChatJournalEntryRepository entryRepository;

    @Mock
    private ChatJournalCheckpointRepository checkpointRepository;

    @Mock
    private ChatJournalCheckpointFactory checkpointFactory;

    @Mock
    private ChatJournalEntryMapper entryMapper;

    private ChatJournalCheckpointer checkpointer;

    @BeforeEach
    void setUp() {
        checkpointer = new ChatJournalCheckpointer(
                entryRepository,
                checkpointRepository,
                checkpointFactory,
                entryMapper,
                1000,  // maxTokens
                2      // minRetainedEntries
        );
    }

    @Nested
    class RequiresCheckpoint {

        @Test
        void shouldReturnTrueWhenTokensExceedMax() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.sumTokens(CONVERSATION_ID)).thenReturn(1500);

            boolean result = checkpointer.requiresCheckpoint(CONVERSATION_ID);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenTokensWithinLimit() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.sumTokens(CONVERSATION_ID)).thenReturn(500);

            boolean result = checkpointer.requiresCheckpoint(CONVERSATION_ID);

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTokensEqualMax() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.sumTokens(CONVERSATION_ID)).thenReturn(1000);

            boolean result = checkpointer.requiresCheckpoint(CONVERSATION_ID);

            assertThat(result).isFalse();
        }

        @Test
        void shouldConsiderExistingCheckpointTokens() {
            ChatJournalCheckpoint existingCheckpoint = new ChatJournalCheckpoint(10, "Previous summary", 400);
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.of(existingCheckpoint));
            when(entryRepository.sumTokensAfterIndex(CONVERSATION_ID, 10)).thenReturn(700);

            boolean result = checkpointer.requiresCheckpoint(CONVERSATION_ID);

            assertThat(result).isTrue();  // 400 + 700 = 1100 > 1000
        }
    }

    @Nested
    class Checkpoint {

        @Test
        void shouldCreateCheckpointWhenNoExistingCheckpoint() {
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, "USER", "Hello", 10),
                    new ChatJournalEntry(2, "ASSISTANT", "Hi!", 10),
                    new ChatJournalEntry(3, "USER", "How are you?", 10),
                    new ChatJournalEntry(4, "ASSISTANT", "I'm good!", 10)
            );
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(entries);
            when(entryMapper.toMessages(any())).thenReturn(List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi!")
            ));
            ChatJournalCheckpoint newCheckpoint = new ChatJournalCheckpoint(2, "Greeting exchange", 20);
            when(checkpointFactory.createCheckpoint(any(), eq(2L))).thenReturn(newCheckpoint);

            checkpointer.checkpoint(CONVERSATION_ID);

            verify(checkpointRepository).saveCheckpoint(CONVERSATION_ID, newCheckpoint);
        }

        @Test
        void shouldIncludeExistingCheckpointSummaryInMessages() {
            ChatJournalCheckpoint existingCheckpoint = new ChatJournalCheckpoint(2, "Previous summary", 50);
            List<ChatJournalEntry> entriesAfterCheckpoint = List.of(
                    new ChatJournalEntry(3, "USER", "Continue", 10),
                    new ChatJournalEntry(4, "ASSISTANT", "Sure!", 10),
                    new ChatJournalEntry(5, "USER", "Thanks", 10),
                    new ChatJournalEntry(6, "ASSISTANT", "You're welcome", 10)
            );

            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.of(existingCheckpoint));
            when(entryRepository.findEntriesAfterIndex(CONVERSATION_ID, 2)).thenReturn(entriesAfterCheckpoint);
            when(entryMapper.toMessages(any())).thenReturn(List.of(
                    new UserMessage("Continue"),
                    new AssistantMessage("Sure!")
            ));
            ChatJournalCheckpoint newCheckpoint = new ChatJournalCheckpoint(4, "Combined summary", 60);
            when(checkpointFactory.createCheckpoint(any(), eq(4L))).thenReturn(newCheckpoint);

            checkpointer.checkpoint(CONVERSATION_ID);

            ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);
            verify(checkpointFactory).createCheckpoint(messagesCaptor.capture(), eq(4L));

            List<Message> capturedMessages = messagesCaptor.getValue();
            assertThat(capturedMessages).hasSize(3);  // 1 system message + 2 messages from mapper
            assertThat(capturedMessages.get(0).getText())
                    .contains("Summary of previous conversation: Previous summary");
        }

        @Test
        void shouldSkipWhenNotEnoughEntries() {
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, "USER", "Hello", 10),
                    new ChatJournalEntry(2, "ASSISTANT", "Hi!", 10)
            );
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(entries);

            checkpointer.checkpoint(CONVERSATION_ID);

            verify(checkpointRepository, never()).saveCheckpoint(anyString(), any());
        }

        @Test
        void shouldSkipWhenEntriesEqualMinRetained() {
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, "USER", "Hello", 10),
                    new ChatJournalEntry(2, "ASSISTANT", "Hi!", 10)
            );
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.findAll(CONVERSATION_ID)).thenReturn(entries);

            checkpointer.checkpoint(CONVERSATION_ID);

            verify(checkpointRepository, never()).saveCheckpoint(anyString(), any());
        }
    }

    @Nested
    class GetTotalTokens {

        @Test
        void shouldReturnSumOfAllTokensWhenNoCheckpoint() {
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.empty());
            when(entryRepository.sumTokens(CONVERSATION_ID)).thenReturn(500);

            int totalTokens = checkpointer.getTotalTokens(CONVERSATION_ID);

            assertThat(totalTokens).isEqualTo(500);
        }

        @Test
        void shouldReturnCheckpointPlusRecentTokensWhenCheckpointExists() {
            ChatJournalCheckpoint checkpoint = new ChatJournalCheckpoint(10, "Summary", 200);
            when(checkpointRepository.findCheckpoint(CONVERSATION_ID)).thenReturn(Optional.of(checkpoint));
            when(entryRepository.sumTokensAfterIndex(CONVERSATION_ID, 10)).thenReturn(300);

            int totalTokens = checkpointer.getTotalTokens(CONVERSATION_ID);

            assertThat(totalTokens).isEqualTo(500);  // 200 + 300
        }
    }

    @Nested
    class MaxTokens {

        @Test
        void shouldReturnConfiguredMaxTokens() {
            assertThat(checkpointer.maxTokens()).isEqualTo(1000);
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullEntryRepository() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            null, checkpointRepository, checkpointFactory, entryMapper, 1000, 2))
                    .withMessage("entryRepository must not be null");
        }

        @Test
        void shouldRejectNullCheckpointRepository() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, null, checkpointFactory, entryMapper, 1000, 2))
                    .withMessage("checkpointRepository must not be null");
        }

        @Test
        void shouldRejectNullCheckpointFactory() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, null, entryMapper, 1000, 2))
                    .withMessage("checkpointFactory must not be null");
        }

        @Test
        void shouldRejectNullEntryMapper() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, checkpointFactory, null, 1000, 2))
                    .withMessage("entryMapper must not be null");
        }

        @Test
        void shouldRejectZeroMaxTokens() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, checkpointFactory, entryMapper, 0, 2))
                    .withMessage("maxTokens must be positive");
        }

        @Test
        void shouldRejectNegativeMaxTokens() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, checkpointFactory, entryMapper, -100, 2))
                    .withMessage("maxTokens must be positive");
        }

        @Test
        void shouldRejectZeroMinRetainedEntries() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, checkpointFactory, entryMapper, 1000, 0))
                    .withMessage("minRetainedEntries must be positive");
        }

        @Test
        void shouldRejectNegativeMinRetainedEntries() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ChatJournalCheckpointer(
                            entryRepository, checkpointRepository, checkpointFactory, entryMapper, 1000, -1))
                    .withMessage("minRetainedEntries must be positive");
        }
    }

    @Nested
    class MethodValidation {

        @Test
        void requiresCheckpointShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> checkpointer.requiresCheckpoint(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void requiresCheckpointShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> checkpointer.requiresCheckpoint(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void checkpointShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> checkpointer.checkpoint(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void checkpointShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> checkpointer.checkpoint(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void getTotalTokensShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> checkpointer.getTotalTokens(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void getTotalTokensShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> checkpointer.getTotalTokens(""))
                    .withMessage("conversationId must not be empty");
        }
    }
}
