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

import java.util.Optional;

/**
 * Repository interface for persisting and retrieving chat journal checkpoints.
 *
 * <p>Checkpoints store summaries of older conversation messages, enabling
 * long-running conversations while staying within LLM context window constraints.
 * Each conversation may have at most one checkpoint.
 *
 * <p>This is a ChatMemory-specific concern used for managing token budgets
 * during LLM interactions.
 *
 * <p>Implementations must be thread-safe.
 *
 * @see ChatJournalCheckpoint
 */
public interface ChatJournalCheckpointRepository {

    /**
     * Retrieves the checkpoint for a conversation, if one exists.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the checkpoint, or empty if no checkpoint exists
     */
    Optional<ChatJournalCheckpoint> findCheckpoint(String conversationId);

    /**
     * Saves or updates the checkpoint for a conversation.
     *
     * <p>This performs a delete-then-insert operation to ensure only one
     * checkpoint exists per conversation.
     *
     * @param conversationId the unique identifier for the conversation
     * @param checkpoint the checkpoint to save; must not be null
     */
    void saveCheckpoint(String conversationId, ChatJournalCheckpoint checkpoint);

    /**
     * Deletes the checkpoint for a conversation, if one exists.
     *
     * @param conversationId the unique identifier for the conversation
     */
    void deleteCheckpoint(String conversationId);
}
