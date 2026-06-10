package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link LibDirResolver}.
 */
class LibDirResolverTest {

    private static String savedBibliotecaRoot;
    private static String savedUserDir;
    private static String savedClassPath;

    static {
        savedBibliotecaRoot = System.getProperty("biblioteca.root");
        savedUserDir = System.getProperty("user.dir");
        savedClassPath = System.getProperty("java.class.path");
    }

    @Test
    @DisplayName("resolve: returns null when no strategy matches and no property set")
    void resolveNullWhenNoMatch() {
        System.clearProperty("biblioteca.root");
        System.setProperty("user.dir", "/nonexistent/zzz/qqq/");
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isNull();
    }

    @Test
    @DisplayName("resolve: biblioteca.root + /lib is found")
    void resolveFromRoot(@TempDir File root) {
        File lib = new File(root, "lib");
        lib.mkdirs();
        System.setProperty("biblioteca.root", root.getAbsolutePath());
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isEqualTo(lib);
    }

    @Test
    @DisplayName("resolve: user.dir + /lib is found when biblioteca.root unset")
    void resolveFromUserDir(@TempDir File work) {
        File lib = new File(work, "lib");
        lib.mkdirs();
        System.clearProperty("biblioteca.root");
        System.setProperty("user.dir", work.getAbsolutePath());
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isEqualTo(lib);
    }

    @Test
    @DisplayName("resolve: biblioteca.root takes priority over user.dir")
    void resolveRootPriority(@TempDir File root, @TempDir File work) {
        File rootLib = new File(root, "lib"); rootLib.mkdirs();
        File workLib = new File(work, "lib"); workLib.mkdirs();
        System.setProperty("biblioteca.root", root.getAbsolutePath());
        System.setProperty("user.dir", work.getAbsolutePath());
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isEqualTo(rootLib);
    }

    @Test
    @DisplayName("resolve: null biblioteca.root falls through to user.dir")
    void nullRootFallsThrough(@TempDir File work) {
        File lib = new File(work, "lib"); lib.mkdirs();
        System.clearProperty("biblioteca.root");
        System.setProperty("user.dir", work.getAbsolutePath());
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isEqualTo(lib);
    }

    @Test
    @DisplayName("resolve: bogus biblioteca.root (no lib/ subdir) falls through to user.dir")
    void bogusRootFallsThrough(@TempDir File work, @TempDir File fakeRoot) {
        File lib = new File(work, "lib"); lib.mkdirs();
        // fakeRoot has no 'lib' subdir
        System.setProperty("biblioteca.root", fakeRoot.getAbsolutePath());
        System.setProperty("user.dir", work.getAbsolutePath());
        File f = LibDirResolver.resolve(new StringBuilder());
        assertThat(f).isEqualTo(lib);
    }

    @Test
    @DisplayName("resolve: diagnostic stringBuilder receives strategy info")
    void diagnosticOnSuccess(@TempDir File root) {
        File lib = new File(root, "lib"); lib.mkdirs();
        System.setProperty("biblioteca.root", root.getAbsolutePath());
        StringBuilder diag = new StringBuilder();
        File f = LibDirResolver.resolve(diag);
        assertThat(f).isEqualTo(lib);
        assertThat(diag.toString()).contains("biblioteca.root");
    }
}
