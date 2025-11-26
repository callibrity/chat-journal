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

import java.util.List;

public interface ChatJournalEntryRepository {
    void save(String conversationId, List<ChatJournalEntry> entries);
    List<ChatJournalEntry> findAll(String conversationId);
    List<ChatJournalEntry> findEntriesForCompaction(String conversationId);
    int getTotalTokens(String conversationId);
    void deleteAll(String conversationId);
    void replaceEntriesWithSummary(String conversationId, ChatJournalEntry summaryEntry);
}
