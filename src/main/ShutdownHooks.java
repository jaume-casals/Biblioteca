package main;

/** Centralized application shutdown-hook registration. */
public final class ShutdownHooks {
    private ShutdownHooks() {}

    public static void register(Runnable... hooks) {
        for (Runnable h : hooks) Runtime.getRuntime().addShutdownHook(new Thread(h));
    }
}
