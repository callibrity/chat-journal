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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@JdbcTest
@Sql("/schema-h2.sql")
class JdbcChatJournalEntryRepositoryTest {

    private static final String CONVERSATION_ID = "test-conversation";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcChatJournalEntryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcChatJournalEntryRepository(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM chat_journal_checkpoint");
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
    class FindVisibleEntries {

        @Test
        void shouldReturnOnlyUserAndAssistantMessages() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "User message", 10),
                    new ChatJournalEntry(0, "SYSTEM", "System message", 15),
                    new ChatJournalEntry(0, "ASSISTANT", "Assistant message", 10)
            ));

            List<ChatJournalEntry> entries = repository.findVisibleEntries(CONVERSATION_ID, 0, 10);

            assertThat(entries).hasSize(2);
            assertThat(entries).extracting(ChatJournalEntry::messageType)
                    .containsExactly("ASSISTANT", "USER");
        }

        @Test
        void shouldReturnInReverseChronologicalOrder() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "First", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Second", 15),
                    new ChatJournalEntry(0, "USER", "Third", 10)
            ));

            List<ChatJournalEntry> entries = repository.findVisibleEntries(CONVERSATION_ID, 0, 10);

            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).content()).isEqualTo("Third");
            assertThat(entries.get(1).content()).isEqualTo("Second");
            assertThat(entries.get(2).content()).isEqualTo("First");
        }

        @Test
        void shouldApplyOffsetAndLimit() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "First", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Second", 15),
                    new ChatJournalEntry(0, "USER", "Third", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Fourth", 15)
            ));

            List<ChatJournalEntry> entries = repository.findVisibleEntries(CONVERSATION_ID, 1, 2);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).content()).isEqualTo("Third");
            assertThat(entries.get(1).content()).isEqualTo("Second");
        }
    }

    @Nested
    class CountVisibleEntries {

        @Test
        void shouldCountOnlyUserAndAssistantMessages() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "User message", 10),
                    new ChatJournalEntry(0, "SYSTEM", "System message", 15),
                    new ChatJournalEntry(0, "ASSISTANT", "Assistant message", 10)
            ));

            int count = repository.countVisibleEntries(CONVERSATION_ID);

            assertThat(count).isEqualTo(2);
        }

        @Test
        void shouldReturnZeroWhenNoVisibleEntries() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "SYSTEM", "System message", 15)
            ));

            int count = repository.countVisibleEntries(CONVERSATION_ID);

            assertThat(count).isZero();
        }
    }

    @Nested
    class FindEntriesAfterIndex {

        @Test
        void shouldReturnOnlyEntriesAfterIndex() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "First", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Second", 15),
                    new ChatJournalEntry(0, "USER", "Third", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Fourth", 15)
            ));

            List<ChatJournalEntry> allEntries = repository.findAll(CONVERSATION_ID);
            long indexAfterSecond = allEntries.get(1).messageIndex();

            List<ChatJournalEntry> entries = repository.findEntriesAfterIndex(CONVERSATION_ID, indexAfterSecond);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).content()).isEqualTo("Third");
            assertThat(entries.get(1).content()).isEqualTo("Fourth");
        }
    }

    @Nested
    class SumTokens {

        @Test
        void shouldSumTokensForConversation() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 25),
                    new ChatJournalEntry(0, "USER", "Message 3", 15)
            ));

            int totalTokens = repository.sumTokens(CONVERSATION_ID);

            assertThat(totalTokens).isEqualTo(50);
        }

        @Test
        void shouldReturnZeroWhenNoEntries() {
            int totalTokens = repository.sumTokens(CONVERSATION_ID);

            assertThat(totalTokens).isZero();
        }
    }

    @Nested
    class SumTokensAfterIndex {

        @Test
        void shouldSumTokensAfterIndex() {
            repository.save(CONVERSATION_ID, List.of(
                    new ChatJournalEntry(0, "USER", "Message 1", 10),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 2", 20),
                    new ChatJournalEntry(0, "USER", "Message 3", 30),
                    new ChatJournalEntry(0, "ASSISTANT", "Message 4", 40)
            ));

            List<ChatJournalEntry> allEntries = repository.findAll(CONVERSATION_ID);
            long indexAfterSecond = allEntries.get(1).messageIndex();

            int totalTokens = repository.sumTokensAfterIndex(CONVERSATION_ID, indexAfterSecond);

            // Message 3 (30) + Message 4 (40) = 70
            assertThat(totalTokens).isEqualTo(70);
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
    class ConstructorValidation {

        @Test
        void shouldRejectNullJdbcTemplate() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new JdbcChatJournalEntryRepository(null))
                    .withMessage("jdbcTemplate must not be null");
        }
    }

    @Nested
    class MethodValidation {

        @Test
        void saveShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.save(null, List.of()))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void saveShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.save("", List.of()))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void saveShouldRejectNullEntries() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.save(CONVERSATION_ID, null))
                    .withMessage("entries must not be null");
        }

        @Test
        void findAllShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.findAll(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void findAllShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.findAll(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void sumTokensShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.sumTokens(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void sumTokensShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.sumTokens(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void deleteAllShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.deleteAll(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void deleteAllShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.deleteAll(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void findVisibleEntriesShouldRejectNegativeOffset() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.findVisibleEntries(CONVERSATION_ID, -1, 10))
                    .withMessage("offset must not be negative");
        }

        @Test
        void findVisibleEntriesShouldRejectZeroLimit() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.findVisibleEntries(CONVERSATION_ID, 0, 0))
                    .withMessage("limit must be positive");
        }

        @Test
        void findVisibleEntriesShouldRejectNegativeLimit() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.findVisibleEntries(CONVERSATION_ID, 0, -1))
                    .withMessage("limit must be positive");
        }
    }
}
