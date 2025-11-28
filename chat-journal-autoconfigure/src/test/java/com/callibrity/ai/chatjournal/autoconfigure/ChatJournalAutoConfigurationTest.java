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
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatJournalAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ChatJournalAutoConfiguration.class));

    @Test
    void shouldCreateSimpleTokenUsageCalculator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TokenUsageCalculator.class);
            assertThat(context.getBean(TokenUsageCalculator.class))
                    .isInstanceOf(SimpleTokenUsageCalculator.class);
        });
    }

    @Test
    void shouldNotCreateSimpleTokenUsageCalculatorWhenCustomBeanExists() {
        contextRunner
                .withUserConfiguration(CustomTokenUsageCalculatorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenUsageCalculator.class);
                    assertThat(context.getBean(TokenUsageCalculator.class))
                            .isNotInstanceOf(SimpleTokenUsageCalculator.class);
                });
    }

    @Test
    void shouldCreateChatClientMessageSummarizerWhenChatClientBuilderExists() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSummarizer.class);
                    assertThat(context.getBean(MessageSummarizer.class))
                            .isInstanceOf(ChatClientMessageSummarizer.class);
                });
    }

    @Test
    void shouldNotCreateMessageSummarizerWhenChatClientBuilderMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(MessageSummarizer.class);
        });
    }

    @Test
    void shouldNotCreateMessageSummarizerWhenCustomBeanExists() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderConfig.class, CustomMessageSummarizerConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MessageSummarizer.class);
                    assertThat(context.getBean(MessageSummarizer.class))
                            .isNotInstanceOf(ChatClientMessageSummarizer.class);
                });
    }

    @Test
    void shouldCreateEntryMapper() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalEntryMapper.class);
                });
    }

    @Test
    void shouldCreateCheckpointFactoryWhenMessageSummarizerExists() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalCheckpointFactory.class);
                });
    }

    @Test
    void shouldNotCreateCheckpointFactoryWhenMessageSummarizerMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ChatJournalCheckpointFactory.class);
        });
    }

    @Test
    void shouldCreateCheckpointerWhenAllDependenciesExist() {
        contextRunner
                .withUserConfiguration(
                        ChatClientBuilderConfig.class,
                        RepositoriesConfig.class
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalCheckpointer.class);
                });
    }

    @Test
    void shouldNotCreateCheckpointerWhenRepositoriesMissing() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ChatJournalCheckpointer.class);
                });
    }

    @Test
    void shouldCreateChatJournalChatMemoryWhenAllDependenciesExist() {
        contextRunner
                .withUserConfiguration(
                        ChatClientBuilderConfig.class,
                        RepositoriesConfig.class
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatMemory.class);
                    assertThat(context.getBean(ChatMemory.class))
                            .isInstanceOf(ChatJournalChatMemory.class);
                });
    }

    @Test
    void shouldNotCreateChatMemoryWhenRepositoriesMissing() {
        contextRunner
                .withUserConfiguration(ChatClientBuilderConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ChatMemory.class);
                });
    }

    @Test
    void shouldNotCreateChatMemoryWhenMessageSummarizerMissing() {
        contextRunner
                .withUserConfiguration(RepositoriesConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ChatMemory.class);
                });
    }

    @Test
    void shouldApplyPropertiesConfiguration() {
        contextRunner
                .withPropertyValues("chat.journal.characters-per-token=5")
                .run(context -> {
                    ChatJournalProperties properties = context.getBean(ChatJournalProperties.class);
                    assertThat(properties.getCharactersPerToken()).isEqualTo(5);
                });
    }

    @Configuration
    static class CustomTokenUsageCalculatorConfig {
        @Bean
        public TokenUsageCalculator tokenUsageCalculator() {
            return mock(TokenUsageCalculator.class);
        }
    }

    @Configuration
    static class ChatClientBuilderConfig {
        @Bean
        public ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }

    @Configuration
    static class CustomMessageSummarizerConfig {
        @Bean
        public MessageSummarizer messageSummarizer() {
            return mock(MessageSummarizer.class);
        }
    }

    @Configuration
    static class RepositoriesConfig {
        @Bean
        public ChatJournalEntryRepository chatJournalEntryRepository() {
            return mock(ChatJournalEntryRepository.class);
        }

        @Bean
        public ChatJournalCheckpointRepository chatJournalCheckpointRepository() {
            return mock(ChatJournalCheckpointRepository.class);
        }

        @Bean
        public TaskExecutor taskExecutor() {
            return mock(TaskExecutor.class);
        }
    }
}
