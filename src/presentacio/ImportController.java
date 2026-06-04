package presentacio;

import java.awt.Component;
import java.awt.Frame;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.BookImporter;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.OpenLibraryClient;
import interficie.BibliotecaWriter;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class ImportController {

    private final Component parent;
    private final BibliotecaWriter cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public ImportController(Component parent, BibliotecaWriter cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
    }

    public void importarCSV() {
        java.io.File f = BookIOController.pickFile(parent, I18n.t("dlg_import_title"), "CSV files", "csv");
        if (f == null) return;
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_import_title"));
        loading.show();
        new SwingWorker<BookImporter.ImportResult, Void>() {
            @Override protected BookImporter.ImportResult doInBackground() {
                return BookImporter.importCSV(f, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    BookImporter.ImportResult r = get();
                    if (r.errors() > 0 && r.imported() == 0 && !r.errorDetails().isEmpty()) {
                        new DialogoError(new Exception(r.errorDetails())).showErrorMessage(); return;
                    }
                    String msg = I18n.t("dlg_import_json_msg", r.imported());
                    if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors()) + r.errorDetails();
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    onDataChanged.run();
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
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
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_calibre_choose_title"));
        loading.show();
        new SwingWorker<BookImporter.ImportResult, Void>() {
            @Override protected BookImporter.ImportResult doInBackground() throws Exception {
                return BookImporter.importCalibre(dbFile, sqlite3, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    BookImporter.ImportResult r = get();
                    onDataChanged.run();
                    String msg = r.imported() + " " + I18n.t("lbl_imported_ok");
                    if (r.skipped() > 0) msg += "\n" + r.skipped() + " " + I18n.t("lbl_skipped");
                    if (r.errors() > 0) msg += "\n" + r.errors() + " " + I18n.t("lbl_errors");
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_calibre_choose_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
    }

    public void importarJSON() {
        java.io.File f = BookIOController.pickFile(parent, I18n.t("dlg_import_json_title"), "JSON files", "json");
        if (f == null) return;
        LoadingDialog loading = new LoadingDialog((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_import_json_title"));
        loading.show();
        new SwingWorker<BookImporter.ImportResult, Void>() {
            @Override protected BookImporter.ImportResult doInBackground() throws Exception {
                return BookImporter.importJSON(f, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    BookImporter.ImportResult r = get();
                    String msg = I18n.t("dlg_import_json_msg", r.imported());
                    if (r.skipped() > 0) msg += "\n" + I18n.t("dlg_import_json_skipped", r.skipped());
                    if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors());
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_json_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    onDataChanged.run();
                } catch (Exception e) { new DialogoError(e).showErrorMessage(); }
            }
        }.execute();
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
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        dialeg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { cancelled.set(true); }
        });
        Thread fetchThread = new Thread(() -> {
            java.util.Map<String, String> meta = OpenLibraryClient.lookupByISBN(finalIsbn);
            if (cancelled.get()) return;
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

        dialeg.setLocationRelativeTo(parent);
        dialeg.setVisible(true);
        onDataChanged.run();
    }
}
