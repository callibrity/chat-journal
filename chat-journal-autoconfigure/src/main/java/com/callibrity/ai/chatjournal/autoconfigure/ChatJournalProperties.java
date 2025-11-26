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
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.journal")
public class ChatJournalProperties {

    /**
     * Maximum number of tokens before compaction is triggered.
     */
    private int maxTokens = 8192;

    /**
     * Minimum number of entries to retain after compaction.
     */
    private int minRetainedEntries = 6;

    /**
     * Characters per token for simple token calculator.
     */
    private int charactersPerToken = 4;

    /**
     * Encoding type for JTokkit token calculator.
     */
    private EncodingType encodingType = EncodingType.O200K_BASE;

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMinRetainedEntries() {
        return minRetainedEntries;
    }

    public void setMinRetainedEntries(int minRetainedEntries) {
        this.minRetainedEntries = minRetainedEntries;
    }

    public int getCharactersPerToken() {
        return charactersPerToken;
    }

    public void setCharactersPerToken(int charactersPerToken) {
        this.charactersPerToken = charactersPerToken;
    }

    public EncodingType getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(EncodingType encodingType) {
        this.encodingType = encodingType;
    }
}
