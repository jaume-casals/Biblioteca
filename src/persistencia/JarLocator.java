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
    public interface Estrategia {
        /** Retorna el path {@code lib/} trobat, o null per continuar provant. */
        File tryLocate(StringBuilder diag, Predicate<File> teJars);
    }

    private static final List<Estrategia> STRATEGIES = new ArrayList<>();

    static {
        STRATEGIES.add(JarLocator::fromBibliotecaRoot);
        STRATEGIES.add(JarLocator::fromUserDir);
        STRATEGIES.add(JarLocator::fromUserDirSiblings);
        STRATEGIES.add((d, h) -> walkUp(d, h, 6, "user.dir"));
        STRATEGIES.add((d, h) -> walkUpFromClassSource(d, h, 10));
    }

    /**
     * Tria la primera estratègia que troba un directori amb JARs.
     * Retorna {@code null} si cap funciona — el caller ({@code
     * ConnexioServidor.loadDriverFromLib}) llança un
     * {@code ClassNotFoundException} amb el diag log. L'antic
     * fallback ({@code new File(user.dir, "lib")}) emmascarava
     * el problema retornant un directori existent però sense el
     * JAR que es volia carregar.
     */
    public static File locate(StringBuilder diag, Predicate<File> teJars) {
        for (int i = 0; i < STRATEGIES.size(); i++) {
            diag.append("  [").append(i + 1).append("] ");
            File found = STRATEGIES.get(i).tryLocate(diag, teJars);
            if (found != null) return found;
        }
        return null;
    }

    private static File fromBibliotecaRoot(StringBuilder diag, Predicate<File> teJars) {
        diag.append("biblioteca.root=").append(System.getProperty("biblioteca.root")).append("\n");
        String root = System.getProperty("biblioteca.root");
        if (root == null || root.isBlank()) return null;
        File lib = new File(root, "lib");
        return recordAndReturn(lib, diag, teJars);
    }

    private static File fromUserDir(StringBuilder diag, Predicate<File> teJars) {
        diag.append("user.dir=").append(System.getProperty("user.dir")).append("\n");
        File lib = new File(System.getProperty("user.dir"), "lib");
        return recordAndReturn(lib, diag, teJars);
    }

    private static File fromUserDirSiblings(StringBuilder diag, Predicate<File> teJars) {
        File[] children = new File(System.getProperty("user.dir")).listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) {
            File lib = new File(child, "lib");
            if (recordAndReturn(lib, diag, teJars) != null) return lib;
        }
        return null;
    }

    private static File walkUp(StringBuilder diag, Predicate<File> teJars,
                               int maxLevels, String originLabel) {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < maxLevels; i++) {
            if (dir == null) return null;
            File lib = new File(dir, "lib");
            if (recordAndReturn(lib, diag, teJars) != null) return lib;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static File walkUpFromClassSource(StringBuilder diag, Predicate<File> teJars, int maxLevels) {
        java.net.URL loc;
        try {
            loc = JarLocator.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (RuntimeException se) {
            // SecurityException és una RuntimeException; algunes JVM
            // exposen la denegació d'accés a code-source com un simple
            // IllegalArgument o SecurityException. Les capturem totes
            // dues per mantenir el localitzador robust.
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
            if (recordAndReturn(lib, diag, teJars) != null) return lib;
            dir = dir.getParentFile();
        }
        return null;
    }

    private static File recordAndReturn(File lib, StringBuilder diag, Predicate<File> teJars) {
        boolean has = teJars.test(lib);
        String label = has ? " HAS_JARS"
            : lib.isDirectory() ? " no-jars" : " missing";
        diag.append(lib.getAbsolutePath()).append(label).append("\n");
        return has ? lib : null;
    }
}
