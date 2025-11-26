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

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static java.util.Optional.ofNullable;


/**
 * A utility class for logging operations with elapsed time measurement.
 *
 * <p>StopwatchLogger simplifies the common pattern of timing operations and logging the results.
 * It wraps an SLF4J Logger and automatically appends formatted elapsed time to log messages.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * var sw = StopwatchLogger.start(log);
 * // ... perform operation ...
 * sw.info("Operation completed");  // Logs: "Operation completed (123.45 ms)"
 * }</pre>
 *
 * <h2>Time Units</h2>
 * <p>The default time unit is milliseconds, but this can be customized via the builder:
 * <pre>{@code
 * var sw = StopwatchLogger.builder()
 *     .logger(log)
 *     .unit(ChronoUnit.SECONDS)
 *     .format("%.3f")
 *     .build();
 * }</pre>
 *
 * <h2>Restarting the Timer</h2>
 * <p>Use {@link #mark()} to reset the start time for timing multiple sequential operations:
 * <pre>{@code
 * var sw = StopwatchLogger.start(log);
 * // ... first operation ...
 * sw.info("First operation completed");
 * sw.mark();  // Reset timer
 * // ... second operation ...
 * sw.info("Second operation completed");
 * }</pre>
 *
 * <p>This class is not thread-safe. Each thread should use its own instance.
 *
 * @see Logger
 */
public class StopwatchLogger {
    private final Logger logger;

    private final String format;

    private final ChronoUnit unit;

    private long startNanos = System.nanoTime();

    /**
     * Creates a new StopwatchLogger with the specified configuration.
     *
     * @param logger the SLF4J Logger to delegate log calls to
     * @param unit the time unit for displaying elapsed time; defaults to milliseconds if null
     * @param format the format string for elapsed time (e.g., "%.2f"); defaults to "%.2f" if null
     */
    @Builder
    public StopwatchLogger(Logger logger, ChronoUnit unit, String format) {
        this.logger = logger;
        this.unit = ofNullable(unit).orElse(ChronoUnit.MILLIS);
        this.format = ofNullable(format).orElse("%.2f");
    }

    /**
     * Creates and starts a new StopwatchLogger with default settings.
     *
     * @param logger the SLF4J Logger to use
     * @return a new StopwatchLogger that has already started timing
     */
    public static StopwatchLogger start(Logger logger) {
        return builder().logger(logger).build();
    }

    /**
     * Creates and starts a new StopwatchLogger for the given class.
     *
     * <p>This is a convenience method that creates a Logger for the class using
     * {@link LoggerFactory#getLogger(Class)}.
     *
     * @param clazz the class to create a Logger for
     * @return a new StopwatchLogger that has already started timing
     */
    public static StopwatchLogger start(Class<?> clazz) {
        return start(LoggerFactory.getLogger(clazz));
    }

    /**
     * Resets the start time to the current time.
     *
     * <p>Use this method to time multiple sequential operations with the same
     * StopwatchLogger instance.
     */
    public void mark() {
        this.startNanos = System.nanoTime();
    }

    private double elapsed() {
        long nanos = System.nanoTime() - startNanos;
        return nanos / (double) unit.getDuration().toNanos();
    }

    private String elapsedFormatted() {
        return String.format(format, elapsed()) + " " + unitSuffix(unit);
    }

    private Object[] appendElapsed(Object[] args) {
        Object[] newArgs = Arrays.copyOf(args, args.length + 1);
        newArgs[args.length] = elapsedFormatted();
        return newArgs;
    }

    /**
     * Logs a message at INFO level with elapsed time appended.
     *
     * @param pattern the log message pattern (SLF4J format)
     * @param args the arguments to substitute in the pattern
     */
    public void info(String pattern, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(pattern + " ({})", appendElapsed(args));
        }
    }

    /**
     * Logs a message at DEBUG level with elapsed time appended.
     *
     * @param pattern the log message pattern (SLF4J format)
     * @param args the arguments to substitute in the pattern
     */
    public void debug(String pattern, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(pattern + " ({})", appendElapsed(args));
        }
    }

    /**
     * Logs a message at WARN level with elapsed time appended.
     *
     * @param pattern the log message pattern (SLF4J format)
     * @param args the arguments to substitute in the pattern
     */
    public void warn(String pattern, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(pattern + " ({})", appendElapsed(args));
        }
    }

    /**
     * Logs a message at ERROR level with elapsed time appended.
     *
     * @param pattern the log message pattern (SLF4J format)
     * @param args the arguments to substitute in the pattern
     */
    public void error(String pattern, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(pattern + " ({})", appendElapsed(args));
        }
    }

    /**
     * Logs a message at TRACE level with elapsed time appended.
     *
     * @param pattern the log message pattern (SLF4J format)
     * @param args the arguments to substitute in the pattern
     */
    public void trace(String pattern, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(pattern + " ({})", appendElapsed(args));
        }
    }

    private static String unitSuffix(ChronoUnit unit) {
        return switch (unit) {
            case NANOS -> "ns";
            case MICROS -> "µs";
            case MILLIS -> "ms";
            case SECONDS -> "s";
            case MINUTES -> "min";
            case HOURS -> "h";
            default -> unit.toString().toLowerCase();
        };
    }
}
