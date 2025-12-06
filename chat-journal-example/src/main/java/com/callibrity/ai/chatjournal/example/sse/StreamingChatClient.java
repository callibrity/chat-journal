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
package com.callibrity.ai.chatjournal.example.sse;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.TaskExecutor;

/**
 * Wrapper around Spring AI's {@link ChatClient} that simplifies Server-Sent Events (SSE) streaming responses.
 *
 * <p>This class handles the common pattern of:
 * <ul>
 *   <li>Creating an SSE emitter with proper lifecycle management</li>
 *   <li>Executing chat streaming on a background thread</li>
 *   <li>Sending initial metadata event</li>
 *   <li>Streaming content chunks as they arrive from the AI model</li>
 *   <li>Sending final completion event with metadata</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *
 *     @Autowired
 *     private StreamingChatClient streamingChatClient;
 *
 *     @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
 *     public SseEmitter chat(@RequestParam String question) {
 *         return streamingChatClient.stream(conversationId)
 *                 .systemPrompt("You are a helpful assistant")
 *                 .onStart(convId -> new Metadata(convId))
 *                 .onChunk(Chunk::new)
 *                 .onComplete(() -> new Done())
 *                 .execute(question);
 *     }
 * }
 * }</pre>
 *
 * @see StreamingRequestBuilder
 * @see ChatCompletionEmitter
 */
public class StreamingChatClient {

    private final ChatClient chatClient;
    private final TaskExecutor executor;

    /**
     * Creates a new StreamingChatClient.
     *
     * @param chatClient the Spring AI ChatClient to wrap
     * @param executor the TaskExecutor for running streaming operations asynchronously
     */
    public StreamingChatClient(ChatClient chatClient, TaskExecutor executor) {
        this.chatClient = chatClient;
        this.executor = executor;
    }

    /**
     * Begin building a streaming request for the given conversation.
     *
     * <p>If the conversationId is {@code null}, a new UUID will be automatically generated
     * for this conversation.
     *
     * @param conversationId the conversation ID to associate with this chat request, or {@code null} to generate a new one
     * @return a builder for configuring the streaming request
     */
    public StreamingRequestBuilder stream(String conversationId) {
        String actualConversationId = conversationId != null
                ? conversationId
                : java.util.UUID.randomUUID().toString();
        return new StreamingRequestBuilder(chatClient, executor, actualConversationId);
    }
}
