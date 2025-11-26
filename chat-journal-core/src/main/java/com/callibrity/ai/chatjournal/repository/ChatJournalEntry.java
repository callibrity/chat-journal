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
package com.callibrity.ai.chatjournal.repository;

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

/**
 * Immutable record representing a persisted chat message entry.
 *
 * <p>This record serves as the data transfer object between the chat memory system and the
 * underlying storage. It captures all information needed to reconstruct a Spring AI
 * {@link Message} while also tracking metadata for memory management.
 *
 * <p>Entries are serializable to various storage backends (JDBC, JPA, etc.) and support
 * all Spring AI message types including tool response messages.
 *
 * @param messageIndex the ordinal position of this message within the conversation;
 *                     used for ordering and identifying entries during compaction
 * @param messageType the type of message (USER, ASSISTANT, SYSTEM, or TOOL);
 *                    corresponds to Spring AI's {@link MessageType}
 * @param content the message content; for tool response messages, this is JSON-serialized
 * @param tokens the estimated or calculated token count for this message
 * @see ChatJournalEntryRepository
 * @see com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory
 */
public record ChatJournalEntry(long messageIndex, String messageType, String content, int tokens) {

    private static final TypeReference<List<ToolResponse>> TOOL_RESPONSE_LIST_TYPE = new TypeReference<>() {};

    /**
     * Creates a ChatJournalEntry from a Spring AI Message.
     *
     * <p>This factory method extracts the message type and content, calculates token usage,
     * and creates a new entry with message index 0 (to be assigned by the repository).
     *
     * <p>Tool response messages are serialized to JSON to preserve their structured content.
     *
     * @param message the Spring AI message to convert
     * @param objectMapper the Jackson ObjectMapper for serializing tool responses
     * @param tokenUsageCalculator the calculator for determining token count
     * @return a new ChatJournalEntry representing the message
     * @throws UncheckedIOException if tool response serialization fails
     */
    public static ChatJournalEntry fromMessage(Message message, ObjectMapper objectMapper, TokenUsageCalculator tokenUsageCalculator) {
        String content = getContent(message, objectMapper);
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(message));
        return new ChatJournalEntry(0, message.getMessageType().name(), content, tokens);
    }

    private static String getContent(Message message, ObjectMapper objectMapper) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            try {
                return objectMapper.writeValueAsString(toolResponseMessage.getResponses());
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException("Failed to serialize tool responses", e);
            }
        }
        return message.getText();
    }

    /**
     * Converts this entry back to a Spring AI Message.
     *
     * <p>Reconstructs the appropriate Message subtype based on the stored message type.
     * Tool response messages are deserialized from their JSON representation.
     *
     * @param objectMapper the Jackson ObjectMapper for deserializing tool responses
     * @return the reconstructed Spring AI Message
     * @throws UncheckedIOException if tool response deserialization fails
     */
    public Message toMessage(ObjectMapper objectMapper) {
        MessageType type = MessageType.valueOf(messageType);
        return switch (type) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> toToolResponseMessage(objectMapper);
        };
    }

    private ToolResponseMessage toToolResponseMessage(ObjectMapper objectMapper) {
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
