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
 * Immutable record representing a persisted chat message entry.
 *
 * <p>This record serves as the data transfer object between the chat memory system
 * and the underlying storage. It captures all information needed to reconstruct a
 * Spring AI Message while also tracking metadata for memory management.
 *
 * <p>Use {@link com.callibrity.ai.chatjournal.memory.ChatJournalEntryMapper} to convert
 * between this record and Spring AI Message instances.
 *
 * @param messageIndex the ordinal position of this message within the conversation;
 *                     used for ordering and identifying entries during compaction
 * @param messageType the type of message (USER or ASSISTANT);
 *                    corresponds to Spring AI's MessageType
 * @param content the message content
 * @param tokens the estimated or calculated token count for this message
 * @see ChatJournalEntryRepository
 * @see com.callibrity.ai.chatjournal.memory.ChatJournalEntryMapper
 */
public record ChatJournalEntry(long messageIndex, String messageType, String content, int tokens) {
}
