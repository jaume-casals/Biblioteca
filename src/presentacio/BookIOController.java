package presentacio;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.BookExporter;
import herramienta.BookImporter;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.OpenLibraryClient;
import interficie.BibliotecaWriter;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class BookIOController {

    private final Component parent;
    private final BibliotecaWriter cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public BookIOController(Component parent, BibliotecaWriter cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
    }

    public void importarCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        BookImporter.ImportResult r = BookImporter.importCSV(fc.getSelectedFile(), cd);
        if (r.errors() > 0 && r.imported() == 0 && !r.errorDetails().isEmpty()) {
            new DialogoError(new Exception(r.errorDetails())).showErrorMessage(); return;
        }
        String msg = r.imported() + " llibres importats.";
        if (r.errors() > 0) msg += "\n" + r.errors() + " errors:" + r.errorDetails();
        JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_title"),
            r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        onDataChanged.run();
    }

    public void importarCalibre() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(I18n.t("dlg_calibre_choose_title"));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        java.io.File dbFile = new java.io.File(fc.getSelectedFile(), "metadata.db");
        if (!dbFile.exists()) {
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_calibre_not_found"),
                I18n.t("dlg_calibre_choose_title"), JOptionPane.ERROR_MESSAGE); return;
        }
        String sqlite3 = BookImporter.findSqlite3();
        if (sqlite3 == null) {
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_calibre_no_sqlite3"),
                I18n.t("dlg_calibre_choose_title"), JOptionPane.ERROR_MESSAGE); return;
        }
        try {
            BookImporter.ImportResult r = BookImporter.importCalibre(dbFile, sqlite3, cd);
            onDataChanged.run();
            String msg = r.imported() + " " + I18n.t("lbl_imported_ok");
            if (r.skipped() > 0) msg += "\n" + r.skipped() + " " + I18n.t("lbl_skipped");
            if (r.errors() > 0) msg += "\n" + r.errors() + " " + I18n.t("lbl_errors");
            JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_calibre_choose_title"),
                r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
    }

    public void escanejarISBN() {
        String isbn = JOptionPane.showInputDialog(parent,
            I18n.t("dlg_scan_isbn_msg"),
            I18n.t("dlg_scan_isbn_title"), JOptionPane.QUESTION_MESSAGE);
        if (isbn == null || isbn.isBlank()) return;

        GuardarLlibresDialogo dialeg = new GuardarLlibresDialogo();
        new GuardarLlibresDialogoControl(dialeg, null, cd);
        dialeg.getTextISBN().setText(isbn.trim());

        String finalIsbn = isbn.trim();
        Thread fetchThread = new Thread(() -> {
            java.util.Map<String, String> meta = OpenLibraryClient.lookupByISBN(finalIsbn);
            if (Thread.interrupted()) return; // dialog closed while fetch was in progress
            SwingUtilities.invokeLater(() -> {
                if (!dialeg.isVisible()) return;
                if (meta.containsKey("error")) {
                    JOptionPane.showMessageDialog(dialeg,
                        I18n.t("dlg_network_error") + "\n" + meta.get("error"),
                        I18n.t("dlg_network_error_title"), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String title = meta.get("title");
                String autor = meta.get("autor");
                String any = meta.get("any");
                if (title != null && !title.isEmpty() && dialeg.getTextNom().getText().isEmpty())
                    dialeg.getTextNom().setText(title);
                if (autor != null && !autor.isEmpty() && dialeg.getTextAutor().getText().isEmpty())
                    dialeg.getTextAutor().setText(autor);
                if (any != null && !any.isEmpty() && dialeg.getTextAny().getText().isEmpty())
                    dialeg.getTextAny().setText(any);
            });
        });
        fetchThread.setDaemon(true);
        fetchThread.start();
        dialeg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { fetchThread.interrupt(); }
        });

        dialeg.setLocationRelativeTo(parent);
        dialeg.setVisible(true);
        onDataChanged.run();
    }

    public void exportarCSV() {
        java.io.File f = chooseExportFile("biblioteca.csv", "csv", "CSV files");
        if (f == null) return;
        try {
            BookExporter.exportCSV(f, currentBooks.get(), cd);
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                I18n.t("dlg_export_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
    }

    public void exportarJSON() {
        java.io.File f = chooseExportFile("biblioteca.json", "json", "JSON files");
        if (f == null) return;
        try {
            BookExporter.exportJSON(f, cd);
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                I18n.t("dlg_export_json_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
    }

    public void exportarHTML() {
        JCheckBox chkShelf = new JCheckBox(I18n.t("html_group_shelf_opt"), false);
        JCheckBox chkTable = new JCheckBox(I18n.t("html_table_view_opt"), false);
        int r = JOptionPane.showConfirmDialog(parent, new Object[]{chkShelf, chkTable},
            I18n.t("dlg_export_html_title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        java.io.File f = chooseExportFile("biblioteca.html", "html", "HTML files", "htm");
        if (f == null) return;
        try {
            BookExporter.exportHTML(f, currentBooks.get(), cd, chkShelf.isSelected(), chkTable.isSelected());
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_export_done", f.getAbsolutePath()),
                I18n.t("dlg_export_html_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
    }

    /** Shows a save JFileChooser filtered to the given extensions; ensures the first ext is appended. */
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

    public void exportarPDF() {
        BookExporter.exportPDF(currentBooks.get());
    }

    public void importarJSON() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        BookImporter.ImportResult r;
        try { r = BookImporter.importJSON(fc.getSelectedFile(), cd); }
        catch (Exception e) { new DialogoError(e).showErrorMessage(); return; }
        String msg = I18n.t("dlg_import_json_msg", r.imported());
        if (r.skipped() > 0) msg += "\n" + I18n.t("dlg_import_json_skipped", r.skipped());
        if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors());
        JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_json_title"),
            r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        onDataChanged.run();
    }

    public void fetchMissingCovers(JButton fetchBtn) {
        ArrayList<Llibre> all = cd.getAllLlibres();
        List<Llibre> missing = all.stream()
            .filter(l -> !l.hasBlob() && l.getImatgeBlob() == null)
            .collect(java.util.stream.Collectors.toList());
        if (missing.isEmpty()) {
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
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(8,
            r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        for (Llibre l : missing) {
            pool.submit(() -> {
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
                            pool.shutdown();
                            dlg.dispose();
                            if (fetchBtn != null) fetchBtn.setEnabled(true);
                            JOptionPane.showMessageDialog(parent,
                                I18n.t("dlg_fetch_portades_done", fetched.get(), total),
                                I18n.t("dlg_fetch_portades_done_title"), JOptionPane.INFORMATION_MESSAGE);
                            onDataChanged.run();
                        }
                    });
                }
            });
        }
    }

    public void backupBD() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("biblioteca_backup.sql"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        java.io.File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".sql")) f = new java.io.File(f.getPath() + ".sql");
        try {
            cd.backupToSQL(f);
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_backup_done", f.getAbsolutePath()),
                I18n.t("dlg_backup_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }

    public void restaurarBD(Runnable onSuccess) {
        int confirm = JOptionPane.showConfirmDialog(parent,
            I18n.t("dlg_confirm_restore_msg"),
            I18n.t("dlg_confirm_restore_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            cd.restoreFromSQL(fc.getSelectedFile());
            onSuccess.run();
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_restore_done"),
                I18n.t("dlg_restore_done_title"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            new DialogoError(e).showErrorMessage();
        }
    }
}
