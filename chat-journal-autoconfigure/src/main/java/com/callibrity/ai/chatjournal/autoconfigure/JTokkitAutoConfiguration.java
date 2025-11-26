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
