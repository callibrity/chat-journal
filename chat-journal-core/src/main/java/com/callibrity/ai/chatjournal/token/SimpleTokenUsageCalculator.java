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

import static java.util.Optional.ofNullable;

/**
 * A simple token usage calculator that estimates tokens based on character count.
 *
 * <p>This implementation provides a fast, approximate token count by dividing the total
 * character count by a configurable characters-per-token ratio. While not as accurate as
 * tokenizer-based implementations (like JTokkit), it provides reasonable estimates for
 * most use cases and has no external dependencies.
 *
 * <p>The default ratio of 4 characters per token is a reasonable approximation for English
 * text with OpenAI models, but may need adjustment for other languages or models.
 *
 * <p>This class is thread-safe.
 *
 * @see TokenUsageCalculator
 */
public class SimpleTokenUsageCalculator implements TokenUsageCalculator {

    /**
     * The number of characters that approximates one token.
     */
    private final int charactersPerToken;

    /**
     * Creates a new SimpleTokenUsageCalculator with the specified characters-per-token ratio.
     *
     * @param charactersPerToken the number of characters per token; must be positive
     * @throws IllegalArgumentException if charactersPerToken is not positive
     */
    public SimpleTokenUsageCalculator(int charactersPerToken) {
        if (charactersPerToken <= 0) {
            throw new IllegalArgumentException("charactersPerToken must be positive");
        }
        this.charactersPerToken = charactersPerToken;
    }

    /**
     * Calculates the estimated token usage for a single message.
     *
     * @param message the message to calculate tokens for
     * @return the estimated token count based on character length divided by charactersPerToken
     */
    public int calculateTokenUsage(Message message) {
        return ofNullable(message.getText())
                .map(String::length)
                .orElse(0) / charactersPerToken;
    }

    @Override
    public int calculateTokenUsage(List<Message> messages) {
        return messages.stream()
                .mapToInt(this::calculateTokenUsage)
                .sum();
    }
}
