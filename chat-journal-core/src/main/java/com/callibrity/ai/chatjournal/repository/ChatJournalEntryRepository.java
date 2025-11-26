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
package com.callibrity.ai.chatjournal.repository;

import java.util.List;

/**
 * Repository interface for persisting and retrieving chat journal entries.
 *
 * <p>This interface defines the storage operations required by {@link
 * com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory} for managing conversation
 * history. Implementations handle the actual persistence mechanism (e.g., JDBC, JPA, etc.).
 *
 * <p>Entries are organized by conversation ID, allowing multiple independent conversations
 * to be stored and managed separately.
 *
 * <p>Implementations must be thread-safe as multiple conversations may be processed
 * concurrently.
 *
 * @see ChatJournalEntry
 * @see com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory
 */
public interface ChatJournalEntryRepository {

    /**
     * Saves a list of chat journal entries for a conversation.
     *
     * <p>Entries are appended to the existing conversation history in the order provided.
     *
     * @param conversationId the unique identifier for the conversation
     * @param entries the entries to save; must not be null
     */
    void save(String conversationId, List<ChatJournalEntry> entries);

    /**
     * Retrieves all entries for a conversation in chronological order.
     *
     * @param conversationId the unique identifier for the conversation
     * @return all entries for the conversation, ordered by message index; never null
     */
    List<ChatJournalEntry> findAll(String conversationId);

    /**
     * Retrieves entries eligible for compaction.
     *
     * <p>Returns older entries that can be summarized, excluding the most recent entries
     * that should be preserved. The number of entries to retain is implementation-specific.
     *
     * @param conversationId the unique identifier for the conversation
     * @return entries eligible for compaction, ordered by message index descending; never null
     */
    List<ChatJournalEntry> findEntriesForCompaction(String conversationId);

    /**
     * Gets the total token count for all entries in a conversation.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the sum of tokens across all entries
     */
    int getTotalTokens(String conversationId);

    /**
     * Deletes all entries for a conversation.
     *
     * @param conversationId the unique identifier for the conversation
     */
    void deleteAll(String conversationId);

    /**
     * Replaces compactable entries with a summary entry.
     *
     * <p>This atomic operation removes older entries and inserts a summary entry in their
     * place, preserving recent entries that should not be compacted.
     *
     * @param conversationId the unique identifier for the conversation
     * @param summaryEntry the summary entry to insert; must not be null
     */
    void replaceEntriesWithSummary(String conversationId, ChatJournalEntry summaryEntry);
}
