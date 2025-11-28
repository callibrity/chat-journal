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
 * <p>This interface defines the storage operations for chat messages. It is a
 * general-purpose persistence layer that can be used by any component needing
 * access to conversation history (UI, APIs, ChatMemory, etc.).
 *
 * <p>Entries are organized by conversation ID, allowing multiple independent
 * conversations to be stored and managed separately.
 *
 * <p>Implementations must be thread-safe as multiple conversations may be
 * processed concurrently.
 *
 * @see ChatJournalEntry
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
     * Retrieves visible entries (USER and ASSISTANT only) for UI display with pagination.
     *
     * <p>Returns entries in reverse chronological order (most recent first), suitable
     * for a chat UI that loads from the bottom and paginates upward. SYSTEM messages
     * are excluded as they are implementation details not meant for display.
     *
     * @param conversationId the unique identifier for the conversation
     * @param offset the number of entries to skip (0 = start from most recent)
     * @param limit the maximum number of entries to return
     * @return visible entries in reverse chronological order; never null
     */
    List<ChatJournalEntry> findVisibleEntries(String conversationId, int offset, int limit);

    /**
     * Counts the total number of visible entries (USER and ASSISTANT only) for a conversation.
     *
     * <p>This is useful for pagination calculations in the UI.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the count of visible entries
     */
    int countVisibleEntries(String conversationId);

    /**
     * Retrieves entries after a specific message index.
     *
     * <p>This is useful for retrieving entries that come after a checkpoint.
     *
     * @param conversationId the unique identifier for the conversation
     * @param messageIndex the index after which to retrieve entries
     * @return entries with index greater than messageIndex, ordered by message index; never null
     */
    List<ChatJournalEntry> findEntriesAfterIndex(String conversationId, long messageIndex);

    /**
     * Calculates the sum of tokens for all entries in a conversation.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the total token count for all entries
     */
    int sumTokens(String conversationId);

    /**
     * Calculates the sum of tokens for entries after a specific message index.
     *
     * @param conversationId the unique identifier for the conversation
     * @param messageIndex the index after which to sum tokens
     * @return the total token count for entries after the index
     */
    int sumTokensAfterIndex(String conversationId, long messageIndex);

    /**
     * Deletes all entries for a conversation.
     *
     * @param conversationId the unique identifier for the conversation
     */
    void deleteAll(String conversationId);
}
