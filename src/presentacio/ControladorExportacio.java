package presentacio;

import java.awt.Component;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.ExportadorLlibres;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.ClientOpenLibrary;
import interficie.EscritorBiblioteca;

public class ControladorExportacio {

    /** La planificació de descàrrega de cobertes passa per
     *  {@link herramienta.CoverService#submitCoverFetch}, que separa les
     *  lectures d'OL (un sol fil, amb limitador) de les escriptures JDBC
     *  (multifil, dimensionades pel nombre de cobertes). El limitador
     *  de 300ms d'OL continua aplicant-se entre consumidors; la
     *  separació elimina la penalització de "escriptures bloquejades
     *  per les lectures" (segons el finding MEDIUM de tot.txt sobre
     *  CoverService). El guard de concurrència al voltant de
     *  {@link #fetchMissingCovers(JButton)} continua evitant el doble
     *  clic al botó. */
    private static final java.util.concurrent.atomic.AtomicBoolean COVER_FETCH_RUNNING =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    private final Component parent;
    private final EscritorBiblioteca cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    private static Frame windowFrame(Component parent) {
        java.awt.Window w = SwingUtilities.getWindowAncestor(parent);
        return w instanceof Frame f ? f : null;
    }

    public ControladorExportacio(Component parent, EscritorBiblioteca cd,
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
        DialegCarrega loading = new DialegCarrega(windowFrame(parent), I18n.t("dlg_export_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                ExportadorLlibres.exportarCSV(f, currentBooks.get(), cd);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }

    public void exportarJSON() {
        java.io.File chosen = chooseExportFile("biblioteca.json", "json", "JSON files");
        if (chosen == null) return;
        final java.io.File f = chosen;
        DialegCarrega loading = new DialegCarrega(windowFrame(parent), I18n.t("dlg_export_json_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                ExportadorLlibres.exportarJSON(f, cd);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_json_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
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
        DialegCarrega loading = new DialegCarrega(windowFrame(parent), I18n.t("dlg_export_html_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                ExportadorLlibres.exportarHTML(f, currentBooks.get(), cd, chkShelf.isSelected(), chkTable.isSelected());
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                    I18n.t("dlg_export_html_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }

    public void exportarPDF() {
        ExportadorLlibres.exportarPDF(currentBooks.get());
    }

    public void fetchMissingCovers(JButton fetchBtn) {
        if (!COVER_FETCH_RUNNING.compareAndSet(false, true)) return;
        new SwingWorker<List<Llibre>, Void>() {
            @Override protected List<Llibre> doInBackground() {
                ArrayList<Llibre> all = new ArrayList<>(cd.obtenirAllLlibres());
                return all.stream()
                    .filter(l -> !l.teBlob() && l.obtenirImatgeBlob() == null)
                    .collect(java.util.stream.Collectors.toList());
            }
            @Override protected void done() {
                if (isCancelled()) {
                    COVER_FETCH_RUNNING.set(false);
                    return;
                }
                List<Llibre> missing;
                try {
                    missing = get();
                } catch (Exception e) {
                    COVER_FETCH_RUNNING.set(false);
                    new DialegError(e).mostrarErrorMessage();
                    return;
                }
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
                java.util.concurrent.atomic.AtomicInteger missingCover = new java.util.concurrent.atomic.AtomicInteger(0);
                try {
                    for (Llibre l : missing) {
                        herramienta.ServeiCoberta.submitCoverFetch(cd, String.valueOf(l.obtenirISBN()), stored -> {
                            if (stored) fetched.incrementAndGet();
                            else missingCover.incrementAndGet();
                            int d = done.incrementAndGet();
                            SwingUtilities.invokeLater(() -> {
                                bar.setValue(d);
                                lbl.setText(I18n.t("dlg_fetch_portades_progress", d, total));
                                if (d >= total) {
                                    dlg.dispose();
                                    if (fetchBtn != null) fetchBtn.setEnabled(true);
                                    // Distingeix "descarregada" de "no tenia
                                    // coberta a OpenLibrary" — l'antiga cadena
                                    // deia "0 de 10", cosa que l'usuari va
                                    // malinterpretar com a error (segons el
                                    // finding MEDIUM de tot.txt).
                                    int noCover = missingCover.get();
                                    if (noCover == 0) {
                                        JOptionPane.showMessageDialog(parent,
                                            I18n.t("dlg_fetch_portades_done", fetched.get(), total),
                                            I18n.t("dlg_fetch_portades_done_title"), JOptionPane.INFORMATION_MESSAGE);
                                    } else {
                                        JOptionPane.showMessageDialog(parent,
                                            I18n.t("dlg_fetch_portades_done_partial", fetched.get(), total, noCover),
                                            I18n.t("dlg_fetch_portades_done_title"), JOptionPane.INFORMATION_MESSAGE);
                                    }
                                    onDataChanged.run();
                                    COVER_FETCH_RUNNING.set(false);
                                }
                            });
                        });
                    }
                } catch (RuntimeException ex) {
                    COVER_FETCH_RUNNING.set(false);
                    throw ex;
                }
            }
        }.execute();
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
        boolean teExt = false;
        for (String ext : allExts) if (f.getName().toLowerCase().endsWith("." + ext)) { teExt = true; break; }
        return teExt ? f : new java.io.File(f.getPath() + "." + primaryExt);
    }
}
