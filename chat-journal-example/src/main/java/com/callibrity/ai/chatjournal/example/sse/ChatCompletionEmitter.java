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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper around Spring's {@link SseEmitter} that provides enhanced logging and error handling
 * for Server-Sent Events (SSE) connections in chat completion scenarios.
 *
 * <p>This class tracks both a conversation ID (shared across multiple connections) and a unique
 * connection ID (specific to this SSE stream). All log messages include both IDs in the format
 * {@code [SSE:conversationId:connectionId]} for easy correlation and debugging.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe completion handling with idempotency via {@link AtomicBoolean}</li>
 *   <li>Automatic error recovery and cleanup on client disconnects or network failures</li>
 *   <li>Detailed logging at appropriate levels (trace for benign race conditions, debug/info for normal flow, warn/error for issues)</li>
 *   <li>Support for named SSE events</li>
 * </ul>
 *
 * @see SseEmitter
 */
@Slf4j
public class ChatCompletionEmitter {

// ------------------------------ FIELDS ------------------------------

    private final String conversationId;
    @Getter
    private final SseEmitter emitter;
    private final String connectionId;
    private final AtomicBoolean completed = new AtomicBoolean(false);

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Creates a new emitter with no timeout (connection can stay open indefinitely).
     *
     * @param conversationId the conversation ID (shared across multiple connections)
     */
    public ChatCompletionEmitter(String conversationId) {
        this(conversationId, Duration.ZERO);
    }

    /**
     * Creates a new emitter with the specified timeout.
     *
     * @param conversationId the conversation ID (shared across multiple connections)
     * @param timeout the SSE connection timeout, or {@link Duration#ZERO} for no timeout
     */
    public ChatCompletionEmitter(String conversationId, Duration timeout) {
        this.conversationId = conversationId;
        this.emitter = new SseEmitter(timeout.isZero() ? 0L : timeout.toMillis());
        this.connectionId = UUID.randomUUID().toString();

        emitter.onCompletion(() -> {
            completed.set(true);
            log.debug("[SSE:{}:{}] Completed", conversationId, connectionId);
        });

        emitter.onTimeout(() -> {
            log.warn("[SSE:{}:{}] Timeout occurred", conversationId, connectionId);
            completeWithError(new TimeoutException("SSE connection timeout"));
        });

        emitter.onError(err -> {
            log.warn("[SSE:{}:{}] Emitter internal error: {}", conversationId, connectionId, err.toString());
            completeWithError(err);
        });
    }

    /**
     * Static factory method to create an emitter with a timeout.
     *
     * @param conversationId the conversation ID (shared across multiple connections)
     * @param timeout the SSE connection timeout, or {@link Duration#ZERO} for no timeout
     * @return a new {@link ChatCompletionEmitter} instance
     */
    public static ChatCompletionEmitter withTimeout(String conversationId, Duration timeout) {
        return new ChatCompletionEmitter(conversationId, timeout);
    }

    // ------------------------------------------------------------
    //  COMPLETION + ERROR HANDLING
    // ------------------------------------------------------------

    /**
     * Completes the emitter with an error. This method is idempotent - multiple calls
     * will only complete the emitter once.
     *
     * @param ex the exception that caused the error
     */
    private void completeWithError(Throwable ex) {
        if (!completed.compareAndSet(false, true)) {
            log.trace("[SSE:{}:{}] completeWithError called but emitter already closed",
                    conversationId, connectionId);
            return;
        }

        log.debug("[SSE:{}:{}] Completing with error: {}", conversationId, connectionId, ex.toString());

        try {
            emitter.completeWithError(ex);
        } catch (IllegalStateException ignored) {
            log.trace("[SSE:{}:{}] completeWithError failed (already closed)",
                    conversationId, connectionId);
        } catch (Exception e) {
            log.warn("[SSE:{}:{}] Unexpected error completing emitter",
                    conversationId, connectionId, e);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Completes the SSE connection normally. This method is idempotent - multiple calls
     * will only complete the emitter once.
     */
    public void complete() {
        if (!completed.compareAndSet(false, true)) {
            log.trace("[SSE:{}:{}] complete() called but emitter already closed",
                    conversationId, connectionId);
            return;
        }

        log.debug("[SSE:{}:{}] Completing connection normally", conversationId, connectionId);

        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            log.trace("[SSE:{}:{}] Emitter already closed during complete()",
                    conversationId, connectionId);
        } catch (Exception e) {
            log.warn("[SSE:{}:{}] Unexpected error while completing emitter",
                    conversationId, connectionId, e);
        }
    }

    /**
     * Sends a final event and then completes the connection normally.
     * If the send fails, the connection will be completed with an error instead.
     *
     * @param eventName the SSE event name
     * @param data the event data
     */
    public void complete(String eventName, Object data) {
        if (trySendInternal(eventName, data)) {
            complete();
        }
    }

    /**
     * Centralized send logic with better logging and concurrency behavior.
     * Handles both named and unnamed events, with automatic error handling and logging.
     *
     * @param eventName the SSE event name, or {@code null} for default unnamed events
     * @param data the event data to send
     * @return {@code true} if the send was successful, {@code false} if it failed or the emitter was already completed
     */
    private boolean trySendInternal(String eventName, Object data) {
        if (completed.get()) {
            log.trace("[SSE:{}:{}] Attempt to send {} after completion ignored",
                    conversationId, connectionId,
                    (eventName != null ? "event '" + eventName + "'" : "data"));
            return false;
        }

        try {
            if (eventName == null) {
                emitter.send(SseEmitter.event().data(data));
            } else {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }

            return true;
        } catch (IOException e) {
            log.debug("[SSE:{}:{}] Client disconnected while sending event {}: {}",
                    conversationId, connectionId,
                    (eventName != null ? "'" + eventName + "'" : "(default)"),
                    e.toString());
            completeWithError(e);
            return false;
        } catch (Exception e) {
            log.error("[SSE:{}:{}] Unexpected exception while sending event {}",
                    conversationId, connectionId,
                    (eventName != null ? "'" + eventName + "'" : "(default)"), e);
            completeWithError(e);
            return false;
        }
    }

    // ------------------------------------------------------------
    //  PUBLIC SEND METHODS
    // ------------------------------------------------------------

    /**
     * Sends a named SSE event to the client.
     *
     * @param eventName the SSE event name
     * @param data the event data
     */
    public void send(String eventName, Object data) {
        trySendInternal(eventName, data);
    }

}
