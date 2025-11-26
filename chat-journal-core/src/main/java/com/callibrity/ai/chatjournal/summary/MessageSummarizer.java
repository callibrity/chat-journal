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
package com.callibrity.ai.chatjournal.summary;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Strategy interface for summarizing conversation messages.
 *
 * <p>Message summarization is used during compaction to condense older conversation history
 * into a shorter summary, freeing up token space while preserving context. This enables
 * longer conversations within LLM context limits.
 *
 * <p>Implementations typically use an LLM to generate human-readable summaries that capture
 * key points, decisions, and context from the conversation.
 *
 * <p>This is a functional interface to allow lambda implementations for simple use cases.
 *
 * @see com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory
 */
@FunctionalInterface
public interface MessageSummarizer {

    /**
     * Summarizes a list of messages into a concise text representation.
     *
     * <p>The summary should capture key points, decisions, and important context needed
     * to continue the conversation meaningfully.
     *
     * @param messages the list of messages to summarize; must not be null or empty
     * @return a concise summary of the conversation; never null
     */
    String summarize(List<Message> messages);
}
