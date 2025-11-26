package com.callibrity.ai.chatjournal.autoconfigure;

import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.callibrity.ai.chatjournal.jtokkit.JTokkitTokenUsageCalculator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(ChatJournalAutoConfiguration.class)
@ConditionalOnClass(JTokkitTokenUsageCalculator.class)
public class JTokkitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenUsageCalculator jtokkitTokenUsageCalculator(ChatJournalProperties properties) {
        return new JTokkitTokenUsageCalculator(properties.getEncodingType());
    }
}
