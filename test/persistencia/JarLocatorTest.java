package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link JarLocator}.
 * Drives the strategy chain with synthetic system properties and a fake
 * hasJars predicate that recognises our fixture directories.
 */
class JarLocatorTest {

    private static String savedBibliotecaRoot;
    private static String savedUserDir;

    static {
        savedBibliotecaRoot = System.getProperty("biblioteca.root");
        savedUserDir = System.getProperty("user.dir");
    }

    private static void clearRoot() {
        System.clearProperty("biblioteca.root");
    }

    private static void setRoot(String path) {
        if (path == null) clearRoot(); else System.setProperty("biblioteca.root", path);
    }

    private static Predicate<File> alwaysNo() {
        return f -> false;
    }

    private static Predicate<File> hasJarNamed(String name) {
        return f -> f.isDirectory() && new File(f, name).exists();
    }

    @Test
    @DisplayName("locate: returns user.dir/lib fallback when no strategy matches")
    void locateFallsBack(@TempDir File tmp) {
        setRoot(null);
        System.setProperty("user.dir", tmp.getAbsolutePath());
        File result = JarLocator.locate(new StringBuilder(), alwaysNo());
        assertThat(result).isEqualTo(new File(tmp, "lib"));
    }

    @Test
    @DisplayName("locate: estrategia biblioteca.root wins when set and has jars")
    void bibliotecaRootStrategyWins(@TempDir File root) throws IOException {
        File lib = new File(root, "lib");
        lib.mkdirs();
        new File(lib, "h2-2.3.232.jar").createNewFile();
        setRoot(root.getAbsolutePath());
        File result = JarLocator.locate(new StringBuilder(), hasJarNamed("h2-2.3.232.jar"));
        assertThat(result).isEqualTo(lib);
    }

    @Test
    @DisplayName("locate: biblioteca.root ignored when it has no jars; falls through to user.dir")
    void rootWithNoJarsFallsThrough(@TempDir File root, @TempDir File work) throws IOException {
        File rootLib = new File(root, "lib");
        rootLib.mkdirs();
        new File(rootLib, "h2-2.3.232.jar").createNewFile();

        File userLib = new File(work, "lib");
        userLib.mkdirs();
        new File(userLib, "h2-2.3.232.jar").createNewFile();

        setRoot(root.getAbsolutePath());
        System.setProperty("user.dir", work.getAbsolutePath());

        // Only userLib has the jar our predicate accepts
        File result = JarLocator.locate(new StringBuilder(), hasJarNamed("h2-2.3.232.jar"));
        assertThat(result).isEqualTo(userLib);
    }

    @Test
    @DisplayName("locate: blank biblioteca.root is treated as null")
    void blankRootFallsThrough(@TempDir File work) throws IOException {
        File lib = new File(work, "lib");
        lib.mkdirs();
        new File(lib, "h2-2.3.232.jar").createNewFile();
        setRoot("   ");
        System.setProperty("user.dir", work.getAbsolutePath());
        File result = JarLocator.locate(new StringBuilder(), hasJarNamed("h2-2.3.232.jar"));
        assertThat(result).isEqualTo(lib);
    }

    @Test
    @DisplayName("locate: diagnostic stringBuilder receives one line per strategy attempt")
    void diagnosticRecordsAllStrategies(@TempDir File work) {
        setRoot(null);
        System.setProperty("user.dir", work.getAbsolutePath());
        StringBuilder diag = new StringBuilder();
        JarLocator.locate(diag, alwaysNo());
        // 5 strategies, each prefixed with [N]
        assertThat(diag.toString()).contains("[1]").contains("[2]").contains("[3]").contains("[4]").contains("[5]");
    }

    @Test
    @DisplayName("locate: user.dir strategy reads user.dir property")
    void userDirStrategyReadsProperty(@TempDir File work) throws IOException {
        File lib = new File(work, "lib");
        lib.mkdirs();
        new File(lib, "h2-2.3.232.jar").createNewFile();
        setRoot(null);
        System.setProperty("user.dir", work.getAbsolutePath());
        File result = JarLocator.locate(new StringBuilder(), hasJarNamed("h2-2.3.232.jar"));
        assertThat(result).isEqualTo(lib);
    }
}
