package com.callibrity.ai.chatjournal.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ChatClientMessageSummarizer implements MessageSummarizer {

    private final ChatClient chatClient;

    @Override
    public String summarize(List<Message> messages) {
        log.info("Summarizing {} messages", messages.size());

        return chatClient.prompt()
                .messages(messages)
                .user("Please provide a concise summary of the conversation above. Capture the key points, decisions, and any important context needed to continue.")
                .call()
                .content();
    }
}
