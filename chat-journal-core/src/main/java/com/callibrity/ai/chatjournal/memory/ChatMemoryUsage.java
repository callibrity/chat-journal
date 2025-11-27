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

/**
 * Represents the current memory usage statistics for a conversation.
 *
 * <p>This record provides information about how much of the allocated memory budget
 * is being used by conversation history, helping clients understand when compaction
 * might occur.
 *
 * @param currentTokens the number of tokens currently used by conversation history
 * @param maxTokens the maximum tokens allowed before compaction is triggered
 */
public record ChatMemoryUsage(int currentTokens, int maxTokens) {

    /**
     * Calculates the percentage of memory budget currently in use.
     *
     * @return the percentage used (0.0 to 100.0+), may exceed 100 if over budget
     */
    public double percentageUsed() {
        if (maxTokens == 0) {
            return 0.0;
        }
        return (double) currentTokens / maxTokens * 100.0;
    }

    /**
     * Calculates the number of tokens remaining before reaching the maximum.
     *
     * @return tokens remaining, or 0 if at or over the maximum
     */
    public int tokensRemaining() {
        return Math.max(0, maxTokens - currentTokens);
    }
}
