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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@JdbcTest
@Sql("/schema-h2.sql")
class JdbcChatJournalCheckpointRepositoryTest {

    private static final String CONVERSATION_ID = "test-conversation";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private JdbcChatJournalCheckpointRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcChatJournalCheckpointRepository(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM chat_journal_checkpoint");
    }

    @Nested
    class FindCheckpoint {

        @Test
        void shouldReturnEmptyWhenNoCheckpoint() {
            Optional<ChatJournalCheckpoint> checkpoint = repository.findCheckpoint(CONVERSATION_ID);

            assertThat(checkpoint).isEmpty();
        }

        @Test
        void shouldReturnCheckpointWhenExists() {
            repository.saveCheckpoint(CONVERSATION_ID, new ChatJournalCheckpoint(100, "Test summary", 50));

            Optional<ChatJournalCheckpoint> checkpoint = repository.findCheckpoint(CONVERSATION_ID);

            assertThat(checkpoint).isPresent();
            assertThat(checkpoint.get().checkpointIndex()).isEqualTo(100);
            assertThat(checkpoint.get().summary()).isEqualTo("Test summary");
            assertThat(checkpoint.get().tokens()).isEqualTo(50);
        }
    }

    @Nested
    class SaveCheckpoint {

        @Test
        void shouldSaveNewCheckpoint() {
            ChatJournalCheckpoint checkpoint = new ChatJournalCheckpoint(100, "Test summary", 50);

            repository.saveCheckpoint(CONVERSATION_ID, checkpoint);

            Optional<ChatJournalCheckpoint> retrieved = repository.findCheckpoint(CONVERSATION_ID);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().checkpointIndex()).isEqualTo(100);
            assertThat(retrieved.get().summary()).isEqualTo("Test summary");
            assertThat(retrieved.get().tokens()).isEqualTo(50);
        }

        @Test
        void shouldReplaceExistingCheckpoint() {
            repository.saveCheckpoint(CONVERSATION_ID, new ChatJournalCheckpoint(100, "First summary", 50));
            repository.saveCheckpoint(CONVERSATION_ID, new ChatJournalCheckpoint(200, "Second summary", 75));

            Optional<ChatJournalCheckpoint> retrieved = repository.findCheckpoint(CONVERSATION_ID);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().checkpointIndex()).isEqualTo(200);
            assertThat(retrieved.get().summary()).isEqualTo("Second summary");
            assertThat(retrieved.get().tokens()).isEqualTo(75);
        }

        @Test
        void shouldNotAffectOtherConversationCheckpoints() {
            repository.saveCheckpoint("conversation-1", new ChatJournalCheckpoint(100, "Summary 1", 50));
            repository.saveCheckpoint("conversation-2", new ChatJournalCheckpoint(200, "Summary 2", 75));

            assertThat(repository.findCheckpoint("conversation-1").get().summary()).isEqualTo("Summary 1");
            assertThat(repository.findCheckpoint("conversation-2").get().summary()).isEqualTo("Summary 2");
        }
    }

    @Nested
    class DeleteCheckpoint {

        @Test
        void shouldDeleteExistingCheckpoint() {
            repository.saveCheckpoint(CONVERSATION_ID, new ChatJournalCheckpoint(100, "Summary", 50));

            repository.deleteCheckpoint(CONVERSATION_ID);

            assertThat(repository.findCheckpoint(CONVERSATION_ID)).isEmpty();
        }

        @Test
        void shouldNotFailWhenNoCheckpointExists() {
            repository.deleteCheckpoint(CONVERSATION_ID);

            assertThat(repository.findCheckpoint(CONVERSATION_ID)).isEmpty();
        }

        @Test
        void shouldNotAffectOtherConversations() {
            repository.saveCheckpoint("conversation-1", new ChatJournalCheckpoint(100, "Summary 1", 50));
            repository.saveCheckpoint("conversation-2", new ChatJournalCheckpoint(200, "Summary 2", 75));

            repository.deleteCheckpoint("conversation-1");

            assertThat(repository.findCheckpoint("conversation-1")).isEmpty();
            assertThat(repository.findCheckpoint("conversation-2")).isPresent();
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullJdbcTemplate() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new JdbcChatJournalCheckpointRepository(null))
                    .withMessage("jdbcTemplate must not be null");
        }
    }

    @Nested
    class MethodValidation {

        @Test
        void findCheckpointShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.findCheckpoint(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void findCheckpointShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.findCheckpoint(""))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void saveCheckpointShouldRejectNullConversationId() {
            ChatJournalCheckpoint checkpoint = new ChatJournalCheckpoint(1, "Summary", 10);
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.saveCheckpoint(null, checkpoint))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void saveCheckpointShouldRejectEmptyConversationId() {
            ChatJournalCheckpoint checkpoint = new ChatJournalCheckpoint(1, "Summary", 10);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.saveCheckpoint("", checkpoint))
                    .withMessage("conversationId must not be empty");
        }

        @Test
        void saveCheckpointShouldRejectNullCheckpoint() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.saveCheckpoint(CONVERSATION_ID, null))
                    .withMessage("checkpoint must not be null");
        }

        @Test
        void deleteCheckpointShouldRejectNullConversationId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> repository.deleteCheckpoint(null))
                    .withMessage("conversationId must not be null");
        }

        @Test
        void deleteCheckpointShouldRejectEmptyConversationId() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> repository.deleteCheckpoint(""))
                    .withMessage("conversationId must not be empty");
        }
    }
}
