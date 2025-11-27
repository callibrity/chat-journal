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

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A wrapper around {@link SseEmitter} that converts checked {@link IOException}s
 * to unchecked {@link UncheckedIOException}s, ensuring proper cleanup on errors.
 *
 * <p>When an {@link IOException} occurs during {@link #send}, this wrapper:
 * <ol>
 *   <li>Calls {@link SseEmitter#completeWithError(Throwable)} to properly close the emitter</li>
 *   <li>Throws an {@link UncheckedIOException} to signal the error to the caller</li>
 * </ol>
 *
 * <p>This design simplifies SSE streaming code by eliminating try-catch blocks while
 * guaranteeing the emitter is always properly completed on error.
 *
 * @see SseEmitter
 * @see UncheckedIOException
 */
public class UncheckedSseEmitter {

    private final SseEmitter emitter;

    private UncheckedSseEmitter(SseEmitter emitter) {
        this.emitter = emitter;
    }

    /**
     * Creates an {@code UncheckedSseEmitter} wrapping the given {@link SseEmitter}.
     *
     * @param emitter the SSE emitter to wrap
     * @return a new {@code UncheckedSseEmitter} instance
     */
    public static UncheckedSseEmitter of(SseEmitter emitter) {
        return new UncheckedSseEmitter(emitter);
    }

    /**
     * Sends an SSE event with the specified name and data.
     *
     * <p>If an {@link IOException} occurs, this method calls
     * {@link SseEmitter#completeWithError(Throwable)} before throwing
     * an {@link UncheckedIOException}.
     *
     * @param eventName the name of the SSE event
     * @param data the data payload for the event
     * @throws UncheckedIOException if an I/O error occurs while sending
     */
    public void send(String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Completes the SSE emitter normally.
     *
     * <p>This signals to the client that no more events will be sent.
     */
    public void complete() {
        emitter.complete();
    }
}
