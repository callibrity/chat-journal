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
package com.callibrity.ai.chatjournal.token;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Strategy interface for calculating token usage of chat messages.
 *
 * <p>Token counting is essential for managing conversation memory within LLM context limits.
 * Different implementations may use various strategies, from simple character-based estimation
 * to accurate tokenizer-based counting (e.g., using JTokkit for OpenAI models).
 *
 * <p>Implementations should be thread-safe as they may be called concurrently from multiple
 * conversations.
 *
 * @see com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory
 */
public interface TokenUsageCalculator {

    /**
     * Calculates the total token usage for a list of messages.
     *
     * @param messages the list of messages to calculate tokens for; must not be null
     * @return the total estimated or actual token count for all messages
     */
    int calculateTokenUsage(List<Message> messages);
}
