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

/**
 * Immutable record representing a checkpoint for conversation compaction.
 *
 * <p>A checkpoint stores a summary of older messages in a conversation, allowing
 * the original messages to be preserved while only sending the summary and recent
 * messages to the LLM. This enables full conversation history to be maintained
 * for UI display while staying within LLM context window constraints.
 *
 * <p>Each conversation has at most one checkpoint, which is updated whenever
 * compaction occurs. The checkpoint index indicates which messages have been
 * summarized—all messages up to and including that index are covered by the summary.
 *
 * @param checkpointIndex the message index up to which this checkpoint summarizes;
 *                        messages with index greater than this are not included in the summary
 * @param summary the summarized content of older messages
 * @param tokens the estimated or calculated token count for the summary
 * @see ChatJournalEntry
 * @see ChatJournalRepository
 */
public record ChatJournalCheckpoint(long checkpointIndex, String summary, int tokens) {
}
