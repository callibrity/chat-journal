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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for configuring and executing streaming chat requests via Server-Sent Events (SSE).
 *
 * <p>This builder provides a fluent API for configuring:
 * <ul>
 *   <li>System prompts (static or dynamic)</li>
 *   <li>Event handlers for start, chunk, and completion events</li>
 *   <li>SSE connection timeout</li>
 *   <li>Custom advisor parameters</li>
 * </ul>
 *
 * <p>Example usage with all options:
 * <pre>{@code
 * streamingChatClient.stream(conversationId)
 *     .systemPrompt("You are a helpful assistant")
 *     .timeout(Duration.ofMinutes(5))
 *     .onStart(convId -> new Metadata(convId))
 *     .onChunk(content -> new Chunk(content))
 *     .onComplete(() -> new Done())
 *     .advisors(a -> a.param("custom", "value"))
 *     .execute("What is the weather today?");
 * }</pre>
 *
 * <p>All handler methods ({@link #onStart}, {@link #onChunk}, {@link #onComplete}) are optional.
 * If not specified, default behavior applies:
 * <ul>
 *   <li>No start event is sent if {@code onStart} is not called</li>
 *   <li>Raw content chunks are sent if {@code onChunk} is not called</li>
 *   <li>Simple completion without data if {@code onComplete} is not called</li>
 * </ul>
 *
 * @see StreamingChatClient
 * @see ChatCompletionEmitter
 */
public class StreamingRequestBuilder {

    private final ChatClient chatClient;
    private final TaskExecutor executor;
    private final String conversationId;

    private Supplier<String> systemPromptProvider;
    private Function<String, Object> onStartHandler;
    private Function<String, Object> onChunkHandler;
    private Supplier<Object> onCompleteHandler;
    private Duration timeout = Duration.ZERO;
    private Consumer<ChatClient.AdvisorSpec> advisorCustomizer;

    /**
     * Package-private constructor. Use {@link StreamingChatClient#stream(String)} to create instances.
     *
     * @param chatClient the Spring AI ChatClient
     * @param executor the TaskExecutor for async operations
     * @param conversationId the conversation ID
     */
    StreamingRequestBuilder(ChatClient chatClient, TaskExecutor executor, String conversationId) {
        this.chatClient = chatClient;
        this.executor = executor;
        this.conversationId = conversationId;
    }

    /**
     * Get the conversation ID for this streaming request.
     *
     * <p>This returns the actual conversation ID that will be used, which may have been
     * auto-generated if {@code null} was passed to {@link StreamingChatClient#stream(String)}.
     *
     * @return the conversation ID
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Set a static system prompt for this chat request.
     *
     * <p>This is a convenience method that delegates to {@link #systemPrompt(Supplier)}.
     *
     * @param systemPrompt the system prompt to include
     * @return this builder for chaining
     */
    public StreamingRequestBuilder systemPrompt(String systemPrompt) {
        return systemPrompt(() -> systemPrompt);
    }

    /**
     * Set a dynamic system prompt provider that will be invoked when the request is executed.
     * This allows the system prompt to be determined based on runtime context.
     *
     * <p>The provider is invoked on the background thread just before streaming begins,
     * allowing it to access thread-local context or perform I/O operations.
     *
     * <p>If the provider returns {@code null}, no system prompt is included.
     *
     * @param systemPromptProvider supplier that provides the system prompt
     * @return this builder for chaining
     */
    public StreamingRequestBuilder systemPrompt(Supplier<String> systemPromptProvider) {
        this.systemPromptProvider = systemPromptProvider;
        return this;
    }

    /**
     * Configure the handler for the initial "metadata" event sent at the start of streaming.
     *
     * <p>The handler receives the conversation ID and should return the data object to send
     * as the metadata event payload. This is typically used to send information like the
     * conversation ID, session metadata, or configuration back to the client.
     *
     * <p>If not specified, no metadata event is sent.
     *
     * @param handler function that receives the conversation ID and creates the metadata object
     * @return this builder for chaining
     */
    public StreamingRequestBuilder onStart(Function<String, Object> handler) {
        this.onStartHandler = handler;
        return this;
    }

    /**
     * Configure the handler for each "chunk" event as content arrives from the AI model.
     *
     * <p>The handler receives each content chunk (as a String) and should return the data object
     * to send as the chunk event payload. This allows wrapping or transforming the raw content
     * before sending it to the client.
     *
     * <p>If not specified, raw content chunks are sent as-is.
     *
     * @param handler function that receives each content chunk and creates the chunk event object
     * @return this builder for chaining
     */
    public StreamingRequestBuilder onChunk(Function<String, Object> handler) {
        this.onChunkHandler = handler;
        return this;
    }

    /**
     * Configure the handler for the final "done" event sent when streaming completes successfully.
     *
     * <p>The handler should return the completion metadata object. This is typically used to send
     * final statistics, token usage, or other completion information back to the client.
     *
     * <p>If not specified, a simple completion is performed without sending a done event.
     *
     * @param handler supplier that creates the completion metadata object
     * @return this builder for chaining
     */
    public StreamingRequestBuilder onComplete(Supplier<Object> handler) {
        this.onCompleteHandler = handler;
        return this;
    }

    /**
     * Set the SSE connection timeout.
     *
     * <p>If the streaming takes longer than this timeout, the connection will be closed.
     * Use {@link Duration#ZERO} for no timeout (the default).
     *
     * @param timeout the timeout duration, or Duration.ZERO for no timeout
     * @return this builder for chaining
     */
    public StreamingRequestBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Customize advisors beyond the default conversation ID parameter.
     *
     * <p>The conversation ID advisor is automatically applied. This method allows adding
     * additional advisor parameters or customizations.
     *
     * @param customizer function to customize advisors
     * @return this builder for chaining
     */
    public StreamingRequestBuilder advisors(Consumer<ChatClient.AdvisorSpec> customizer) {
        this.advisorCustomizer = customizer;
        return this;
    }

    /**
     * Execute the streaming request with the given user prompt.
     *
     * <p>This method:
     * <ol>
     *   <li>Creates a {@link ChatCompletionEmitter} for this request</li>
     *   <li>Submits a background task to the executor</li>
     *   <li>Returns the SSE emitter immediately (non-blocking)</li>
     * </ol>
     *
     * <p>The background task:
     * <ol>
     *   <li>Sends the start event (if configured)</li>
     *   <li>Builds and executes the ChatClient prompt</li>
     *   <li>Streams chunks as they arrive</li>
     *   <li>Sends the completion event (if configured)</li>
     *   <li>Handles errors by completing the emitter with an error</li>
     * </ol>
     *
     * @param userPrompt the user's question/prompt
     * @return the SSE emitter that will stream the response
     */
    public SseEmitter execute(String userPrompt) {
        var emitter = new ChatCompletionEmitter(conversationId, timeout);

        executor.execute(() -> {
            try {
                // Send initial metadata if handler provided
                if (onStartHandler != null) {
                    emitter.send("metadata", onStartHandler.apply(conversationId));
                }

                // Build the prompt
                var promptSpec = chatClient.prompt();

                // Add system prompt if provided
                if (systemPromptProvider != null) {
                    String systemPrompt = systemPromptProvider.get();
                    if (systemPrompt != null) {
                        promptSpec.system(systemPrompt);
                    }
                }

                // Add user prompt
                promptSpec.user(userPrompt);

                // Configure advisors
                promptSpec.advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, conversationId);
                    if (advisorCustomizer != null) {
                        advisorCustomizer.accept(a);
                    }
                });

                // Stream the response
                var flux = promptSpec.stream().content();

                // Send each chunk
                for (var chunk : flux.toIterable()) {
                    if (onChunkHandler != null) {
                        emitter.send("chunk", onChunkHandler.apply(chunk));
                    } else {
                        emitter.send("chunk", chunk);
                    }
                }

                // Send completion event if handler provided
                if (onCompleteHandler != null) {
                    emitter.complete("done", onCompleteHandler.get());
                } else {
                    emitter.complete();
                }

            } catch (Exception e) {
                // Emitter will handle error completion internally via its error handlers
                // No need to explicitly handle here as ChatCompletionEmitter manages this
            }
        });

        return emitter.getEmitter();
    }
}
