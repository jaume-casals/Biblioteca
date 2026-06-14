package persistencia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Localitza el directori {@code lib/} que conté els JAR del projecte.
 * S'intenten 5 estratègies en ordre fins que una funcioni:
 * <ol>
 *   <li>System property {@code biblioteca.root} + "/lib"</li>
 *   <li>{@code user.dir/lib}</li>
 *   <li>Subdirs d'{@code user.dir} que continguin un "lib"</li>
 *   <li>Walk up fins 6 nivells des d'{@code user.dir}</li>
 *   <li>Walk up fins 10 nivells des del classloader code source</li>
 * </ol>
 * Cada intent s'enregistra en un {@code StringBuilder diag} per al diagnòstic
 * d'errors de connexió JDBC.
 */
public final class JarLocator {

    private JarLocator() {}

    @FunctionalInterface
    public interface Strategy {
        /** Retorna el path {@code lib/} trobat, o null per continuar provant. */
        File tryLocate(StringBuilder diag, Predicate<File> hasJars);
    }

    private static final List<Strategy> STRATEGIES = new ArrayList<>();

    static {
        STRATEGIES.add(JarLocator::fromBibliotecaRoot);
        STRATEGIES.add(JarLocator::fromUserDir);
        STRATEGIES.add(JarLocator::fromUserDirSiblings);
        STRATEGIES.add((d, h) -> walkUp(d, h, 6, "user.dir"));
        STRATEGIES.add((d, h) -> walkUpFromClassSource(d, h, 10));
    }

    /**
     * Tria la primera estratègia que troba un directori amb JARs.
     * Retorna un path per defecte ({@code user.dir/lib}) si cap funciona.
     */
    public static File locate(StringBuilder diag, Predicate<File> hasJars) {
        for (int i = 0; i < STRATEGIES.size(); i++) {
            diag.append("  [").append(i + 1).append("] ");
            File found = STRATEGIES.get(i).tryLocate(diag, hasJars);
            if (found != null) return found;
        }
        return new File(System.getProperty("user.dir"), "lib");
    }

    private static File fromBibliotecaRoot(StringBuilder diag, Predicate<File> hasJars) {
        diag.append("biblioteca.root=").append(System.getProperty("biblioteca.root")).append("\n");
        String root = System.getProperty("biblioteca.root");
        if (root == null || root.isBlank()) return null;
        File lib = new File(root, "lib");
        return recordAndReturn(lib, diag, hasJars);
    }

    private static File fromUserDir(StringBuilder diag, Predicate<File> hasJars) {
        diag.append("user.dir=").append(System.getProperty("user.dir")).append("\n");
        File lib = new File(System.getProperty("user.dir"), "lib");
        return recordAndReturn(lib, diag, hasJars);
    }

    private static File fromUserDirSiblings(StringBuilder diag, Predicate<File> hasJars) {
        File[] children = new File(System.getProperty("user.dir")).listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) {
            File lib = new File(child, "lib");
            if (recordAndReturn(lib, diag, hasJars) != null) return lib;
        }
        return null;
    }

    private static File walkUp(StringBuilder diag, Predicate<File> hasJars,
                               int maxLevels, String originLabel) {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < maxLevels; i++) {
            if (dir == null) return null;
            File lib = new File(dir, "lib");
            if (recordAndReturn(lib, diag, hasJars) != null) return lib;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static File walkUpFromClassSource(StringBuilder diag, Predicate<File> hasJars, int maxLevels) {
        java.net.URL loc;
        try {
            loc = JarLocator.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (RuntimeException se) {
            // SecurityException is a RuntimeException; some JVMs surface
            // the code-source access denial as plain IllegalArgument or
            // SecurityException. Catch both to keep the locator robust.
            diag.append("  classSource=SecurityException:").append(se.getMessage()).append("\n");
            return null;
        }
        if (loc == null) {
            diag.append("  classSource=null\n");
            return null;
        }
        diag.append("  classSource=").append(loc).append("\n");
        File dir;
        try {
            dir = new File(loc.toURI());
        } catch (java.net.URISyntaxException ue) {
            diag.append("  classSource=URISyntaxException:").append(ue.getMessage()).append("\n");
            return null;
        } catch (java.lang.IllegalArgumentException iae) {
            diag.append("  classSource=IllegalArgument:").append(iae.getMessage()).append("\n");
            return null;
        }
        for (int i = 0; i < maxLevels; i++) {
            if (dir == null) return null;
            File lib = new File(dir, "lib");
            if (recordAndReturn(lib, diag, hasJars) != null) return lib;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static File recordAndReturn(File lib, StringBuilder diag, Predicate<File> hasJars) {
        String label = hasJars.test(lib) ? " HAS_JARS"
            : lib.isDirectory() ? " no-jars" : " missing";
        diag.append(lib.getAbsolutePath()).append(label).append("\n");
        return hasJars.test(lib) ? lib : null;
    }
}
