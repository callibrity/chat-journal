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
import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpoint;
import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpointRepository;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles checkpointing (compaction) of chat journal entries.
 *
 * <p>This class determines when a conversation requires checkpointing based on
 * token usage and performs the actual checkpoint creation and storage. It separates
 * the "when" (token threshold) from the "how" (summarization via factory).
 *
 * <p>Checkpointing preserves full conversation history in the entry repository while
 * creating summaries in the checkpoint repository, enabling long-running conversations
 * to stay within LLM context window constraints.
 *
 * <p>This class is thread-safe.
 */
@Slf4j
public class ChatJournalCheckpointer {

    private final ChatJournalEntryRepository entryRepository;
    private final ChatJournalCheckpointRepository checkpointRepository;
    private final ChatJournalCheckpointFactory checkpointFactory;
    private final ChatJournalEntryMapper entryMapper;
    private final int maxTokens;
    private final int minRetainedEntries;

    /**
     * Creates a new ChatJournalCheckpointer.
     *
     * @param entryRepository the repository for chat entries
     * @param checkpointRepository the repository for checkpoints
     * @param checkpointFactory the factory for creating checkpoints
     * @param entryMapper the mapper for converting entries to messages
     * @param maxTokens the token threshold that triggers checkpointing; must be positive
     * @param minRetainedEntries the minimum number of recent entries to retain; must be positive
     * @throws NullPointerException if any object parameter is null
     * @throws IllegalArgumentException if maxTokens or minRetainedEntries is not positive
     */
    public ChatJournalCheckpointer(ChatJournalEntryRepository entryRepository,
                                   ChatJournalCheckpointRepository checkpointRepository,
                                   ChatJournalCheckpointFactory checkpointFactory,
                                   ChatJournalEntryMapper entryMapper,
                                   int maxTokens,
                                   int minRetainedEntries) {
        this.entryRepository = Objects.requireNonNull(entryRepository, "entryRepository must not be null");
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository, "checkpointRepository must not be null");
        this.checkpointFactory = Objects.requireNonNull(checkpointFactory, "checkpointFactory must not be null");
        this.entryMapper = Objects.requireNonNull(entryMapper, "entryMapper must not be null");
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = maxTokens;
        if (minRetainedEntries <= 0) {
            throw new IllegalArgumentException("minRetainedEntries must be positive");
        }
        this.minRetainedEntries = minRetainedEntries;
    }

    /**
     * Determines if a conversation requires checkpointing.
     *
     * <p>A conversation requires checkpointing when its effective token count
     * (checkpoint tokens + tokens for entries after checkpoint) exceeds the
     * configured maximum.
     *
     * @param conversationId the unique identifier for the conversation
     * @return true if checkpointing is required, false otherwise
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    public boolean requiresCheckpoint(String conversationId) {
        validateConversationId(conversationId);
        int totalTokens = getTotalTokens(conversationId);
        return totalTokens > maxTokens;
    }

    /**
     * Performs checkpointing for a conversation.
     *
     * <p>This method:
     * <ol>
     *   <li>Retrieves entries after the current checkpoint (or all entries if none)</li>
     *   <li>Identifies entries to compact (all but the most recent minRetainedEntries)</li>
     *   <li>Creates a summary including any existing checkpoint summary</li>
     *   <li>Saves the new checkpoint</li>
     * </ol>
     *
     * <p>If there are not enough entries to compact, this method returns without
     * creating a checkpoint.
     *
     * @param conversationId the unique identifier for the conversation
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    public void checkpoint(String conversationId) {
        validateConversationId(conversationId);

        Optional<ChatJournalCheckpoint> existingCheckpoint = checkpointRepository.findCheckpoint(conversationId);

        List<ChatJournalEntry> entriesAfterCheckpoint = existingCheckpoint
                .map(cp -> entryRepository.findEntriesAfterIndex(conversationId, cp.checkpointIndex()))
                .orElseGet(() -> entryRepository.findAll(conversationId));

        if (entriesAfterCheckpoint.size() <= minRetainedEntries) {
            log.info("Not enough messages to compact for conversation {}: {} entries, need more than {}",
                    conversationId, entriesAfterCheckpoint.size(), minRetainedEntries);
            return;
        }

        List<ChatJournalEntry> entriesToCompact = entriesAfterCheckpoint.subList(
                0, entriesAfterCheckpoint.size() - minRetainedEntries);

        List<Message> messagesToSummarize = new ArrayList<>();

        existingCheckpoint.ifPresent(cp ->
                messagesToSummarize.add(new SystemMessage(ChatJournalCheckpointFactory.getSummaryPrefix() + cp.summary()))
        );

        messagesToSummarize.addAll(entryMapper.toMessages(entriesToCompact));

        long checkpointIndex = entriesToCompact.getLast().messageIndex();

        var sw = StopwatchLogger.start(log);
        ChatJournalCheckpoint newCheckpoint = checkpointFactory.createCheckpoint(messagesToSummarize, checkpointIndex);
        sw.info("Created checkpoint for conversation {}", conversationId);

        sw = StopwatchLogger.start(log);
        checkpointRepository.saveCheckpoint(conversationId, newCheckpoint);
        sw.info("Saved checkpoint for conversation {}", conversationId);
    }

    /**
     * Gets the effective token count for a conversation.
     *
     * <p>If a checkpoint exists, returns checkpoint tokens plus entry tokens after
     * the checkpoint. Otherwise, returns the sum of all entry tokens.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the effective token count
     */
    public int getTotalTokens(String conversationId) {
        validateConversationId(conversationId);
        return checkpointRepository.findCheckpoint(conversationId)
                .map(cp -> cp.tokens() + entryRepository.sumTokensAfterIndex(conversationId, cp.checkpointIndex()))
                .orElseGet(() -> entryRepository.sumTokens(conversationId));
    }

    /**
     * Returns the configured maximum token threshold.
     *
     * @return the maximum tokens allowed before checkpointing is triggered
     */
    public int maxTokens() {
        return maxTokens;
    }

    private static void validateConversationId(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        if (conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId must not be empty");
        }
    }
}
