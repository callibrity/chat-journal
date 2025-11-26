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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatJournalPropertiesTest {

    @Test
    void shouldHaveDefaultMaxTokens() {
        ChatJournalProperties properties = new ChatJournalProperties();
        assertThat(properties.getMaxTokens()).isEqualTo(8192);
    }

    @Test
    void shouldSetMaxTokens() {
        ChatJournalProperties properties = new ChatJournalProperties();
        properties.setMaxTokens(16384);
        assertThat(properties.getMaxTokens()).isEqualTo(16384);
    }

    @Test
    void shouldHaveDefaultMinRetainedEntries() {
        ChatJournalProperties properties = new ChatJournalProperties();
        assertThat(properties.getMinRetainedEntries()).isEqualTo(6);
    }

    @Test
    void shouldSetMinRetainedEntries() {
        ChatJournalProperties properties = new ChatJournalProperties();
        properties.setMinRetainedEntries(10);
        assertThat(properties.getMinRetainedEntries()).isEqualTo(10);
    }

    @Test
    void shouldHaveDefaultCharactersPerToken() {
        ChatJournalProperties properties = new ChatJournalProperties();
        assertThat(properties.getCharactersPerToken()).isEqualTo(4);
    }

    @Test
    void shouldSetCharactersPerToken() {
        ChatJournalProperties properties = new ChatJournalProperties();
        properties.setCharactersPerToken(5);
        assertThat(properties.getCharactersPerToken()).isEqualTo(5);
    }

    @Test
    void shouldHaveDefaultEncodingType() {
        ChatJournalProperties properties = new ChatJournalProperties();
        assertThat(properties.getEncodingType()).isEqualTo(EncodingType.O200K_BASE);
    }

    @Test
    void shouldSetEncodingType() {
        ChatJournalProperties properties = new ChatJournalProperties();
        properties.setEncodingType(EncodingType.CL100K_BASE);
        assertThat(properties.getEncodingType()).isEqualTo(EncodingType.CL100K_BASE);
    }
}
