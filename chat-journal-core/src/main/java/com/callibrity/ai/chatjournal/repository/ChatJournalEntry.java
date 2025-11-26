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

public record ChatJournalEntry(long messageIndex, String messageType, String content, int tokens) {

    private static final TypeReference<List<ToolResponse>> TOOL_RESPONSE_LIST_TYPE = new TypeReference<>() {};

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
