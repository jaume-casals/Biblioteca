package presentacio.dialegs;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.ExportadorLlibres;
import herramienta.text.ValidadorLlibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import javax.swing.SwingWorker;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the export-flow deadlock.
 *
 * <p>The original {@code ControladorExportacio.exportarCSV} called
 * {@code loading.show()} BEFORE {@code new SwingWorker<...>().execute()}.
 * A modal undecorated {@code JDialog} opened via {@code setVisible(true)}
 * from the EDT blocks the EDT in a nested event loop until the dialog is
 * disposed. The {@code .execute()} call lives on the line AFTER the
 * blocking {@code setVisible(true)}, so the SwingWorker is never started,
 * {@code doInBackground} never runs, {@code done} is never dispatched,
 * nothing ever calls {@code loading.hide()}, and the dialog sticks forever.
 * The user saw the loading dialog with the static "Exportació completada"
 * label (which is misleading — it is actually the loading dialog's label,
 * not the success message) and a frozen progress bar, with no file on disk.
 *
 * <p>Fix: start the worker first, then show the loading dialog. This test
 * replicates the corrected ordering with a bare modal dialog and asserts
 * the worker actually completes and writes the file.
 */
class DialegCarregaSwingWorkerTestTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:dialeg_carrega_sw;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    @DisplayName("export with worker.execute() before dialog.show(): file written, dialog disposed")
    void ordreCorrecteWorkerAbansShow() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965, "", 9.0, 19.99, true, "");
        cd.afegirLlibre(l);

        File tmp = File.createTempFile("deadlock_check_", ".csv");
        tmp.delete();
        assertThat(tmp.exists()).isFalse();

        DialegCarrega loading = new DialegCarrega(null, "Exportant…");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                ExportadorLlibres.exportarCSV(tmp, new ArrayList<>(cd.obtenirAllLlibres()), cd);
                return null;
            }
            @Override protected void done() {
                loading.hide();
            }
        };
        worker.execute();
        loading.show();

        assertTrue(tmp.exists(), "CSV must exist on disk after the worker completes");
        assertThat(tmp.length()).isGreaterThan(0L);
        String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertThat(content).contains("9780306406157").contains("Dune");
    }
}
