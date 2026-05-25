package persistencia;

import java.io.File;

/**
 * Resolves the lib/ directory containing JDBC driver JARs, trying multiple strategies in order.
 * Façade over {@code ServerConect#findLibDir}; future refactors should migrate the strategy
 * methods here directly.
 */
public final class LibDirResolver {
    private LibDirResolver() {}

    /** Inputs to a single resolution strategy. Each returns a non-null File or null to fall through. */
    @FunctionalInterface
    public interface Strategy { File tryResolve(StringBuilder diag); }

    private static final Strategy[] STRATEGIES = {
        diag -> systemProperty(diag, "biblioteca.root", "lib"),
        diag -> systemProperty(diag, "user.dir", "lib"),
        diag -> {
            String classPath = System.getProperty("java.class.path", "");
            for (String part : classPath.split(File.pathSeparator)) {
                File f = new File(part);
                File candidate = new File(f.isDirectory() ? f : f.getParentFile(), "lib");
                if (candidate.isDirectory()) return candidate;
            }
            return null;
        }
    };

    private static File systemProperty(StringBuilder diag, String key, String sub) {
        String v = System.getProperty(key);
        if (v == null) return null;
        File c = new File(v, sub);
        diag.append("  ").append(key).append(" + ").append(sub).append(" = ").append(c).append("\n");
        return c.isDirectory() ? c : null;
    }

    public static File resolve(StringBuilder diag) {
        for (Strategy s : STRATEGIES) {
            File f = s.tryResolve(diag);
            if (f != null) return f;
        }
        return null;
    }
}
