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

@Builder
public class StopwatchLogger {
    private final Logger logger;

    @Builder.Default
    private final String format = "%.2f";

    @Builder.Default
    private final ChronoUnit unit = ChronoUnit.MILLIS;

    @Builder.Default
    private long startNanos = System.nanoTime();

    public static StopwatchLogger start(Logger logger) {
        return builder().logger(logger).build();
    }

    public static StopwatchLogger start(Class<?> clazz) {
        return start(LoggerFactory.getLogger(clazz));
    }

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

    public void info(String pattern, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(pattern + " ({})", appendElapsed(args));
        }
    }

    public void debug(String pattern, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(pattern + " ({})", appendElapsed(args));
        }
    }

    public void warn(String pattern, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(pattern + " ({})", appendElapsed(args));
        }
    }

    public void error(String pattern, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(pattern + " ({})", appendElapsed(args));
        }
    }

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
