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
package com.callibrity.ai.chatjournal.autoconfigure;

import com.callibrity.ai.chatjournal.memory.ChatJournalChatMemory;
import com.callibrity.ai.chatjournal.memory.ChatJournalCheckpointFactory;
import com.callibrity.ai.chatjournal.memory.ChatJournalCheckpointer;
import com.callibrity.ai.chatjournal.memory.ChatJournalEntryMapper;
import com.callibrity.ai.chatjournal.repository.ChatJournalCheckpointRepository;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.summary.ChatClientMessageSummarizer;
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
import org.springframework.core.task.TaskExecutor;

@AutoConfiguration(
        after = {JdbcAutoConfiguration.class, JTokkitAutoConfiguration.class},
        afterName = {
                "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
                "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration"
        },
        beforeName = "org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration"
)
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
    @ConditionalOnBean(ObjectMapper.class)
    public ChatJournalEntryMapper chatJournalEntryMapper(
            ObjectMapper objectMapper,
            TokenUsageCalculator tokenUsageCalculator) {
        return new ChatJournalEntryMapper(objectMapper, tokenUsageCalculator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageSummarizer.class)
    public ChatJournalCheckpointFactory chatJournalCheckpointFactory(
            MessageSummarizer summarizer,
            TokenUsageCalculator tokenUsageCalculator) {
        return new ChatJournalCheckpointFactory(summarizer, tokenUsageCalculator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = {
            ChatJournalEntryRepository.class,
            ChatJournalCheckpointRepository.class,
            ChatJournalCheckpointFactory.class,
            ChatJournalEntryMapper.class
    })
    public ChatJournalCheckpointer chatJournalCheckpointer(
            ChatJournalEntryRepository entryRepository,
            ChatJournalCheckpointRepository checkpointRepository,
            ChatJournalCheckpointFactory checkpointFactory,
            ChatJournalEntryMapper entryMapper,
            ChatJournalProperties properties) {
        return new ChatJournalCheckpointer(
                entryRepository,
                checkpointRepository,
                checkpointFactory,
                entryMapper,
                properties.getMaxTokens(),
                properties.getMinRetainedEntries()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = {
            ChatJournalEntryRepository.class,
            ChatJournalCheckpointRepository.class,
            ChatJournalCheckpointer.class,
            TaskExecutor.class
    })
    public ChatMemory chatJournalChatMemory(
            ChatJournalEntryRepository entryRepository,
            ChatJournalCheckpointRepository checkpointRepository,
            ChatJournalEntryMapper entryMapper,
            ChatJournalCheckpointer checkpointer,
            TaskExecutor taskExecutor,
            ChatJournalProperties properties) {
        return new ChatJournalChatMemory(
                entryRepository,
                checkpointRepository,
                entryMapper,
                checkpointer,
                taskExecutor,
                properties.getMaxConversationLength()
        );
    }
}
