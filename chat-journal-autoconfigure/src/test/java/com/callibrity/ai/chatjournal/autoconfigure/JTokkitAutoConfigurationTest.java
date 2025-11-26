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

import com.callibrity.ai.chatjournal.jtokkit.JTokkitTokenUsageCalculator;
import com.callibrity.ai.chatjournal.token.TokenUsageCalculator;
import com.knuddels.jtokkit.api.EncodingType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JTokkitAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JTokkitAutoConfiguration.class,
                    ChatJournalAutoConfiguration.class
            ));

    @Test
    void shouldCreateJTokkitTokenUsageCalculatorByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TokenUsageCalculator.class);
            assertThat(context.getBean(TokenUsageCalculator.class))
                    .isInstanceOf(JTokkitTokenUsageCalculator.class);
        });
    }

    @Test
    void shouldNotCreateJTokkitTokenUsageCalculatorWhenCustomBeanExists() {
        contextRunner
                .withUserConfiguration(CustomTokenUsageCalculatorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenUsageCalculator.class);
                    assertThat(context.getBean(TokenUsageCalculator.class))
                            .isNotInstanceOf(JTokkitTokenUsageCalculator.class);
                });
    }

    @Test
    void shouldUseDefaultEncodingType() {
        contextRunner.run(context -> {
            ChatJournalProperties properties = context.getBean(ChatJournalProperties.class);
            assertThat(properties.getEncodingType()).isEqualTo(EncodingType.O200K_BASE);
        });
    }

    @Test
    void shouldApplyEncodingTypeProperty() {
        contextRunner
                .withPropertyValues("chat.journal.encoding-type=CL100K_BASE")
                .run(context -> {
                    ChatJournalProperties properties = context.getBean(ChatJournalProperties.class);
                    assertThat(properties.getEncodingType()).isEqualTo(EncodingType.CL100K_BASE);
                });
    }

    @Test
    void shouldConfigureBeforeChatJournalAutoConfiguration() {
        contextRunner.run(context -> {
            // If JTokkit autoconfiguration runs before ChatJournal autoconfiguration,
            // JTokkitTokenUsageCalculator should be created instead of SimpleTokenUsageCalculator
            assertThat(context).hasSingleBean(TokenUsageCalculator.class);
            assertThat(context.getBean(TokenUsageCalculator.class))
                    .isInstanceOf(JTokkitTokenUsageCalculator.class);
        });
    }

    @Configuration
    static class CustomTokenUsageCalculatorConfig {
        @Bean
        public TokenUsageCalculator tokenUsageCalculator() {
            return mock(TokenUsageCalculator.class);
        }
    }
}
