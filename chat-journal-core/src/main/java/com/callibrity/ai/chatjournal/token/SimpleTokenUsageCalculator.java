package com.callibrity.ai.chatjournal.token;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class SimpleTokenUsageCalculator implements TokenUsageCalculator {

    private final int charactersPerToken;

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
