package main;

/** Centralized shutdown-hook registration shared by web and swing modes. */
public final class ShutdownHooks {
    private ShutdownHooks() {}

    public static void register(Runnable... hooks) {
        for (Runnable h : hooks) Runtime.getRuntime().addShutdownHook(new Thread(h));
    }
}
