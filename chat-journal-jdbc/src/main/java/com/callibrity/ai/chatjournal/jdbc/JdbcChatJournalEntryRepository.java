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

import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Objects;

/**
 * JDBC-based implementation of {@link ChatJournalEntryRepository}.
 *
 * <p>This implementation stores chat journal entries in a relational database
 * using Spring's {@link JdbcTemplate}. It requires a table named {@code chat_journal}.
 *
 * <p>This class is thread-safe as it delegates all operations to the thread-safe JdbcTemplate.
 *
 * @see ChatJournalEntryRepository
 */
public class JdbcChatJournalEntryRepository implements ChatJournalEntryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a new JdbcChatJournalEntryRepository.
     *
     * @param jdbcTemplate the JdbcTemplate for database operations
     * @throws NullPointerException if jdbcTemplate is null
     */
    public JdbcChatJournalEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public void save(String conversationId, List<ChatJournalEntry> entries) {
        validateConversationId(conversationId);
        Objects.requireNonNull(entries, "entries must not be null");
        jdbcTemplate.batchUpdate(
                "INSERT INTO chat_journal (conversation_id, message_type, content, tokens) VALUES (?, ?, ?, ?)",
                entries,
                entries.size(),
                (ps, entry) -> {
                    ps.setString(1, conversationId);
                    ps.setString(2, entry.messageType());
                    ps.setString(3, entry.content());
                    ps.setInt(4, entry.tokens());
                }
        );
    }

    @Override
    public List<ChatJournalEntry> findAll(String conversationId) {
        validateConversationId(conversationId);
        return jdbcTemplate.query(
                "SELECT message_index, message_type, content, tokens FROM chat_journal WHERE conversation_id = ? ORDER BY message_index",
                (rs, rowNum) -> new ChatJournalEntry(
                        rs.getLong("message_index"),
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getInt("tokens")
                ),
                conversationId
        );
    }

    @Override
    public List<ChatJournalEntry> findVisibleEntries(String conversationId, int offset, int limit) {
        validateConversationId(conversationId);
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return jdbcTemplate.query(
                "SELECT message_index, message_type, content, tokens FROM chat_journal WHERE conversation_id = ? AND message_type IN ('USER', 'ASSISTANT') ORDER BY message_index DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ChatJournalEntry(
                        rs.getLong("message_index"),
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getInt("tokens")
                ),
                conversationId,
                limit,
                offset
        );
    }

    @Override
    public int countVisibleEntries(String conversationId) {
        validateConversationId(conversationId);
        //noinspection DataFlowIssue - COUNT guarantees non-null result
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_journal WHERE conversation_id = ? AND message_type IN ('USER', 'ASSISTANT')",
                Integer.class,
                conversationId
        );
    }

    @Override
    public List<ChatJournalEntry> findEntriesAfterIndex(String conversationId, long messageIndex) {
        validateConversationId(conversationId);
        return jdbcTemplate.query(
                "SELECT message_index, message_type, content, tokens FROM chat_journal WHERE conversation_id = ? AND message_index > ? ORDER BY message_index",
                (rs, rowNum) -> new ChatJournalEntry(
                        rs.getLong("message_index"),
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getInt("tokens")
                ),
                conversationId,
                messageIndex
        );
    }

    @Override
    public int sumTokens(String conversationId) {
        validateConversationId(conversationId);
        //noinspection DataFlowIssue - COALESCE guarantees non-null result
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(tokens), 0) FROM chat_journal WHERE conversation_id = ?",
                Integer.class,
                conversationId
        );
    }

    @Override
    public int sumTokensAfterIndex(String conversationId, long messageIndex) {
        validateConversationId(conversationId);
        //noinspection DataFlowIssue - COALESCE guarantees non-null result
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(tokens), 0) FROM chat_journal WHERE conversation_id = ? AND message_index > ?",
                Integer.class,
                conversationId,
                messageIndex
        );
    }

    @Override
    public void deleteAll(String conversationId) {
        validateConversationId(conversationId);
        jdbcTemplate.update("DELETE FROM chat_journal WHERE conversation_id = ?", conversationId);
    }

    private static void validateConversationId(String conversationId) {
        Objects.requireNonNull(conversationId, "conversationId must not be null");
        if (conversationId.isEmpty()) {
            throw new IllegalArgumentException("conversationId must not be empty");
        }
    }
}
