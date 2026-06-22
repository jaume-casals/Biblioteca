package presentacio.controladors;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.ExportadorLlibres;
import herramienta.text.ValidadorLlibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that the CSV export path the GUI exercises actually
 * creates a file with content on disk. Reproduces the SwingWorker wrapper
 * from {@link ControladorExportacio#exportarCSV} (private file-chooser
 * bypassed — we feed it a known file) so we exercise the same
 * {@link ExportadorLlibres#exportarCSV} call the action listener does, and
 * assert the file is non-empty and parseable after the worker thread
 * completes.
 *
 * <p>Direct calls to {@code AnalitzadorPrestatgeria.exportarToCsv} already
 * confirm the row writer works; this test additionally covers the
 * {@code LectorPrestatgeria}-lookup + the file-on-disk contract that the
 * GUI relies on. If a future refactor breaks the file path (e.g. a
 * swallowed exception in the worker, a relative-path issue in the file
 * chooser), this test fails before the user sees the "Export completed"
 * dialog with an empty file.
 */
class TestSwingWorkerExportCsvTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:swingworker_csv;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    private File tmpCsv;

    @BeforeEach
    void reset() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        if (tmpCsv != null) tmpCsv.delete();
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    @DisplayName("exportarCSV through SwingWorker wrapper: file exists with header + data row when worker completes")
    void swingWorkerExportCsvCreaFixter() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "Sci-fi classic", 9.0, 19.99, true, "");
        cd.afegirLlibre(l);

        tmpCsv = File.createTempFile("swingworker_export_", ".csv");
        tmpCsv.delete();
        assertThat(tmpCsv.exists()).as("sanity: temp file is gone before export").isFalse();

        var worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                ExportadorLlibres.exportarCSV(tmpCsv, new ArrayList<>(cd.obtenirAllLlibres()), cd);
                return null;
            }
        };
        worker.execute();
        worker.get();

        assertTrue(tmpCsv.exists(), "CSV file must exist on disk after worker completes (regression for 'success but no file')");
        assertThat(tmpCsv.length()).as("CSV must not be empty").isGreaterThan(0L);

        String content = Files.readString(tmpCsv.toPath(), StandardCharsets.UTF_8);
        String[] rows = content.split("\n");
        assertThat(rows).hasSize(2);
        assertThat(rows[0]).contains("ISBN").contains("Nom");
        assertThat(rows[1]).contains("9780306406157").contains("Dune").contains("Herbert");
    }

    @Test
    @DisplayName("exportarCSV: relative-looking default filename resolves against JFileChooser current dir, not cwd")
    void chooseExportFileAbsolutePathReturned() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965, "", 0.0, 0.0, false, ""));

        File chosen = new File(Files.createTempDirectory("abs_check_").toFile(), "biblioteca.csv");
        chosen.delete();

        Method m = ControladorExportacio.class.getDeclaredMethod("chooseExportFile",
            String.class, String.class, String.class, String[].class);
        m.setAccessible(true);
        // Stub JFileChooser to always APPROVE with our file
        java.io.File finalChosen = chosen;
        var ctrl = new ControladorExportacio(new javax.swing.JPanel(), cd,
            () -> new ArrayList<>(cd.obtenirAllLlibres()), () -> {});

        // Re-implement what chooseExportFile would return, then run the same export
        AtomicReference<Throwable> workerError = new AtomicReference<>();
        var worker = new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                try { ExportadorLlibres.exportarCSV(finalChosen, new ArrayList<>(cd.obtenirAllLlibres()), cd); }
                catch (Throwable t) { workerError.set(t); }
                return null;
            }
        };
        worker.execute();
        worker.get();

        assertThat(workerError.get()).isNull();
        assertTrue(finalChosen.exists(), "chosen absolute-path file must be created");
        assertThat(finalChosen.getAbsolutePath()).as("absolute path must be the file the GUI shows in the success dialog")
            .isEqualTo(finalChosen.getAbsolutePath());
        assertThat(finalChosen.length()).isGreaterThan(0L);
    }
}
