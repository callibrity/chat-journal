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
 * Exception thrown when adding messages to a conversation would exceed
 * the maximum allowed conversation length.
 */
public class ConversationLimitExceededException extends RuntimeException {

    /** The conversation ID. */
    private final String conversationId;
    /** The current number of messages in the conversation. */
    private final int currentLength;
    /** The maximum allowed conversation length. */
    private final int maxLength;
    /** The number of new messages that were attempted to be added. */
    private final int newMessageCount;

    /**
     * Creates a new ConversationLimitExceededException.
     *
     * @param conversationId the conversation ID
     * @param currentLength the current number of messages in the conversation
     * @param maxLength the maximum allowed conversation length
     * @param newMessageCount the number of new messages attempted to add
     */
    public ConversationLimitExceededException(
            String conversationId,
            int currentLength,
            int maxLength,
            int newMessageCount) {
        super(String.format(
                "Cannot add %d messages to conversation '%s': would exceed maximum length of %d (current: %d)",
                newMessageCount, conversationId, maxLength, currentLength));
        this.conversationId = conversationId;
        this.currentLength = currentLength;
        this.maxLength = maxLength;
        this.newMessageCount = newMessageCount;
    }

    /**
     * Returns the conversation ID.
     *
     * @return the conversation ID
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Returns the current number of messages in the conversation.
     *
     * @return the current conversation length
     */
    public int getCurrentLength() {
        return currentLength;
    }

    /**
     * Returns the maximum allowed conversation length.
     *
     * @return the maximum length
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Returns the number of new messages that were attempted to be added.
     *
     * @return the new message count
     */
    public int getNewMessageCount() {
        return newMessageCount;
    }
}
