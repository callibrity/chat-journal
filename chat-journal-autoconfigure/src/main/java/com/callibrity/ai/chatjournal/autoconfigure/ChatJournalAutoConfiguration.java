package com.callibrity.ai.chatjournal.autoconfigure;

import com.callibrity.ai.chatjournal.summary.ChatClientMessageSummarizer;
import com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.summary.MessageSummarizer;
import com.callibrity.ai.chatjournal.token.SimpleTokenUsageCalculator;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;

@AutoConfiguration
@EnableConfigurationProperties(ChatJournalProperties.class)
public class ChatJournalAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenUsageCalculator simpleTokenUsageCalculator(ChatJournalProperties properties) {
        return new SimpleTokenUsageCalculator(properties.getCharactersPerToken());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ChatClient.Builder.class)
    public MessageSummarizer chatClientMessageSummarizer(ChatClient.Builder builder) {
        return new ChatClientMessageSummarizer(builder.build());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ChatJournalEntryRepository.class, MessageSummarizer.class})
    public ChatMemory chatJournalChatMemory(
            ChatJournalEntryRepository repository,
            TokenUsageCalculator tokenUsageCalculator,
            ObjectMapper objectMapper,
            MessageSummarizer messageSummarizer,
            ChatJournalProperties properties,
            AsyncTaskExecutor taskExecutor) {
        return new ChatJournalChatMemory(
                repository,
                tokenUsageCalculator,
                objectMapper,
                messageSummarizer,
                properties.getMaxTokens(),
                taskExecutor
        );
    }
}
