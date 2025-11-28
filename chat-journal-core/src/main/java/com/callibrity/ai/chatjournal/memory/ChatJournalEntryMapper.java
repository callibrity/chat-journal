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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Objects;

/**
 * Mapper for bidirectional conversion between Spring AI {@link Message} and {@link ChatJournalEntry}.
 *
 * <p>This class handles the conversion of USER and ASSISTANT messages.
 *
 * <p>This class is thread-safe.
 */
@Slf4j
public class ChatJournalEntryMapper {

    private final TokenUsageCalculator tokenUsageCalculator;

    /**
     * Creates a new ChatJournalEntryMapper.
     *
     * @param tokenUsageCalculator the calculator for determining token count
     * @throws NullPointerException if tokenUsageCalculator is null
     */
    public ChatJournalEntryMapper(TokenUsageCalculator tokenUsageCalculator) {
        this.tokenUsageCalculator = Objects.requireNonNull(tokenUsageCalculator, "tokenUsageCalculator must not be null");
    }

    /**
     * Converts a Spring AI Message to a ChatJournalEntry.
     *
     * <p>The entry is created with message index 0, to be assigned by the repository on save.
     *
     * @param message the Spring AI message to convert
     * @return a new ChatJournalEntry representing the message
     * @throws NullPointerException if message is null
     */
    public ChatJournalEntry toEntry(Message message) {
        Objects.requireNonNull(message, "message must not be null");
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(message));
        return new ChatJournalEntry(0, message.getMessageType().name(), message.getText(), tokens);
    }

    /**
     * Converts a list of Spring AI Messages to ChatJournalEntries.
     *
     * @param messages the messages to convert
     * @return a list of entries in the same order
     * @throws NullPointerException if messages is null
     */
    public List<ChatJournalEntry> toEntries(List<Message> messages) {
        Objects.requireNonNull(messages, "messages must not be null");
        return messages.stream()
                .map(this::toEntry)
                .toList();
    }

    /**
     * Converts a ChatJournalEntry back to a Spring AI Message.
     *
     * <p>Reconstructs the appropriate Message subtype based on the stored message type.
     * Unsupported message types are logged and converted to UserMessage as a fallback.
     *
     * @param entry the entry to convert
     * @return the reconstructed Spring AI Message, or null if the message type is unsupported
     * @throws NullPointerException if entry is null
     */
    public Message toMessage(ChatJournalEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        MessageType type = MessageType.valueOf(entry.messageType());
        return switch (type) {
            case USER -> new UserMessage(entry.content());
            case ASSISTANT -> new AssistantMessage(entry.content());
            default -> {
                log.warn("Unsupported message type: {}", type);
                yield null;
            }
        };
    }

    /**
     * Converts a list of ChatJournalEntries to Spring AI Messages.
     *
     * <p>Entries with unsupported message types are filtered out.
     *
     * @param entries the entries to convert
     * @return a list of messages in the same order, excluding unsupported types
     * @throws NullPointerException if entries is null
     */
    public List<Message> toMessages(List<ChatJournalEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        return entries.stream()
                .map(this::toMessage)
                .filter(Objects::nonNull)
                .toList();
    }
}
