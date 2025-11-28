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
package com.callibrity.ai.chatjournal.jdbc;

import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpoint;
import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpointRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-based implementation of {@link ChatJournalCheckpointRepository}.
 *
 * <p>This implementation stores chat journal checkpoints in a relational database
 * using Spring's {@link JdbcTemplate}. It requires a table named {@code chat_journal_checkpoint}.
 *
 * <p>This class is thread-safe as it delegates all operations to the thread-safe JdbcTemplate.
 *
 * @see ChatJournalCheckpointRepository
 */
public class JdbcChatJournalCheckpointRepository implements ChatJournalCheckpointRepository {

    private static final String COL_CHECKPOINT_INDEX = "checkpoint_index";
    private static final String COL_SUMMARY = "summary";
    private static final String COL_TOKENS = "tokens";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a new JdbcChatJournalCheckpointRepository.
     *
     * @param jdbcTemplate the JdbcTemplate for database operations
     * @throws NullPointerException if jdbcTemplate is null
     */
    public JdbcChatJournalCheckpointRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public Optional<ChatJournalCheckpoint> findCheckpoint(String conversationId) {
        validateConversationId(conversationId);
        List<ChatJournalCheckpoint> checkpoints = jdbcTemplate.query(
                "SELECT checkpoint_index, summary, tokens FROM chat_journal_checkpoint WHERE conversation_id = ?",
                this::mapRow,
                conversationId
        );
        return checkpoints.isEmpty() ? Optional.empty() : Optional.of(checkpoints.getFirst());
    }

    @Override
    @Transactional
    public void saveCheckpoint(String conversationId, ChatJournalCheckpoint checkpoint) {
        validateConversationId(conversationId);
        Objects.requireNonNull(checkpoint, "checkpoint must not be null");
        jdbcTemplate.update("DELETE FROM chat_journal_checkpoint WHERE conversation_id = ?", conversationId);
        jdbcTemplate.update(
                "INSERT INTO chat_journal_checkpoint (conversation_id, checkpoint_index, summary, tokens) VALUES (?, ?, ?, ?)",
                conversationId,
                checkpoint.checkpointIndex(),
                checkpoint.summary(),
                checkpoint.tokens()
        );
    }

    @Override
    public void deleteCheckpoint(String conversationId) {
        validateConversationId(conversationId);
        jdbcTemplate.update("DELETE FROM chat_journal_checkpoint WHERE conversation_id = ?", conversationId);
    }

    private static void validateConversationId(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        if (conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId must not be empty");
        }
    }

    private ChatJournalCheckpoint mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ChatJournalCheckpoint(
                rs.getLong(COL_CHECKPOINT_INDEX),
                rs.getString(COL_SUMMARY),
                rs.getInt(COL_TOKENS)
        );
    }
}
