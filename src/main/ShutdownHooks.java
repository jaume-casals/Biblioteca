package main;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Registre centralitzat dels shutdown hooks de l'aplicació. */
public final class ShutdownHooks {
    private static final Logger LOG = Logger.getLogger(ShutdownHooks.class.getName());
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private ShutdownHooks() {}

    public static Thread register(Runnable hook) {
        Thread t = new Thread(hook, "shutdown-hook-" + COUNTER.incrementAndGet());
        try {
            Runtime.getRuntime().addShutdownHook(t);
            return t;
        } catch (IllegalStateException lateShutdown) {
            // La JVM ja està tancant — no podem afegir el ganxo,
            // però el consumidor pot cridar directament el hook
            // per assegurar-se que es completa la neteja.
            LOG.log(Level.FINE, "register: la JVM ja s'està tancant — invocant el hook directament", lateShutdown);
            try { hook.run(); } catch (Exception ignored) {}
            return null;
        }
    }

    public static void register(Runnable... hooks) {
        for (Runnable h : hooks) register(h);
    }

    /** Elimina un hook registrat. Útil per a tests que reinstancien
     *  objectes i volen evitar l'acumulació de ganxos entre iteracions. */
    public static void unregister(Thread hook) {
        if (hook == null) return;
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException lateShutdown) {
            // Ja estem tancant — no passa res.
        }
    }
}
