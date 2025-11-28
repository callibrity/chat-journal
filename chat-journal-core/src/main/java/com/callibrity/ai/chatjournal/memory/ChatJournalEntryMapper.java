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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Mapper for bidirectional conversion between Spring AI {@link Message} and {@link ChatJournalEntry}.
 *
 * <p>This class handles the serialization and deserialization of messages, including
 * special handling for tool response messages which require JSON serialization.
 *
 * <p>This class is thread-safe.
 */
public class ChatJournalEntryMapper {

    private static final TypeReference<List<ToolResponse>> TOOL_RESPONSE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final TokenUsageCalculator tokenUsageCalculator;

    /**
     * Creates a new ChatJournalEntryMapper.
     *
     * @param objectMapper the Jackson ObjectMapper for serializing/deserializing tool responses
     * @param tokenUsageCalculator the calculator for determining token count
     * @throws NullPointerException if any parameter is null
     */
    public ChatJournalEntryMapper(ObjectMapper objectMapper, TokenUsageCalculator tokenUsageCalculator) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.tokenUsageCalculator = Objects.requireNonNull(tokenUsageCalculator, "tokenUsageCalculator must not be null");
    }

    /**
     * Converts a Spring AI Message to a ChatJournalEntry.
     *
     * <p>The entry is created with message index 0, to be assigned by the repository on save.
     * Tool response messages are serialized to JSON to preserve their structured content.
     *
     * @param message the Spring AI message to convert
     * @return a new ChatJournalEntry representing the message
     * @throws NullPointerException if message is null
     * @throws UncheckedIOException if tool response serialization fails
     */
    public ChatJournalEntry toEntry(Message message) {
        Objects.requireNonNull(message, "message must not be null");
        String content = getContent(message);
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(message));
        return new ChatJournalEntry(0, message.getMessageType().name(), content, tokens);
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
     * Tool response messages are deserialized from their JSON representation.
     *
     * @param entry the entry to convert
     * @return the reconstructed Spring AI Message
     * @throws NullPointerException if entry is null
     * @throws UncheckedIOException if tool response deserialization fails
     */
    public Message toMessage(ChatJournalEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        MessageType type = MessageType.valueOf(entry.messageType());
        return switch (type) {
            case USER -> new UserMessage(entry.content());
            case ASSISTANT -> new AssistantMessage(entry.content());
            case SYSTEM -> new SystemMessage(entry.content());
            case TOOL -> toToolResponseMessage(entry.content());
        };
    }

    /**
     * Converts a list of ChatJournalEntries to Spring AI Messages.
     *
     * @param entries the entries to convert
     * @return a list of messages in the same order
     * @throws NullPointerException if entries is null
     */
    public List<Message> toMessages(List<ChatJournalEntry> entries) {
        Objects.requireNonNull(entries, "entries must not be null");
        return entries.stream()
                .map(this::toMessage)
                .toList();
    }

    private String getContent(Message message) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            try {
                return objectMapper.writeValueAsString(toolResponseMessage.getResponses());
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException("Failed to serialize tool responses", e);
            }
        }
        return message.getText();
    }

    private ToolResponseMessage toToolResponseMessage(String content) {
        try {
            List<ToolResponse> responses = objectMapper.readValue(content, TOOL_RESPONSE_LIST_TYPE);
            return ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to deserialize tool responses", e);
        }
    }
}
