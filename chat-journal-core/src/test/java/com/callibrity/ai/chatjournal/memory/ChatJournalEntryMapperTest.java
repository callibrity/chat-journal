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
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatJournalEntryMapperTest {

    @Mock
    private TokenUsageCalculator tokenUsageCalculator;

    private ChatJournalEntryMapper mapper;

    @BeforeEach
    void setUp() {
        lenient().when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(100);
        mapper = new ChatJournalEntryMapper(tokenUsageCalculator);
    }

    @Nested
    class ToEntry {

        @Test
        void shouldCreateEntryFromUserMessage() {
            UserMessage message = new UserMessage("Hello");

            ChatJournalEntry entry = mapper.toEntry(message);

            assertThat(entry.messageIndex()).isZero();
            assertThat(entry.messageType()).isEqualTo(MessageType.USER.name());
            assertThat(entry.content()).isEqualTo("Hello");
            assertThat(entry.tokens()).isEqualTo(100);
        }

        @Test
        void shouldCreateEntryFromAssistantMessage() {
            AssistantMessage message = new AssistantMessage("Hi there!");

            ChatJournalEntry entry = mapper.toEntry(message);

            assertThat(entry.messageType()).isEqualTo(MessageType.ASSISTANT.name());
            assertThat(entry.content()).isEqualTo("Hi there!");
        }

        @Test
        void shouldRejectNullMessage() {
            assertThatNullPointerException()
                    .isThrownBy(() -> mapper.toEntry(null))
                    .withMessage("message must not be null");
        }
    }

    @Nested
    class ToEntries {

        @Test
        void shouldConvertMultipleMessages() {
            List<Message> messages = List.of(
                    new UserMessage("Hello"),
                    new AssistantMessage("Hi there!")
            );

            List<ChatJournalEntry> entries = mapper.toEntries(messages);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).messageType()).isEqualTo(MessageType.USER.name());
            assertThat(entries.get(1).messageType()).isEqualTo(MessageType.ASSISTANT.name());
        }

        @Test
        void shouldRejectNullMessages() {
            assertThatNullPointerException()
                    .isThrownBy(() -> mapper.toEntries(null))
                    .withMessage("messages must not be null");
        }
    }

    @Nested
    class ToMessage {

        @Test
        void shouldConvertUserEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10);

            Message message = mapper.toMessage(entry);

            assertThat(message).isInstanceOf(UserMessage.class);
            assertThat(message.getText()).isEqualTo("Hello");
        }

        @Test
        void shouldConvertAssistantEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(2, MessageType.ASSISTANT.name(), "Hi there!", 15);

            Message message = mapper.toMessage(entry);

            assertThat(message).isInstanceOf(AssistantMessage.class);
            assertThat(message.getText()).isEqualTo("Hi there!");
        }

        @Test
        void shouldReturnNullForSystemMessageType() {
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.SYSTEM.name(), "some content", 10);

            Message message = mapper.toMessage(entry);

            assertThat(message).isNull();
        }

        @Test
        void shouldReturnNullForToolMessageType() {
            ChatJournalEntry entry = new ChatJournalEntry(1, MessageType.TOOL.name(), "some content", 10);

            Message message = mapper.toMessage(entry);

            assertThat(message).isNull();
        }

        @Test
        void shouldRejectNullEntry() {
            assertThatNullPointerException()
                    .isThrownBy(() -> mapper.toMessage(null))
                    .withMessage("entry must not be null");
        }
    }

    @Nested
    class ToMessages {

        @Test
        void shouldConvertMultipleEntries() {
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(2, MessageType.ASSISTANT.name(), "Hi!", 10)
            );

            List<Message> messages = mapper.toMessages(entries);

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
        }

        @Test
        void shouldFilterOutUnsupportedMessageTypes() {
            List<ChatJournalEntry> entries = List.of(
                    new ChatJournalEntry(1, MessageType.USER.name(), "Hello", 10),
                    new ChatJournalEntry(2, MessageType.TOOL.name(), "tool content", 10),
                    new ChatJournalEntry(3, MessageType.ASSISTANT.name(), "Hi!", 10)
            );

            List<Message> messages = mapper.toMessages(entries);

            assertThat(messages).hasSize(2);
            assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
            assertThat(messages.get(1)).isInstanceOf(AssistantMessage.class);
        }

        @Test
        void shouldRejectNullEntries() {
            assertThatNullPointerException()
                    .isThrownBy(() -> mapper.toMessages(null))
                    .withMessage("entries must not be null");
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullTokenUsageCalculator() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalEntryMapper(null))
                    .withMessage("tokenUsageCalculator must not be null");
        }
    }
}
