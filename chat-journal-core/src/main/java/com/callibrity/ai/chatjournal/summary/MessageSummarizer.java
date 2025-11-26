package com.callibrity.ai.chatjournal.summary;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

@FunctionalInterface
public interface MessageSummarizer {

    String summarize(List<Message> messages);
}
