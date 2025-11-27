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

import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.jdbc.JdbcChatJournalEntryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass(JdbcChatJournalEntryRepository.class)
public class JdbcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JdbcTemplate.class)
    public ChatJournalEntryRepository jdbcChatJournalEntryRepository(
            JdbcTemplate jdbcTemplate,
            ChatJournalProperties properties) {
        return new JdbcChatJournalEntryRepository(jdbcTemplate, properties.getMinRetainedEntries());
    }
}
