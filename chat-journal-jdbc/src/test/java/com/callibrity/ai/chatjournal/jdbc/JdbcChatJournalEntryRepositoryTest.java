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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql("/schema-h2.sql")
class JdbcChatJournalEntryRepositoryTest {

    private static final String CONVERSATION_ID = "test-conversation";
    private static final int MIN_RETAINED_ENTRIES = 2;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcChatJournalEntryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcChatJournalEntryRepository(jdbcTemplate, MIN_RETAINED_ENTRIES);
        jdbcTemplate.update("DELETE FROM chat_journal");
    }

    @Nested
    class Save {

        @Test
        void shouldInsertSingleEntry() {
            ChatJournalEntry entry = new ChatJournalEntry(0, "USER", "Hello", 10);

            repository.save(CONVERSATION_ID, List.of(entry));

            List<ChatJournalEntry> entries = repository.findAll(CONVERSATION_ID);
            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().messageType()).isEqualTo("USER");
            assertThat(entries.getFirst().content()).isEqualTo("Hello");
            assertThat(entries.getFirst().tokens()).isEqualTo(10);
        }

        @Test
        void shouldInsertMultipleEntries() {
            List<ChatJournalEntry> toSave = List.of(
                    new ChatJournalEntry(0, "USER", "Hello", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Hi there!", 15)
            );

            repository.save(CONVERSATION_ID, toSave);

            List<ChatJournalEntry> entries = repository.findAll(CONVERSATION_ID);
            assertThat(entries).hasSize(2);
        }

        @Test
        void shouldAutoGenerateMessageIndex() {
            repository.save(CONVERSATION_ID, List.of(new ChatJournalEntry(0, "USER", "First", 10)));
            repository.save(CONVERSATION_ID, List.of(new ChatJournalEntry(0, "USER", "Second", 10)));

            List<ChatJournalEntry> entries = repository.findAll(CONVERSATION_ID);
            assertThat(entries.get(0).messageIndex()).isLessThan(entries.get(1).messageIndex());
        }
    }

    @Nested
    class FindAll {

        @Test
        void shouldReturnEntriesInOrder() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "First", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Second", 15),
                    new ChatJournalEntry(0, "USER", "Third", 10)
            ));

            List<ChatJournalEntry> entries = repository.findAll(CONVERSATION_ID);

            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).content()).isEqualTo("First");
            assertThat(entries.get(1).content()).isEqualTo("Second");
            assertThat(entries.get(2).content()).isEqualTo("Third");
        }

        @Test
        void shouldReturnEmptyListWhenNoEntries() {
            List<ChatJournalEntry> entries = repository.findAll(CONVERSATION_ID);

            assertThat(entries).isEmpty();
        }

        @Test
        void shouldOnlyReturnEntriesForSpecifiedConversation() {
            repository.save("conversation-1", List.of(new ChatJournalEntry(0, "USER", "Conv 1", 10)));
            repository.save("conversation-2", List.of(new ChatJournalEntry(0, "USER", "Conv 2", 10)));

            List<ChatJournalEntry> entries = repository.findAll("conversation-1");

            assertThat(entries).hasSize(1);
            assertThat(entries.getFirst().content()).isEqualTo("Conv 1");
        }
    }

    @Nested
    class FindEntriesForCompaction {

        @Test
        void shouldReturnEntriesExcludingMostRecent() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 15),
                    new ChatJournalEntry(0, "USER", "Message 3", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 4", 15)
            ));

            List<ChatJournalEntry> entries = repository.findEntriesForCompaction(CONVERSATION_ID);

            // With MIN_RETAINED_ENTRIES = 2, should return all but the 2 most recent
            assertThat(entries).hasSize(2);
            // Results are in descending order
            assertThat(entries.get(0).content()).isEqualTo("Message 2");
            assertThat(entries.get(1).content()).isEqualTo("Message 1");
        }

        @Test
        void shouldReturnEmptyWhenNotEnoughEntries() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 15)
            ));

            List<ChatJournalEntry> entries = repository.findEntriesForCompaction(CONVERSATION_ID);

            assertThat(entries).isEmpty();
        }

        @Test
        void shouldReturnEntriesInDescendingOrder() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "First", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Second", 15),
                    new ChatJournalEntry(0, "USER", "Third", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Fourth", 15),
                    new ChatJournalEntry(0, "USER", "Fifth", 10)
            ));

            List<ChatJournalEntry> entries = repository.findEntriesForCompaction(CONVERSATION_ID);

            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).content()).isEqualTo("Third");
            assertThat(entries.get(1).content()).isEqualTo("Second");
            assertThat(entries.get(2).content()).isEqualTo("First");
        }
    }

    @Nested
    class GetTotalTokens {

        @Test
        void shouldSumTokensForConversation() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 25),
                    new ChatJournalEntry(0, "USER", "Message 3", 15)
            ));

            int totalTokens = repository.getTotalTokens(CONVERSATION_ID);

            assertThat(totalTokens).isEqualTo(50);
        }

        @Test
        void shouldReturnZeroWhenNoEntries() {
            int totalTokens = repository.getTotalTokens(CONVERSATION_ID);

            assertThat(totalTokens).isZero();
        }

        @Test
        void shouldOnlySumTokensForSpecifiedConversation() {
            repository.save("conversation-1", List.of(new ChatJournalEntry(0, "USER", "Conv 1", 100)));
            repository.save("conversation-2", List.of(new ChatJournalEntry(0, "USER", "Conv 2", 200)));

            int totalTokens = repository.getTotalTokens("conversation-1");

            assertThat(totalTokens).isEqualTo(100);
        }
    }

    @Nested
    class DeleteAll {

        @Test
        void shouldDeleteAllEntriesForConversation() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 15)
            ));

            repository.deleteAll(CONVERSATION_ID);

            assertThat(repository.findAll(CONVERSATION_ID)).isEmpty();
        }

        @Test
        void shouldNotAffectOtherConversations() {
            repository.save("conversation-1", List.of(new ChatJournalEntry(0, "USER", "Conv 1", 10)));
            repository.save("conversation-2", List.of(new ChatJournalEntry(0, "USER", "Conv 2", 10)));

            repository.deleteAll("conversation-1");

            assertThat(repository.findAll("conversation-1")).isEmpty();
            assertThat(repository.findAll("conversation-2")).hasSize(1);
        }
    }

    @Nested
    class ReplaceEntriesWithSummary {

        @Test
        void shouldDeleteOldEntriesAndInsertSummary() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 15),
                    new ChatJournalEntry(0, "USER", "Message 3", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 4", 15)
            ));

            List<ChatJournalEntry> allEntries = repository.findAll(CONVERSATION_ID);
            long thirdMessageIndex = allEntries.get(2).messageIndex();

            ChatJournalEntry summaryEntry = new ChatJournalEntry(
                    thirdMessageIndex,
                    "SYSTEM",
                    "Summary of previous conversation",
                    50
            );

            repository.replaceEntriesWithSummary(CONVERSATION_ID, summaryEntry);

            List<ChatJournalEntry> remainingEntries = repository.findAll(CONVERSATION_ID);
            assertThat(remainingEntries).hasSize(2);
            assertThat(remainingEntries.get(0).messageType()).isEqualTo("SYSTEM");
            assertThat(remainingEntries.get(0).content()).isEqualTo("Summary of previous conversation");
            assertThat(remainingEntries.get(1).content()).isEqualTo("Message 4");
        }

        @Test
        void shouldPreserveSummaryMessageIndex() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 15)
            ));

            List<ChatJournalEntry> allEntries = repository.findAll(CONVERSATION_ID);
            long firstMessageIndex = allEntries.get(0).messageIndex();

            ChatJournalEntry summaryEntry = new ChatJournalEntry(
                    firstMessageIndex,
                    "SYSTEM",
                    "Summary",
                    30
            );

            repository.replaceEntriesWithSummary(CONVERSATION_ID, summaryEntry);

            List<ChatJournalEntry> remainingEntries = repository.findAll(CONVERSATION_ID);
            assertThat(remainingEntries.get(0).messageIndex()).isEqualTo(firstMessageIndex);
        }
    }
}
