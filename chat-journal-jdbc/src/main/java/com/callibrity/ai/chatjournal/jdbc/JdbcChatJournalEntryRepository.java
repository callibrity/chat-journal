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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class JdbcChatJournalEntryRepository implements ChatJournalEntryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final int minRetainedEntries;

    public JdbcChatJournalEntryRepository(JdbcTemplate jdbcTemplate, int minRetainedEntries) {
        this.jdbcTemplate = jdbcTemplate;
        this.minRetainedEntries = minRetainedEntries;
    }

    @Override
    public void save(String conversationId, List<ChatJournalEntry> entries) {
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
    public List<ChatJournalEntry> findEntriesForCompaction(String conversationId) {
        return jdbcTemplate.query(
                "SELECT message_index, message_type, content, tokens FROM chat_journal WHERE conversation_id = ? ORDER BY message_index DESC OFFSET ?",
                (rs, rowNum) -> new ChatJournalEntry(
                        rs.getLong("message_index"),
                        rs.getString("message_type"),
                        rs.getString("content"),
                        rs.getInt("tokens")
                ),
                conversationId,
                minRetainedEntries
        );
    }

    @Override
    public int getTotalTokens(String conversationId) {
        //noinspection DataFlowIssue - COALESCE guarantees non-null result
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(tokens), 0) FROM chat_journal WHERE conversation_id = ?",
                Integer.class,
                conversationId
        );
    }

    @Override
    public void deleteAll(String conversationId) {
        jdbcTemplate.update("DELETE FROM chat_journal WHERE conversation_id = ?", conversationId);
    }

    @Override
    @Transactional
    public void replaceEntriesWithSummary(String conversationId, ChatJournalEntry summaryEntry) {
        jdbcTemplate.update(
                "DELETE FROM chat_journal WHERE conversation_id = ? AND message_index <= ?",
                conversationId,
                summaryEntry.messageIndex()
        );
        jdbcTemplate.update(
                "INSERT INTO chat_journal (message_index, conversation_id, message_type, content, tokens) VALUES (?, ?, ?, ?, ?)",
                summaryEntry.messageIndex(),
                conversationId,
                summaryEntry.messageType(),
                summaryEntry.content(),
                summaryEntry.tokens()
        );
    }
}
