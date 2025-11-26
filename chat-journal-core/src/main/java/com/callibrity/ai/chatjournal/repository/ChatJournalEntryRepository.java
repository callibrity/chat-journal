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
