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
package com.callibrity.ai.chatjournal.memory;

import com.callibrity.ai.chatjournal.logging.StopwatchLogger;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.summary.MessageSummarizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ChatMemory} implementation that persists conversation history with automatic compaction.
 *
 * <p>This implementation stores chat messages in a pluggable repository and automatically
 * compacts older messages into summaries when the conversation exceeds a configured token limit.
 * This enables long-running conversations while staying within LLM context window constraints.
 *
 * <h2>Compaction Strategy</h2>
 * <p>When the total token count exceeds {@code maxTokens}, older messages are summarized
 * and replaced with a single system message containing the summary. Recent messages are
 * preserved to maintain conversation continuity. Compaction runs asynchronously to avoid
 * blocking the main conversation flow.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The repository and summarizer implementations must also
 * be thread-safe as they may be accessed concurrently during compaction.
 *
 * <h2>Concurrency Limitations</h2>
 * <p>In high-concurrency scenarios (especially multi-instance deployments), rapid message
 * additions to the same conversation may trigger multiple concurrent compaction tasks.
 * While data integrity is preserved through repository-level guards, this can result in
 * redundant LLM summarization calls. Applications requiring stricter compaction coordination
 * should implement database-level locking in their {@link ChatJournalEntryRepository}.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ChatJournalChatMemory memory = new ChatJournalChatMemory(
 *     repository,
 *     tokenCalculator,
 *     objectMapper,
 *     summarizer,
 *     4000,  // maxTokens
 *     taskExecutor
 * );
 *
 * // Use with Spring AI ChatClient
 * ChatClient client = ChatClient.builder(chatModel)
 *     .defaultAdvisors(new MessageChatMemoryAdvisor(memory))
 *     .build();
 * }</pre>
 *
 * @see ChatMemory
 * @see ChatJournalEntryRepository
 * @see MessageSummarizer
 */
@Slf4j
public class ChatJournalChatMemory implements ChatMemory {

    private static final int MIN_RECOMMENDED_TOKENS = 500;

    private final ChatJournalEntryRepository repository;
    private final TokenUsageCalculator tokenUsageCalculator;
    private final ObjectMapper objectMapper;
    private final MessageSummarizer summarizer;
    private final int maxTokens;
    private final AsyncTaskExecutor compactionExecutor;

    /**
     * Creates a new ChatJournalChatMemory with the specified components.
     *
     * @param repository the repository for persisting chat entries
     * @param tokenUsageCalculator the calculator for estimating token counts
     * @param objectMapper the Jackson ObjectMapper for message serialization
     * @param summarizer the strategy for generating conversation summaries
     * @param maxTokens the token threshold that triggers compaction; must be positive
     * @param compactionExecutor the executor for running asynchronous compaction tasks
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if maxTokens is not positive
     */
    public ChatJournalChatMemory(ChatJournalEntryRepository repository,
                                 TokenUsageCalculator tokenUsageCalculator,
                                 ObjectMapper objectMapper,
                                 MessageSummarizer summarizer,
                                 int maxTokens,
                                 AsyncTaskExecutor compactionExecutor) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.tokenUsageCalculator = Objects.requireNonNull(tokenUsageCalculator, "tokenUsageCalculator must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.summarizer = Objects.requireNonNull(summarizer, "summarizer must not be null");
        this.compactionExecutor = Objects.requireNonNull(compactionExecutor, "compactionExecutor must not be null");
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        if (maxTokens < MIN_RECOMMENDED_TOKENS) {
            log.warn("maxTokens ({}) is below the recommended minimum of {}; this may cause excessive compaction",
                    maxTokens, MIN_RECOMMENDED_TOKENS);
        }
        this.maxTokens = maxTokens;
    }

    /**
     * {@inheritDoc}
     *
     * <p>After saving the messages, this method checks if the total token count exceeds
     * the configured maximum. If so, an asynchronous compaction task is scheduled.
     *
     * @throws NullPointerException if conversationId or messages is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        validateConversationId(conversationId);
        Objects.requireNonNull(messages, "messages must not be null");

        List<ChatJournalEntry> entries = messages.stream()
                .map(msg -> ChatJournalEntry.fromMessage(msg, objectMapper, tokenUsageCalculator))
                .toList();
        repository.save(conversationId, entries);

        int totalTokens = repository.getTotalTokens(conversationId);
        if (totalTokens > maxTokens) {
            log.info("Scheduling compaction for conversation {}: {} tokens exceeds max of {}",
                    conversationId, totalTokens, maxTokens);
            compactionExecutor.submit(() -> performCompaction(conversationId));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns all messages for the conversation, including any summary messages
     * that replaced older compacted entries.
     *
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public List<Message> get(String conversationId) {
        validateConversationId(conversationId);
        return repository.findAll(conversationId).stream()
                .map(entry -> entry.toMessage(objectMapper))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes all entries for the conversation, including any summary messages.
     *
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public void clear(String conversationId) {
        validateConversationId(conversationId);
        repository.deleteAll(conversationId);
    }

    private static void validateConversationId(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        if (conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId must not be empty");
        }
    }

    private void performCompaction(String conversationId) {

        createSummaryEntry(conversationId).ifPresent(
                summaryEntry -> {
                    var sw = StopwatchLogger.start(log);
                    repository.replaceEntriesWithSummary(conversationId, summaryEntry);
                    sw.info("Compacted conversation {}", conversationId);
                }
        );
    }

    private Optional<ChatJournalEntry> createSummaryEntry(String conversationId) {
        List<ChatJournalEntry> entriesToCompact = repository.findEntriesForCompaction(conversationId);
        if (entriesToCompact.isEmpty()) {
            log.info("Not enough messages to compact for conversation {}", conversationId);
            return Optional.empty();
        }

        List<Message> messages = entriesToCompact.reversed().stream()
                .map(entry -> entry.toMessage(objectMapper))
                .toList();

        var sw = StopwatchLogger.start(log);
        String summaryContent = "Summary of previous conversation: " + summarizer.summarize(messages);
        sw.info("Created summary for conversation {}", conversationId);
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(new SystemMessage(summaryContent)));

        return Optional.of(new ChatJournalEntry(
                entriesToCompact.getFirst().messageIndex(),
                MessageType.SYSTEM.name(),
                summaryContent,
                tokens
        ));
    }
}
