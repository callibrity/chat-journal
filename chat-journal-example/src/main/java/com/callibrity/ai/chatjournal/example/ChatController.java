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

import com.callibrity.ai.chatjournal.example.sse.StreamingChatClient;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsage;
import com.callibrity.ai.chatjournal.memory.ChatMemoryUsageProvider;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class ChatController {

    private final StreamingChatClient streamingChatClient;
    private final ChatMemoryUsageProvider memoryUsageProvider;
    private final ChatJournalEntryRepository entryRepository;

    public ChatController(StreamingChatClient streamingChatClient, ChatMemoryUsageProvider memoryUsageProvider,
                          ChatJournalEntryRepository entryRepository) {
        this.streamingChatClient = streamingChatClient;
        this.memoryUsageProvider = memoryUsageProvider;
        this.entryRepository = entryRepository;
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String question,
            @RequestParam(required = false) String conversationId) {

        var builder = streamingChatClient.stream(conversationId);

        return builder
                .onStart(convId -> new Metadata(convId))
                .onChunk(Chunk::new)
                .onComplete(() -> {
                    ChatMemoryUsage usage = memoryUsageProvider.getMemoryUsage(builder.getConversationId());
                    return new Done(usage.currentTokens(), usage.maxTokens(), usage.percentageUsed());
                })
                .execute(question);
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
