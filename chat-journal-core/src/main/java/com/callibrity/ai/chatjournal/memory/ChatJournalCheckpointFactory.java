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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;
import java.util.Objects;

/**
 * Factory for creating {@link ChatJournalCheckpoint} instances from messages.
 *
 * <p>This factory encapsulates the logic for summarizing messages and calculating
 * token usage for the resulting checkpoint. It is a pure transformation - it does
 * not interact with any storage or state management.
 *
 * <p>This class is thread-safe.
 */
public class ChatJournalCheckpointFactory {

    private static final String SUMMARY_PREFIX = "Summary of previous conversation: ";

    private final MessageSummarizer summarizer;
    private final TokenUsageCalculator tokenUsageCalculator;

    /**
     * Creates a new ChatJournalCheckpointFactory.
     *
     * @param summarizer the strategy for generating conversation summaries
     * @param tokenUsageCalculator the calculator for estimating token counts
     * @throws NullPointerException if any parameter is null
     */
    public ChatJournalCheckpointFactory(MessageSummarizer summarizer, TokenUsageCalculator tokenUsageCalculator) {
        this.summarizer = Objects.requireNonNull(summarizer, "summarizer must not be null");
        this.tokenUsageCalculator = Objects.requireNonNull(tokenUsageCalculator, "tokenUsageCalculator must not be null");
    }

    /**
     * Creates a checkpoint from a list of messages.
     *
     * <p>The messages are summarized and the resulting summary's token count is calculated.
     * The checkpoint includes the standard summary prefix in its token calculation.
     *
     * @param messages the messages to summarize
     * @param checkpointIndex the message index that this checkpoint covers up to
     * @return a new checkpoint containing the summary
     * @throws NullPointerException if messages is null
     */
    public ChatJournalCheckpoint createCheckpoint(List<Message> messages, long checkpointIndex) {
        Objects.requireNonNull(messages, "messages must not be null");
        String summary = summarizer.summarize(messages);
        String fullSummaryContent = SUMMARY_PREFIX + summary;
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(new SystemMessage(fullSummaryContent)));
        return new ChatJournalCheckpoint(checkpointIndex, summary, tokens);
    }

    /**
     * Returns the prefix used when presenting checkpoint summaries to the LLM.
     *
     * @return the summary prefix string
     */
    public static String getSummaryPrefix() {
        return SUMMARY_PREFIX;
    }
}
