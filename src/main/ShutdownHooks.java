package main;

import java.util.concurrent.atomic.AtomicInteger;

/** Centralized application shutdown-hook registration. */
public final class ShutdownHooks {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private ShutdownHooks() {}

    public static void register(Runnable... hooks) {
        for (Runnable h : hooks)
            Runtime.getRuntime().addShutdownHook(new Thread(h, "shutdown-hook-" + COUNTER.incrementAndGet()));
    }
}
