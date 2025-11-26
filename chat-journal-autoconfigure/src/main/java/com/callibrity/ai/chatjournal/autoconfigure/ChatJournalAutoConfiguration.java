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
