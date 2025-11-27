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

/**
 * Provides memory usage statistics for chat conversations.
 *
 * <p>This interface allows clients to query memory usage without coupling
 * to the specific {@link ChatJournalChatMemory} implementation.
 *
 * @see ChatMemoryUsage
 * @see ChatJournalChatMemory
 */
public interface ChatMemoryUsageProvider {

    /**
     * Returns the current memory usage statistics for a conversation.
     *
     * @param conversationId the unique identifier for the conversation
     * @return the memory usage statistics for the conversation
     * @throws NullPointerException if conversationId is null
     * @throws IllegalArgumentException if conversationId is empty
     */
    ChatMemoryUsage getMemoryUsage(String conversationId);
}
