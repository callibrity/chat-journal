package com.callibrity.ai.chatjournal.example;

import com.callibrity.ai.chatjournal.example.sse.FluxSseEventStream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static java.util.Optional.ofNullable;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final FluxSseEventStream fluxSseEventStream;

    public ChatController(ChatClient chatClient, FluxSseEventStream fluxSseEventStream) {
        this.chatClient = chatClient;
        this.fluxSseEventStream = fluxSseEventStream;
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId) {

        var actualConversationId = ofNullable(conversationId)
                .orElseGet(() -> UUID.randomUUID().toString());

        var contentFlux = chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, actualConversationId))
                .stream()
                .content();

        return fluxSseEventStream.stream(new Metadata(actualConversationId), contentFlux);
    }

    public record Metadata(String conversationId) {}
}
