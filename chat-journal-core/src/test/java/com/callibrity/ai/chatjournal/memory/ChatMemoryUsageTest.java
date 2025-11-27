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
package com.callibrity.ai.chatjournal.memory;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChatMemoryUsageTest {

    @Nested
    class PercentageUsed {

        @Test
        void shouldReturnZeroWhenNoTokensUsed() {
            var usage = new ChatMemoryUsage(0, 1000);

            assertThat(usage.percentageUsed()).isZero();
        }

        @Test
        void shouldReturnFiftyPercentWhenHalfUsed() {
            var usage = new ChatMemoryUsage(500, 1000);

            assertThat(usage.percentageUsed()).isCloseTo(50.0, within(0.01));
        }

        @Test
        void shouldReturnHundredPercentWhenFullyUsed() {
            var usage = new ChatMemoryUsage(1000, 1000);

            assertThat(usage.percentageUsed()).isCloseTo(100.0, within(0.01));
        }

        @Test
        void shouldExceedHundredPercentWhenOverBudget() {
            var usage = new ChatMemoryUsage(1500, 1000);

            assertThat(usage.percentageUsed()).isCloseTo(150.0, within(0.01));
        }

        @Test
        void shouldReturnZeroWhenMaxTokensIsZero() {
            var usage = new ChatMemoryUsage(500, 0);

            assertThat(usage.percentageUsed()).isZero();
        }
    }

    @Nested
    class TokensRemaining {

        @Test
        void shouldReturnMaxTokensWhenNoneUsed() {
            var usage = new ChatMemoryUsage(0, 1000);

            assertThat(usage.tokensRemaining()).isEqualTo(1000);
        }

        @Test
        void shouldReturnDifferenceWhenPartiallyUsed() {
            var usage = new ChatMemoryUsage(300, 1000);

            assertThat(usage.tokensRemaining()).isEqualTo(700);
        }

        @Test
        void shouldReturnZeroWhenFullyUsed() {
            var usage = new ChatMemoryUsage(1000, 1000);

            assertThat(usage.tokensRemaining()).isZero();
        }

        @Test
        void shouldReturnZeroWhenOverBudget() {
            var usage = new ChatMemoryUsage(1500, 1000);

            assertThat(usage.tokensRemaining()).isZero();
        }
    }
}
