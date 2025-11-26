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
package com.callibrity.ai.chatjournal.jtokkit;

import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JTokkitTokenUsageCalculatorTest {

    @Nested
    class WithMockedEncoding {

        @Mock
        private Encoding encoding;

        private JTokkitTokenUsageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new JTokkitTokenUsageCalculator(encoding);
        }

        @Test
        void shouldDelegateToEncoding() {
            when(encoding.countTokens("Hello")).thenReturn(2);
            UserMessage message = new UserMessage("Hello");

            int tokens = calculator.calculateTokenUsage(List.of(message));

            assertThat(tokens).isEqualTo(2);
        }

        @Test
        void shouldReturnZeroForNullText() {
            AssistantMessage message = new AssistantMessage(null);

            int tokens = calculator.calculateTokenUsage(List.of(message));

            assertThat(tokens).isZero();
        }

        @Test
        void shouldSumTokensForMultipleMessages() {
            when(encoding.countTokens("First")).thenReturn(1);
            when(encoding.countTokens("Second")).thenReturn(2);

            List<Message> messages = List.of(
                    new UserMessage("First"),
                    new UserMessage("Second")
            );

            int tokens = calculator.calculateTokenUsage(messages);

            assertThat(tokens).isEqualTo(3);
        }

        @Test
        void shouldReturnZeroForEmptyList() {
            int tokens = calculator.calculateTokenUsage(List.of());

            assertThat(tokens).isZero();
        }
    }

    @Nested
    class WithRealEncoding {

        private JTokkitTokenUsageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new JTokkitTokenUsageCalculator(EncodingType.CL100K_BASE);
        }

        @Test
        void shouldTokenizeUsingCl100kBase() {
            // "Hello, world!" tokenizes to approximately 4 tokens with cl100k_base
            UserMessage message = new UserMessage("Hello, world!");

            int tokens = calculator.calculateTokenUsage(List.of(message));

            assertThat(tokens).isGreaterThan(0);
        }

        @Test
        void shouldHandleLongerText() {
            String longerText = "The quick brown fox jumps over the lazy dog. This is a test of the token counting functionality.";
            UserMessage message = new UserMessage(longerText);

            int tokens = calculator.calculateTokenUsage(List.of(message));

            // Longer text should produce more tokens
            assertThat(tokens).isGreaterThan(10);
        }

        @Test
        void shouldHandleEmptyString() {
            UserMessage message = new UserMessage("");

            int tokens = calculator.calculateTokenUsage(List.of(message));

            assertThat(tokens).isZero();
        }
    }

    @Nested
    class ConstructorValidation {

        @Test
        void shouldRejectNullEncoding() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new JTokkitTokenUsageCalculator((Encoding) null))
                    .withMessage("encoding must not be null");
        }

        @Test
        void shouldRejectNullEncodingType() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new JTokkitTokenUsageCalculator((EncodingType) null))
                    .withMessage("encodingType must not be null");
        }
    }
}
