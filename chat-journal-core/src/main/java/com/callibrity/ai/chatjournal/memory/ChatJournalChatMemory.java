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

import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpointRepository;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link ChatMemory} implementation that persists conversation history with automatic compaction.
 *
 * <p>This implementation stores chat messages in a pluggable entry repository and uses
 * a checkpointer to manage token budgets via checkpoint-based compaction.
 *
 * <h2>Checkpoint-Based Compaction</h2>
 * <p>When entries are added, the checkpointer is consulted to determine if compaction
 * is needed. If so, compaction runs asynchronously via the provided TaskExecutor.
 * Checkpoints store summaries of older messages, preserving full conversation history
 * while staying within LLM context window constraints.
 *
 * <h2>Message Limits</h2>
 * <p>Conversations are limited to {@code maxConversationLength} messages to prevent unbounded growth.
 * Attempts to add messages that would exceed this limit will throw an exception.
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The repository and checkpointer implementations must
 * also be thread-safe.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ChatJournalChatMemory memory = new ChatJournalChatMemory(
 *     entryRepository,
 *     checkpointRepository,
 *     entryMapper,
 *     checkpointer,
 *     taskExecutor,
 *     10000  // maxConversationLength
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
 * @see ChatJournalCheckpointer
 */
@Slf4j
public class ChatJournalChatMemory implements ChatMemory, ChatMemoryUsageProvider {

    private final ChatJournalEntryRepository entryRepository;
    private final ChatJournalCheckpointRepository checkpointRepository;
    private final ChatJournalEntryMapper entryMapper;
    private final ChatJournalCheckpointer checkpointer;
    private final TaskExecutor taskExecutor;
    private final int maxConversationLength;

    /**
     * Creates a new ChatJournalChatMemory with the specified components.
     *
     * @param entryRepository the repository for persisting chat entries
     * @param checkpointRepository the repository for persisting checkpoints
     * @param entryMapper the mapper for converting between messages and entries
     * @param checkpointer the checkpointer for managing compaction
     * @param taskExecutor the executor for running asynchronous compaction tasks
     * @param maxConversationLength the maximum number of messages allowed per conversation; must be positive
     * @throws NullPointerException if any object parameter is null
     * @throws IllegalArgumentException if maxConversationLength is not positive
     */
    public ChatJournalChatMemory(ChatJournalEntryRepository entryRepository,
                                 ChatJournalCheckpointRepository checkpointRepository,
                                 ChatJournalEntryMapper entryMapper,
                                 ChatJournalCheckpointer checkpointer,
                                 TaskExecutor taskExecutor,
                                 int maxConversationLength) {
        this.entryRepository = Objects.requireNonNull(entryRepository, "entryRepository must not be null");
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository, "checkpointRepository must not be null");
        this.entryMapper = Objects.requireNonNull(entryMapper, "entryMapper must not be null");
        this.checkpointer = Objects.requireNonNull(checkpointer, "checkpointer must not be null");
        this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor must not be null");
        if (maxConversationLength <= 0) {
            throw new IllegalArgumentException("maxConversationLength must be positive");
        }
        this.maxConversationLength = maxConversationLength;
    }

    /**
     * {@inheritDoc}
     *
     * <p>After saving the messages, this method checks if checkpointing is required
     * and schedules asynchronous compaction if needed.
     *
     * @throws NullPointerException if conversationId or messages is null
     * @throws IllegalArgumentException if conversationId is empty
     * @throws ConversationLimitExceededException if adding the messages would exceed the maximum entries limit
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        validateConversationId(conversationId);
        Objects.requireNonNull(messages, "messages must not be null");

        int currentLength = entryRepository.findAll(conversationId).size();
        if (currentLength + messages.size() > maxConversationLength) {
            throw new ConversationLimitExceededException(
                    conversationId, currentLength, maxConversationLength, messages.size());
        }

        List<ChatJournalEntry> entries = entryMapper.toEntries(messages);
        entryRepository.save(conversationId, entries);

        if (checkpointer.requiresCheckpoint(conversationId)) {
            log.info("Scheduling checkpointing for conversation {}", conversationId);
            taskExecutor.execute(() -> checkpointer.checkpoint(conversationId));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns messages suitable for LLM context. If a checkpoint exists, returns
     * the checkpoint summary as a system message followed by entries after the checkpoint.
     * If no checkpoint exists, returns all entries.
     *
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public List<Message> get(String conversationId) {
        validateConversationId(conversationId);

        List<Message> messages = new ArrayList<>();

        checkpointRepository.findCheckpoint(conversationId).ifPresent(checkpoint ->
                messages.add(new SystemMessage(ChatJournalCheckpointFactory.getSummaryPrefix() + checkpoint.summary()))
        );

        List<ChatJournalEntry> entries = checkpointRepository.findCheckpoint(conversationId)
                .map(cp -> entryRepository.findEntriesAfterIndex(conversationId, cp.checkpointIndex()))
                .orElseGet(() -> entryRepository.findAll(conversationId));

        messages.addAll(entryMapper.toMessages(entries));

        return messages;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes all entries and the checkpoint for the conversation.
     *
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public void clear(String conversationId) {
        validateConversationId(conversationId);
        checkpointRepository.deleteCheckpoint(conversationId);
        entryRepository.deleteAll(conversationId);
    }

    /**
     * Returns the current memory usage statistics for a conversation.
     *
     * <p>This provides insight into how much of the configured memory budget is being
     * used by the effective conversation context (checkpoint + recent entries).
     *
     * @param conversationId the unique identifier for the conversation
     * @return the memory usage statistics for the conversation
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    @Override
    public ChatMemoryUsage getMemoryUsage(String conversationId) {
        validateConversationId(conversationId);
        return new ChatMemoryUsage(checkpointer.getTotalTokens(conversationId), checkpointer.maxTokens());
    }

    private static void validateConversationId(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        if (conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId must not be empty");
        }
    }
}
