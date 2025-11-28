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

import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpoint;
import com.callibrity.ai.chatjournal.summary.MessageSummarizer;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatJournalCheckpointFactoryTest {

    @Mock
    private MessageSummarizer summarizer;

    @Mock
    private TokenUsageCalculator tokenUsageCalculator;

    private ChatJournalCheckpointFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ChatJournalCheckpointFactory(summarizer, tokenUsageCalculator);
    }

    @Nested
    class CreateCheckpoint {

        @Test
        void shouldCreateCheckpointWithSummaryAndTokens() {
            List<Message> messages = List.of(
                    new UserMessage("Hello"),
                    new UserMessage("How are you?")
            );
            when(summarizer.summarize(messages)).thenReturn("User greeted and asked about wellbeing");
            when(tokenUsageCalculator.calculateTokenUsage(anyList())).thenReturn(50);

            ChatJournalCheckpoint checkpoint = factory.createCheckpoint(messages, 100);

            assertThat(checkpoint.checkpointIndex()).isEqualTo(100);
            assertThat(checkpoint.summary()).isEqualTo("User greeted and asked about wellbeing");
            assertThat(checkpoint.tokens()).isEqualTo(50);
        }

        @Test
        void shouldRejectNullMessages() {
            assertThatNullPointerException()
                    .isThrownBy(() -> factory.createCheckpoint(null, 100))
                    .withMessage("messages must not be null");
        }
    }

    @Nested
    class GetSummaryPrefix {

        @Test
        void shouldReturnExpectedPrefix() {
            assertThat(ChatJournalCheckpointFactory.getSummaryPrefix())
                    .isEqualTo("Summary of previous conversation: ");
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullSummarizer() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointFactory(null, tokenUsageCalculator))
                    .withMessage("summarizer must not be null");
        }

        @Test
        void shouldRejectNullTokenUsageCalculator() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ChatJournalCheckpointFactory(summarizer, null))
                    .withMessage("tokenUsageCalculator must not be null");
        }
    }
}
