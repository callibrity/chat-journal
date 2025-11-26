package com.callibrity.ai.chatjournal.token;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface TokenUsageCalculator {
    int calculateTokenUsage(List<Message> messages);
}
