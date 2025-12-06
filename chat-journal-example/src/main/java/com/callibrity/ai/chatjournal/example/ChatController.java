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
package com.callibrity.ai.chatjournal.example;

import com.callibrity.ai.chatjournal.example.sse.ChatCompletionEmitter;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsage;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsageProvider;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final TaskExecutor executor;
    private final ChatMemoryUsageProvider memoryUsageProvider;
    private final ChatJournalEntryRepository entryRepository;

    public ChatController(ChatClient chatClient, TaskExecutor executor, ChatMemoryUsageProvider memoryUsageProvider,
                          ChatJournalEntryRepository entryRepository) {
        this.chatClient = chatClient;
        this.executor = executor;
        this.memoryUsageProvider = memoryUsageProvider;
        this.entryRepository = entryRepository;
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId) {

        var actualConversationId = ofNullable(conversationId)
                .orElseGet(() -> UUID.randomUUID().toString());

        var flux = chatClient.prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, actualConversationId))
                .stream()
                .content();
        var emitter = new ChatCompletionEmitter(conversationId);

        executor.execute(() -> {
            emitter.send("metadata", new Metadata(actualConversationId));
            for (var item : flux.toIterable()) {
                emitter.send("chunk", new Chunk(item));
            }
            ChatMemoryUsage usage = memoryUsageProvider.getMemoryUsage(actualConversationId);
            emitter.complete("done", new Done(usage.currentTokens(), usage.maxTokens(), usage.percentageUsed()));
        });

        return emitter.getEmitter();
    }

    @GetMapping("/chat/history")
    public HistoryResponse history(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {

        List<ChatJournalEntry> entries = entryRepository.findVisibleEntries(conversationId, offset, limit);
        int totalCount = entryRepository.countVisibleEntries(conversationId);
        ChatMemoryUsage usage = memoryUsageProvider.getMemoryUsage(conversationId);

        List<HistoryMessage> messages = entries.stream()
                .map(e -> new HistoryMessage(e.messageType(), e.content()))
                .toList();

        return new HistoryResponse(
                messages,
                totalCount,
                usage.currentTokens(),
                usage.maxTokens(),
                usage.percentageUsed()
        );
    }

    public record HistoryResponse(
            List<HistoryMessage> messages,
            int totalCount,
            int currentTokens,
            int maxTokens,
            double percentageUsed
    ) {}

    public record HistoryMessage(String type, String content) {}

    public record Chunk(String content) {

    }

    public record Metadata(String conversationId) {
    }

    public record Done(int currentTokens, int maxTokens, double percentageUsed) {
    }
}
