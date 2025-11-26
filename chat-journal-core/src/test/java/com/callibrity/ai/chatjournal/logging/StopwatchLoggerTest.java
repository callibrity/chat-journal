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
package com.callibrity.ai.chatjournal.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopwatchLoggerTest {

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<Object[]> argsCaptor;

    @Nested
    class Info {

        @BeforeEach
        void setUp() {
            when(logger.isInfoEnabled()).thenReturn(true);
        }

        @Test
        void shouldLogWithElapsedTime() {
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.info("Task completed");

            verify(logger).info(eq("Task completed ({})"), argsCaptor.capture());
            Object[] args = argsCaptor.getValue();
            assertThat(args).hasSize(1);
            assertThat(args[0].toString()).matches("\\d+\\.\\d{2} ms");
        }

        @Test
        void shouldAppendElapsedToExistingArgs() {
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.info("Processed {} items in {} batches", 100, 5);

            verify(logger).info(eq("Processed {} items in {} batches ({})"), argsCaptor.capture());
            Object[] args = argsCaptor.getValue();
            assertThat(args).hasSize(3);
            assertThat(args[0]).isEqualTo(100);
            assertThat(args[1]).isEqualTo(5);
            assertThat(args[2].toString()).matches("\\d+\\.\\d{2} ms");
        }

        @Test
        void shouldNotLogWhenInfoDisabled() {
            when(logger.isInfoEnabled()).thenReturn(false);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.info("Task completed");

            verify(logger, never()).info(any(String.class), any(Object[].class));
        }
    }

    @Nested
    class Debug {

        @Test
        void shouldLogWhenDebugEnabled() {
            when(logger.isDebugEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.debug("Debug message");

            verify(logger).debug(eq("Debug message ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).contains("ms");
        }

        @Test
        void shouldNotLogWhenDebugDisabled() {
            when(logger.isDebugEnabled()).thenReturn(false);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.debug("Debug message");

            verify(logger, never()).debug(any(String.class), any(Object[].class));
        }
    }

    @Nested
    class Warn {

        @Test
        void shouldLogWhenWarnEnabled() {
            when(logger.isWarnEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.warn("Warning message");

            verify(logger).warn(eq("Warning message ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).contains("ms");
        }

        @Test
        void shouldNotLogWhenWarnDisabled() {
            when(logger.isWarnEnabled()).thenReturn(false);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.warn("Warning message");

            verify(logger, never()).warn(any(String.class), any(Object[].class));
        }
    }

    @Nested
    class Error {

        @Test
        void shouldLogWhenErrorEnabled() {
            when(logger.isErrorEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.error("Error message");

            verify(logger).error(eq("Error message ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).contains("ms");
        }

        @Test
        void shouldNotLogWhenErrorDisabled() {
            when(logger.isErrorEnabled()).thenReturn(false);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.error("Error message");

            verify(logger, never()).error(any(String.class), any(Object[].class));
        }
    }

    @Nested
    class Trace {

        @Test
        void shouldLogWhenTraceEnabled() {
            when(logger.isTraceEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.trace("Trace message");

            verify(logger).trace(eq("Trace message ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).contains("ms");
        }

        @Test
        void shouldNotLogWhenTraceDisabled() {
            when(logger.isTraceEnabled()).thenReturn(false);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.trace("Trace message");

            verify(logger, never()).trace(any(String.class), any(Object[].class));
        }
    }

    @Nested
    class Mark {

        @Test
        void shouldResetTimer() throws InterruptedException {
            when(logger.isInfoEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            Thread.sleep(50);
            sw.mark();

            sw.info("After mark");

            verify(logger).info(eq("After mark ({})"), argsCaptor.capture());
            String elapsed = argsCaptor.getValue()[0].toString();
            // After mark, elapsed time should be small (< 50ms typically)
            double value = Double.parseDouble(elapsed.split(" ")[0]);
            assertThat(value).isLessThan(50);
        }
    }

    @Nested
    class CustomConfiguration {

        @Test
        void shouldUseCustomFormat() {
            when(logger.isInfoEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .format("%.0f")
                    .build();

            sw.info("Task completed");

            verify(logger).info(eq("Task completed ({})"), argsCaptor.capture());
            String elapsed = argsCaptor.getValue()[0].toString();
            // Should not have decimal places
            assertThat(elapsed).matches("\\d+ ms");
        }

        @Test
        void shouldUseCustomUnit() {
            when(logger.isInfoEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.SECONDS)
                    .build();

            sw.info("Task completed");

            verify(logger).info(eq("Task completed ({})"), argsCaptor.capture());
            String elapsed = argsCaptor.getValue()[0].toString();
            assertThat(elapsed).endsWith(" s");
        }
    }

    @Nested
    class UnitSuffixes {

        @BeforeEach
        void setUp() {
            when(logger.isInfoEnabled()).thenReturn(true);
        }

        @Test
        void shouldUseNanosecondsSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.NANOS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" ns");
        }

        @Test
        void shouldUseMicrosecondsSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.MICROS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" µs");
        }

        @Test
        void shouldUseMillisecondsSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.MILLIS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" ms");
        }

        @Test
        void shouldUseSecondsSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.SECONDS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" s");
        }

        @Test
        void shouldUseMinutesSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.MINUTES)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" min");
        }

        @Test
        void shouldUseHoursSuffix() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.HOURS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" h");
        }

        @Test
        void shouldUseLowercaseUnitNameForOtherUnits() {
            StopwatchLogger sw = StopwatchLogger.builder()
                    .logger(logger)
                    .unit(ChronoUnit.DAYS)
                    .build();

            sw.info("Task");

            verify(logger).info(eq("Task ({})"), argsCaptor.capture());
            assertThat(argsCaptor.getValue()[0].toString()).endsWith(" days");
        }
    }

    @Nested
    class StaticFactoryMethods {

        @Test
        void shouldCreateFromLogger() {
            when(logger.isInfoEnabled()).thenReturn(true);
            StopwatchLogger sw = StopwatchLogger.start(logger);

            sw.info("Test");

            verify(logger).info(eq("Test ({})"), any(Object[].class));
        }

        @Test
        void shouldCreateFromClass() {
            // This test just verifies the factory method doesn't throw
            StopwatchLogger sw = StopwatchLogger.start(StopwatchLoggerTest.class);
            assertThat(sw).isNotNull();
        }
    }
}
