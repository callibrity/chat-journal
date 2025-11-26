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

import com.callibrity.ai.chatjournal.jdbc.JdbcChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JdbcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JdbcAutoConfiguration.class,
                    ChatJournalAutoConfiguration.class
            ));

    @Test
    void shouldCreateJdbcChatJournalEntryRepositoryWhenJdbcTemplateExists() {
        contextRunner
                .withUserConfiguration(DataSourceConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalEntryRepository.class);
                    assertThat(context.getBean(ChatJournalEntryRepository.class))
                            .isInstanceOf(JdbcChatJournalEntryRepository.class);
                });
    }

    @Test
    void shouldNotCreateRepositoryWhenJdbcTemplateIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ChatJournalEntryRepository.class);
        });
    }

    @Test
    void shouldNotCreateRepositoryWhenCustomBeanExists() {
        contextRunner
                .withUserConfiguration(DataSourceConfig.class, CustomRepositoryConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalEntryRepository.class);
                    assertThat(context.getBean(ChatJournalEntryRepository.class))
                            .isNotInstanceOf(JdbcChatJournalEntryRepository.class);
                });
    }

    @Test
    void shouldApplyMinRetainedEntriesProperty() {
        contextRunner
                .withUserConfiguration(DataSourceConfig.class)
                .withPropertyValues("chat.journal.min-retained-entries=10")
                .run(context -> {
                    assertThat(context).hasSingleBean(ChatJournalEntryRepository.class);
                    ChatJournalProperties properties = context.getBean(ChatJournalProperties.class);
                    assertThat(properties.getMinRetainedEntries()).isEqualTo(10);
                });
    }

    @Configuration
    static class DataSourceConfig {
        @Bean
        public JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(mock(DataSource.class));
        }
    }

    @Configuration
    static class CustomRepositoryConfig {
        @Bean
        public ChatJournalEntryRepository chatJournalEntryRepository() {
            return mock(ChatJournalEntryRepository.class);
        }
    }
}
