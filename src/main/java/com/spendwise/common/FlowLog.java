package com.spendwise.common;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Times one named step of a user flow and logs it, so slow steps are visible in the terminal.
 * Each line carries the request id and user id from MDC (see {@link RequestLoggingFilter}).
 */
public final class FlowLog {
    private static final Logger LOG = LoggerFactory.getLogger("flow");

    private FlowLog() {
    }

    public static <T> T step(String flow, String step, Supplier<T> work) {
        long started = System.nanoTime();
        try {
            return work.get();
        } finally {
            LOG.info("flow={} step={} ms={}", flow, step, elapsedMs(started));
        }
    }

    public static void run(String flow, String step, Runnable work) {
        step(flow, step, () -> {
            work.run();
            return null;
        });
    }

    public static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
