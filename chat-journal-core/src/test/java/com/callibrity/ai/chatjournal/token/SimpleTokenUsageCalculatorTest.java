package com.callibrity.ai.chatjournal.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleTokenUsageCalculatorTest {

    private static final int CHARACTERS_PER_TOKEN = 4;

    private SimpleTokenUsageCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SimpleTokenUsageCalculator(CHARACTERS_PER_TOKEN);
    }

    @Test
    void shouldCalculateTokensBasedOnCharacterCount() {
        // 12 characters / 4 = 3 tokens
        UserMessage message = new UserMessage("Hello World!");

        int tokens = calculator.calculateTokenUsage(List.of(message));

        assertThat(tokens).isEqualTo(3);
    }

    @Test
    void shouldTruncatePartialTokens() {
        // 5 characters / 4 = 1 token (integer division)
        UserMessage message = new UserMessage("Hello");

        int tokens = calculator.calculateTokenUsage(List.of(message));

        assertThat(tokens).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroForEmptyMessage() {
        UserMessage message = new UserMessage("");

        int tokens = calculator.calculateTokenUsage(List.of(message));

        assertThat(tokens).isZero();
    }

    @Test
    void shouldReturnZeroForNullText() {
        AssistantMessage message = new AssistantMessage(null);

        int tokens = calculator.calculateTokenUsage(List.of(message));

        assertThat(tokens).isZero();
    }

    @Test
    void shouldSumTokensForMultipleMessages() {
        // 8 chars / 4 = 2 tokens + 8 chars / 4 = 2 tokens = 4 total
        List<Message> messages = List.of(
                new UserMessage("Message1"),
                new UserMessage("Message2")
        );

        int tokens = calculator.calculateTokenUsage(messages);

        assertThat(tokens).isEqualTo(4);
    }

    @Test
    void shouldReturnZeroForEmptyList() {
        int tokens = calculator.calculateTokenUsage(List.of());

        assertThat(tokens).isZero();
    }

    @Test
    void shouldReturnZeroWhenTextShorterThanCharactersPerToken() {
        // 3 characters / 4 = 0 tokens
        UserMessage message = new UserMessage("Hi!");

        int tokens = calculator.calculateTokenUsage(List.of(message));

        assertThat(tokens).isZero();
    }
}
