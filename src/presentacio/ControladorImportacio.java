package presentacio;

import java.awt.Component;
import java.awt.Frame;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.ImportadorLlibres;
import herramienta.DialegError;
import herramienta.I18n;
import herramienta.ClientOpenLibrary;
import interficie.EscritorBiblioteca;
import presentacio.detalles.control.ControladorDialegDesarLlibres;
import presentacio.detalles.vista.DialegDesarLlibres;

public class ControladorImportacio {

    private final Component parent;
    private final EscritorBiblioteca cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public ControladorImportacio(Component parent, EscritorBiblioteca cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
    }

    public void importarCSV() {
        java.io.File f = ControladorIOLlibre.pickFile(parent, I18n.t("dlg_import_title"), "CSV files", "csv");
        if (f == null) return;
        DialegCarrega loading = new DialegCarrega((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_import_title"));
        loading.show();
        new SwingWorker<ImportadorLlibres.ResultatImportacio, Void>() {
            @Override protected ImportadorLlibres.ResultatImportacio doInBackground() {
                return ImportadorLlibres.importarCSV(f, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    ImportadorLlibres.ResultatImportacio r = get();
                    if (r.errors() > 0 && r.imported() == 0 && !r.errorDetails().isEmpty()) {
                        new DialegError(new Exception(String.join("\n", r.errorDetails()))).mostrarErrorMessage(); return;
                    }
                    String msg = I18n.t("dlg_import_json_msg", r.imported());
                    if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors()) + String.join("\n", r.errorDetails());
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    onDataChanged.run();
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
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
        String sqlite3 = ImportadorLlibres.cercarSqlite3();
        if (sqlite3 == null) {
            JOptionPane.showMessageDialog(parent, I18n.t("dlg_calibre_no_sqlite3"),
                I18n.t("dlg_calibre_choose_title"), JOptionPane.ERROR_MESSAGE); return;
        }
        DialegCarrega loading = new DialegCarrega((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_calibre_choose_title"));
        loading.show();
        new SwingWorker<ImportadorLlibres.ResultatImportacio, Void>() {
            @Override protected ImportadorLlibres.ResultatImportacio doInBackground() throws Exception {
                return ImportadorLlibres.importarCalibre(dbFile, sqlite3, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    ImportadorLlibres.ResultatImportacio r = get();
                    onDataChanged.run();
                    String msg = r.imported() + " " + I18n.t("lbl_imported_ok");
                    if (r.skipped() > 0) msg += "\n" + r.skipped() + " " + I18n.t("lbl_skipped");
                    if (r.errors() > 0) msg += "\n" + r.errors() + " " + I18n.t("lbl_errors");
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_calibre_choose_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }

    public void importarJSON() {
        java.io.File f = ControladorIOLlibre.pickFile(parent, I18n.t("dlg_import_json_title"), "JSON files", "json");
        if (f == null) return;
        DialegCarrega loading = new DialegCarrega((Frame) SwingUtilities.getWindowAncestor(parent), I18n.t("dlg_import_json_title"));
        loading.show();
        new SwingWorker<ImportadorLlibres.ResultatImportacio, Void>() {
            @Override protected ImportadorLlibres.ResultatImportacio doInBackground() throws Exception {
                return ImportadorLlibres.importarJSON(f, cd);
            }
            @Override protected void done() {
                loading.hide();
                try {
                    ImportadorLlibres.ResultatImportacio r = get();
                    String msg = I18n.t("dlg_import_json_msg", r.imported());
                    if (r.skipped() > 0) msg += "\n" + I18n.t("dlg_import_json_skipped", r.skipped());
                    if (r.errors() > 0) msg += "\n" + I18n.t("dlg_import_json_errors", r.errors());
                    JOptionPane.showMessageDialog(parent, msg, I18n.t("dlg_import_json_title"),
                        r.errors() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    onDataChanged.run();
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }

    public void escanejarISBN() {
        String isbn = JOptionPane.showInputDialog(parent,
            I18n.t("dlg_scan_isbn_msg"),
            I18n.t("dlg_scan_isbn_title"), JOptionPane.QUESTION_MESSAGE);
        if (isbn == null) return;
        isbn = isbn.trim();
        if (isbn.isEmpty()) return;

        // Comprovació prèvia de duplicats abans d'obrir el diàleg perquè
        // l'usuari rebi un missatge immediat i específic del camp en
        // lloc d'un error genèric d'"addLlibre" després d'haver escrit
        // totes les metadades (segons el finding MEDIUM de tot.txt).
        // L'usuari encara pot confirmar i forçar l'obertura del diàleg
        // fent clic a "Sí".
        long parsedIsbn;
        try { parsedIsbn = Long.parseLong(isbn); }
        catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(parent, I18n.t("val_isbn_invalid"),
                I18n.t("dlg_duplicate_title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cd.existsLlibre(parsedIsbn)) {
            int ans = JOptionPane.showConfirmDialog(parent,
                I18n.t("dlg_isbn_exists", parsedIsbn),
                I18n.t("dlg_duplicate_title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
        }

        DialegDesarLlibres dialeg = new DialegDesarLlibres();
        new ControladorDialegDesarLlibres(dialeg, null, cd);
        dialeg.obtenirTextISBN().setText(isbn);

        final String finalIsbn = isbn;
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        dialeg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { cancelled.set(true); }
        });
        Thread fetchThread = new Thread(() -> {
            java.util.Map<String, String> meta = ClientOpenLibrary.lookupByISBN(finalIsbn);
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
                if (title != null && !title.isEmpty() && dialeg.obtenirTextNom().getText().isEmpty())
                    dialeg.obtenirTextNom().setText(title);
                if (autor != null && !autor.isEmpty() && dialeg.obtenirTextAutor().getText().isEmpty())
                    dialeg.obtenirTextAutor().setText(autor);
                if (any != null && !any.isEmpty() && dialeg.obtenirTextAny().getText().isEmpty())
                    dialeg.obtenirTextAny().setText(any);
            });
        });
        fetchThread.setName("isbn-lookup");
        fetchThread.setDaemon(true);
        fetchThread.start();

        dialeg.setLocationRelativeTo(parent);
        dialeg.setVisible(true);
        onDataChanged.run();
    }
}
