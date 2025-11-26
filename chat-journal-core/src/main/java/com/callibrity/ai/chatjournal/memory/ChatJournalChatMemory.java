package com.callibrity.ai.chatjournal.memory;

import com.callibrity.ai.chatjournal.logging.StopwatchLogger;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntry;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.summary.MessageSummarizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.List;
import java.util.Optional;

@Slf4j
public class ChatJournalChatMemory implements ChatMemory {

    private final ChatJournalEntryRepository repository;
    private final TokenUsageCalculator tokenUsageCalculator;
    private final ObjectMapper objectMapper;
    private final MessageSummarizer summarizer;
    private final int maxTokens;
    private final AsyncTaskExecutor compactionExecutor;

    public ChatJournalChatMemory(ChatJournalEntryRepository repository,
                                 TokenUsageCalculator tokenUsageCalculator,
                                 ObjectMapper objectMapper,
                                 MessageSummarizer summarizer,
                                 int maxTokens,
                                 AsyncTaskExecutor compactionExecutor) {
        this.repository = repository;
        this.tokenUsageCalculator = tokenUsageCalculator;
        this.objectMapper = objectMapper;
        this.summarizer = summarizer;
        this.maxTokens = maxTokens;
        this.compactionExecutor = compactionExecutor;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<ChatJournalEntry> entries = messages.stream()
                .map(msg -> ChatJournalEntry.fromMessage(msg, objectMapper, tokenUsageCalculator))
                .toList();
        repository.save(conversationId, entries);

        int totalTokens = repository.getTotalTokens(conversationId);
        if (totalTokens > maxTokens) {
            log.info("Scheduling compaction for conversation {}: {} tokens exceeds max of {}",
                    conversationId, totalTokens, maxTokens);
            compactionExecutor.submit(() -> performCompaction(conversationId));
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return repository.findAll(conversationId).stream()
                .map(entry -> entry.toMessage(objectMapper))
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteAll(conversationId);
    }

    private void performCompaction(String conversationId) {

        createSummaryEntry(conversationId).ifPresent(
                summaryEntry -> {
                    var sw = StopwatchLogger.start(log);
                    repository.replaceEntriesWithSummary(conversationId, summaryEntry);
                    sw.info("Compacted conversation {}", conversationId);
                }
        );
    }

    private Optional<ChatJournalEntry> createSummaryEntry(String conversationId) {
        List<ChatJournalEntry> entriesToCompact = repository.findEntriesForCompaction(conversationId);
        if (entriesToCompact.isEmpty()) {
            log.info("Not enough messages to compact for conversation {}", conversationId);
            return Optional.empty();
        }

        List<Message> messages = entriesToCompact.reversed().stream()
                .map(entry -> entry.toMessage(objectMapper))
                .toList();

        var sw = StopwatchLogger.start(log);
        String summaryContent = "Summary of previous conversation: " + summarizer.summarize(messages);
        sw.info("Created summary for conversation {}", conversationId);
        int tokens = tokenUsageCalculator.calculateTokenUsage(List.of(new SystemMessage(summaryContent)));

        return Optional.of(new ChatJournalEntry(
                entriesToCompact.getFirst().messageIndex(),
                MessageType.SYSTEM.name(),
                summaryContent,
                tokens
        ));
    }
}
