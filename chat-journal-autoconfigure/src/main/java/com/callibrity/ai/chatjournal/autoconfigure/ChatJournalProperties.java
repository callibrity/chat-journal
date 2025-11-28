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

import com.knuddels.jtokkit.api.EncodingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "chat.journal")
@Validated
public class ChatJournalProperties {

    /**
     * Maximum number of tokens before compaction is triggered.
     */
    @Positive
    private int maxTokens = 8192;

    /**
     * Maximum number of messages allowed per conversation.
     */
    @Positive
    private int maxConversationLength = 10000;

    /**
     * Minimum number of entries to retain after compaction.
     */
    @Positive
    private int minRetainedEntries = 6;

    /**
     * Characters per token for simple token calculator.
     */
    @Positive
    private int charactersPerToken = 4;

    /**
     * Encoding type for JTokkit token calculator.
     */
    @NotNull
    private EncodingType encodingType = EncodingType.O200K_BASE;
}
