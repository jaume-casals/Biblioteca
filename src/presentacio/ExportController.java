package presentacio;

import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.BookExporter;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.OpenLibraryClient;
import interficie.BibliotecaWriter;

public class ExportController {

    /** Reused cover-fetch pool — guard against concurrent invocations of
     *  {@link #fetchMissingCovers(JButton)} (the button is also disabled). */
    private static final java.util.concurrent.ExecutorService COVER_POOL =
        java.util.concurrent.Executors.newFixedThreadPool(8, new java.util.concurrent.ThreadFactory() {
            private final java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "cover-fetch-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    private static final java.util.concurrent.atomic.AtomicBoolean COVER_FETCH_RUNNING =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    private final Component parent;
    private final BibliotecaWriter cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public ExportController(Component parent, BibliotecaWriter cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
    }

    public void exportarCSV() {
        java.io.File chosen = chooseExportFile("biblioteca.csv", "csv", "CSV files");
        if (chosen == null) return;
        final java.io.File f = chosen;
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_export_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                BookExporter.exportCSV(f, currentBooks.get(), cd);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
    }

    public void exportarJSON() {
        java.io.File chosen = chooseExportFile("biblioteca.json", "json", "JSON files");
        if (chosen == null) return;
        final java.io.File f = chosen;
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_export_json_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                BookExporter.exportJSON(f, cd);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_json_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
    }

    public void exportarHTML() {
        JCheckBox chkShelf = new JCheckBox(I18n.t("html_group_shelf_opt"), false);
        JCheckBox chkTable = new JCheckBox(I18n.t("html_table_view_opt"), false);
        int r = JOptionPane.showConfirmDialog(parent, new Object[]{chkShelf, chkTable},
            I18n.t("dlg_export_html_title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        java.io.File chosen = chooseExportFile("biblioteca.html", "html", "HTML files", "htm");
        if (chosen == null) return;
        final java.io.File f = chosen;
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_export_html_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                BookExporter.exportHTML(f, currentBooks.get(), cd, chkShelf.isSelected(), chkTable.isSelected());
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_html_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
    }

    public void exportarPDF() {
        BookExporter.exportPDF(currentBooks.get());
    }

    public void fetchMissingCovers(JButton fetchBtn) {
        if (!COVER_FETCH_RUNNING.compareAndSet(false, true)) return;
        ArrayList<Llibre> all = new ArrayList<>(cd.getAllLlibres());
        List<Llibre> missing = all.stream()
            .filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
            .collect(java.util.stream.Collectors.toList());
        if (missing.isEmpty()) {
            COVER_FETCH_RUNNING.set(false);
            JOptionPane.showMessageDialog(parent,
                I18n.t("dlg_fetch_portades_all_done"), I18n.t("dlg_fetch_portades_title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int total = missing.size();
        JProgressBar bar = new JProgressBar(0, total);
        bar.setStringPainted(true);
        JLabel lbl = new JLabel(I18n.t("dlg_fetch_portades_progress", 0, total));
        JPanel p = new JPanel(new java.awt.BorderLayout(8, 8));
        p.add(lbl, java.awt.BorderLayout.NORTH);
        p.add(bar, java.awt.BorderLayout.CENTER);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_fetch_portades_title"),
            java.awt.Dialog.ModalityType.MODELESS);
        dlg.setContentPane(p);
        dlg.pack(); dlg.setSize(360, 90);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
        if (fetchBtn != null) fetchBtn.setEnabled(false);
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger fetched = new java.util.concurrent.atomic.AtomicInteger(0);
        try {
            for (Llibre l : missing) {
                COVER_POOL.submit(() -> {
                    try {
                        byte[] blob = OpenLibraryClient.fetchCoverByISBN(String.valueOf(l.getISBN()));
                        if (blob != null && blob.length > 0) {
                            cd.setLlibreBlob(l.getISBN(), blob);
                            fetched.incrementAndGet();
                        }
                    } catch (Exception ignored) {} finally {
                        int d = done.incrementAndGet();
                        SwingUtilities.invokeLater(() -> {
                            bar.setValue(d);
                            lbl.setText(I18n.t("dlg_fetch_portades_progress", d, total));
                            if (d >= total) {
                                dlg.dispose();
                                if (fetchBtn != null) fetchBtn.setEnabled(true);
                                JOptionPane.showMessageDialog(parent,
                                    I18n.t("dlg_fetch_portades_done", fetched.get(), total),
                                    I18n.t("dlg_fetch_portades_done_title"), JOptionPane.INFORMATION_MESSAGE);
                                onDataChanged.run();
                                COVER_FETCH_RUNNING.set(false);
                            }
                        });
                    }
                });
            }
        } catch (RuntimeException ex) {
            COVER_FETCH_RUNNING.set(false);
            throw ex;
        }
    }

    private java.io.File chooseExportFile(String defaultName, String primaryExt, String desc, String... extraExts) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(defaultName));
        String[] allExts = new String[1 + extraExts.length];
        allExts[0] = primaryExt;
        System.arraycopy(extraExts, 0, allExts, 1, extraExts.length);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, allExts));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        java.io.File f = fc.getSelectedFile();
        boolean hasExt = false;
        for (String ext : allExts) if (f.getName().toLowerCase().endsWith("." + ext)) { hasExt = true; break; }
        return hasExt ? f : new java.io.File(f.getPath() + "." + primaryExt);
    }
}
