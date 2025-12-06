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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChatCompletionEmitterTest {

    private static final String CONVERSATION_ID = "conv-123";

    @Nested
    class Construction {

        @Test
        void shouldCreateEmitterWithNoTimeout() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);

            assertThat(emitter.getEmitter()).isNotNull();
            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(0L);
        }

        @Test
        void shouldCreateEmitterWithSpecifiedTimeout() {
            var timeout = Duration.ofSeconds(30);

            var emitter = new ChatCompletionEmitter(CONVERSATION_ID, timeout);

            assertThat(emitter.getEmitter()).isNotNull();
            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(timeout.toMillis());
        }

        @Test
        void shouldCreateEmitterWithZeroTimeoutUsingDurationZero() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID, Duration.ZERO);

            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(0L);
        }

        @Test
        void shouldCreateEmitterViaStaticFactoryMethod() {
            var timeout = Duration.ofMinutes(1);

            var emitter = ChatCompletionEmitter.withTimeout(CONVERSATION_ID, timeout);

            assertThat(emitter.getEmitter()).isNotNull();
            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(timeout.toMillis());
        }
    }

    @Nested
    class CompletionHandling {

        private ChatCompletionEmitter emitter;

        @BeforeEach
        void setUp() {
            emitter = new ChatCompletionEmitter(CONVERSATION_ID);
        }

        @Test
        void shouldCompleteWithoutThrowingException() {
            // Verify that complete() doesn't throw an exception
            emitter.complete();

            // Verify the emitter still exists
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldBeIdempotentWhenCalledMultipleTimes() {
            // Multiple calls should not throw exceptions
            emitter.complete();
            emitter.complete();
            emitter.complete();

            // Verify the emitter still exists
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldCompleteWithEventAndData() {
            // Verify that complete(event, data) doesn't throw an exception
            emitter.complete("finalEvent", "finalData");

            // Verify the emitter still exists
            assertThat(emitter.getEmitter()).isNotNull();
        }
    }

    @Nested
    class SendOperations {

        private ChatCompletionEmitter emitter;

        @BeforeEach
        void setUp() {
            emitter = new ChatCompletionEmitter(CONVERSATION_ID);
        }

        @Test
        void shouldSendNamedEvent() {
            // This test verifies that send doesn't throw an exception
            emitter.send("chatUpdate", "Hello, World!");

            // If we got here, the send was successful
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldNotThrowWhenSendingAfterCompletion() {
            emitter.complete();

            // Sending after completion should be silently ignored
            emitter.send("event", "data");

            // No exception should be thrown
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleMultipleSendsBeforeCompletion() {
            emitter.send("event1", "data1");
            emitter.send("event2", "data2");
            emitter.send("event3", "data3");

            // All sends should succeed without exception
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleSendsWithVariousDataTypes() {
            emitter.send("stringEvent", "string data");
            emitter.send("numberEvent", 42);
            emitter.send("booleanEvent", true);
            emitter.send("objectEvent", new TestData("value"));

            // All sends should succeed without exception
            assertThat(emitter.getEmitter()).isNotNull();
        }
    }

    @Nested
    class ConcurrentCompletionHandling {

        @Test
        void shouldHandleConcurrentCompletionCallsSafely() throws InterruptedException {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);

            // Simulate concurrent completion attempts
            var thread1 = new Thread(emitter::complete);
            var thread2 = new Thread(emitter::complete);
            var thread3 = new Thread(emitter::complete);

            thread1.start();
            thread2.start();
            thread3.start();

            thread1.join();
            thread2.join();
            thread3.join();

            // All threads should complete without exceptions
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleConcurrentSendAndCompletion() throws InterruptedException {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);
            var exceptions = new ArrayList<Exception>();

            var sendThread = new Thread(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        emitter.send("event", "data" + i);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });

            var completeThread = new Thread(() -> {
                try {
                    Thread.sleep(10); // Let some sends happen first
                    emitter.complete();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });

            sendThread.start();
            completeThread.start();

            sendThread.join();
            completeThread.join();

            // Verify no exceptions were thrown
            assertThat(exceptions).isEmpty();
        }

        @Test
        void shouldHandleMultipleThreadsCompletingConcurrently() throws InterruptedException {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);
            var threadCount = 10;
            var threads = new ArrayList<Thread>();
            var exceptions = new ArrayList<Exception>();

            for (int i = 0; i < threadCount; i++) {
                var thread = new Thread(() -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    }
                });
                threads.add(thread);
            }

            threads.forEach(Thread::start);
            for (var thread : threads) {
                thread.join();
            }

            // Verify no exceptions were thrown
            assertThat(exceptions).isEmpty();
        }
    }

    @Nested
    class CompleteWithEventAndData {

        private ChatCompletionEmitter emitter;

        @BeforeEach
        void setUp() {
            emitter = new ChatCompletionEmitter(CONVERSATION_ID);
        }

        @Test
        void shouldSendEventAndCompleteNormally() {
            emitter.complete("done", "Finished processing");

            // Verify no exception was thrown
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldBeIdempotentWhenCalledMultipleTimes() {
            emitter.complete("event1", "data1");
            emitter.complete("event2", "data2");

            // Multiple calls should not throw exceptions
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleNullEventName() {
            // Even though the API uses String, verify it handles edge cases gracefully
            emitter.complete("done", null);

            // Verify no exception was thrown
            assertThat(emitter.getEmitter()).isNotNull();
        }
    }

    @Nested
    class TimeoutConfiguration {

        @Test
        void shouldConfigureShortTimeout() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID, Duration.ofMillis(100));

            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(100L);
        }

        @Test
        void shouldConfigureLongTimeout() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID, Duration.ofHours(1));

            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(Duration.ofHours(1).toMillis());
        }

        @Test
        void shouldHandleZeroDurationAsNoTimeout() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID, Duration.ZERO);

            assertThat(emitter.getEmitter().getTimeout()).isEqualTo(0L);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldHandleSendAfterMultipleCompletions() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);

            emitter.complete();
            emitter.complete();
            emitter.send("event", "data");

            // Should not throw any exceptions
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleRapidSendAndCompleteSequence() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);

            for (int i = 0; i < 10; i++) {
                emitter.send("event" + i, "data" + i);
            }
            emitter.complete();

            // Should complete successfully
            assertThat(emitter.getEmitter()).isNotNull();
        }

        @Test
        void shouldHandleCompleteWithEventAfterRegularComplete() {
            var emitter = new ChatCompletionEmitter(CONVERSATION_ID);

            emitter.complete();
            emitter.complete("finalEvent", "finalData");

            // Should not throw exceptions
            assertThat(emitter.getEmitter()).isNotNull();
        }
    }

    // Helper class for testing various data types
    private record TestData(String value) {}
}
