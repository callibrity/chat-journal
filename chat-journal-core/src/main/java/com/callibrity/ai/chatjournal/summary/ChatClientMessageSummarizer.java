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
package com.callibrity.ai.chatjournal.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ChatClientMessageSummarizer implements MessageSummarizer {

    private final ChatClient chatClient;

    @Override
    public String summarize(List<Message> messages) {
        log.info("Summarizing {} messages", messages.size());

        return chatClient.prompt()
                .messages(messages)
                .user("Please provide a concise summary of the conversation above. Capture the key points, decisions, and any important context needed to continue.")
                .call()
                .content();
    }
}
