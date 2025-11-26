package com.callibrity.ai.chatjournal.autoconfigure;

import com.callibrity.ai.chatjournal.repository.ChatJournalEntryRepository;
import com.callibrity.ai.chatjournal.jdbc.JdbcChatJournalEntryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
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
