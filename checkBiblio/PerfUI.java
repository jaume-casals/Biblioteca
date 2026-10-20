package checkBiblio;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import presentacio.panells.PanelMostrarBiblioteca;

/**
 * Mesura de rendiment de la interfície.
 *
 * <p>Mesura quatre moments que l'usuari nota:
 * <ol>
 *   <li><b>Arrencada</b>: des del llançament del procés fins que la finestra
 *       principal apareix.</li>
 *   <li><b>Canvi de vista</b>: galeria ↔ taula.</li>
 *   <li><b>Redimensionament</b>: la finestra creix i encolleix.</li>
 *   <li><b>Memòria</b>: heap usada i temps de CPU acumulat.</li>
 * </ol>
 *
 * <p>La mesura es fa contra l'aplicació real i una base de dades H2 en
 * memòria: no toca cap dada persistent.
 */
public final class PerfUI {

    private PerfUI() {}

    public static void main(String[] args) throws Exception {
        long t0 = System.nanoTime();

        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:perfUi;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("biblioteca-perf");
            System.setProperty("user.home", tmp.toString());
        } catch (Exception ignored) {}

        Thread app = new Thread(() -> {
            try { main.Executable.main(new String[]{"--swing"}); }
            catch (Exception e) { System.err.println("APP ERROR: " + e); }
        }, "biblioteca-perf");
        app.setDaemon(true);
        app.start();

        JFrame frame = SuportTestUi.waitForMainFrame(30000);
        if (frame == null) {
            System.err.println("FATAL: la finestra principal no ha aparegut");
            System.exit(1);
        }
        double arrencadaMs = (System.nanoTime() - t0) / 1_000_000.0;
        SuportTestUi.settle();

        PanelMostrarBiblioteca panell = SuportTestUi.findComponent(frame, PanelMostrarBiblioteca.class);

        // ── Canvi de vista ──────────────────────────────────────────────────
        // Els botons es resolen a cada iteració: en la versió anterior el
        // mateix botó canviava d'etiqueta entre "Galeria" i "Taula", i
        // conservar la referència faria que la segona pulsació no fes res.
        List<Double> canvis = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AbstractButton galeria = SuportTestUi.findBtnIn(frame, "Galeria", "Galería", "Gallery");
            long a = System.nanoTime();
            if (galeria != null) SwingUtilities.invokeAndWait(galeria::doClick);
            long b = System.nanoTime();
            AbstractButton taula = SuportTestUi.findBtnIn(frame, "Taula", "Tabla", "Table");
            if (taula != null) SwingUtilities.invokeAndWait(taula::doClick);
            long c = System.nanoTime();
            canvis.add((b - a) / 1_000_000.0);
            canvis.add((c - b) / 1_000_000.0);
            SuportTestUi.settle();
        }

        // ── Redimensionament ────────────────────────────────────────────────
        long r0 = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            final int w = 1200 + i * 40;
            SwingUtilities.invokeAndWait(() -> frame.setSize(w, 800));
            SuportTestUi.settle();
        }
        double resizeMs = (System.nanoTime() - r0) / 1_000_000.0;

        // ── Cerca ────────────────────────────────────────────────────────────
        long s0 = System.nanoTime();
        if (panell != null) {
            javax.swing.JTextField cerca = panell.obtenirSearchBar();
            SwingUtilities.invokeAndWait(() -> cerca.setText("sombra"));
            SuportTestUi.settle();
        }
        double cercaMs = (System.nanoTime() - s0) / 1_000_000.0;

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long heapUsada = mem.getHeapMemoryUsage().getUsed();
        long cpuMs = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 1_000_000;

        System.out.println("=== Mesures de rendiment de la interfície ===");
        System.out.printf("arrencada_ms=%.1f%n", arrencadaMs);
        System.out.printf("canvi_vista_ms_media=%.1f%n", mitjana(canvis));
        double gal = 0, tab = 0;
        for (int i = 0; i < canvis.size(); i += 2) { gal += canvis.get(i); tab += canvis.get(i + 1); }
        int n = canvis.size() / 2;
        System.out.printf("anada_a_galeria_ms=%.1f%n", gal / n);
        System.out.printf("tornada_a_taula_ms=%.1f%n", tab / n);
        System.out.printf("canvi_vista_ms_max=%.1f%n", max(canvis));
        System.out.printf("redimensionament_ms_total=%.1f%n", resizeMs);
        System.out.printf("cerca_ms=%.1f%n", cercaMs);
        System.out.printf("heap_usat_mb=%.1f%n", heapUsada / (1024.0 * 1024.0));
        System.out.printf("cpu_thread_ms=%d%n", cpuMs);
        System.exit(0);
    }

    private static double mitjana(List<Double> v) {
        if (v.isEmpty()) return 0;
        double s = 0;
        for (double d : v) s += d;
        return s / v.size();
    }

    private static double max(List<Double> v) {
        double m = 0;
        for (double d : v) m = Math.max(m, d);
        return m;
    }
}
