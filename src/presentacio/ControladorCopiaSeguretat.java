package presentacio;

import java.awt.Component;
import java.awt.Frame;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.DialegError;
import herramienta.I18n;
import interficie.AdministradorBiblioteca;

public class ControladorCopiaSeguretat {

    private final Component parent;
    private final AdministradorBiblioteca cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public ControladorCopiaSeguretat(Component parent, AdministradorBiblioteca cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
    }

    private static Frame windowFrame(Component parent) {
        java.awt.Window w = SwingUtilities.getWindowAncestor(parent);
        return w instanceof Frame f ? f : null;
    }

    public void copiaSegBD() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("biblioteca_backup.sql"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        java.io.File selectedFile = fc.getSelectedFile();
        if (!selectedFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".sql")) selectedFile = new java.io.File(selectedFile.getPath() + ".sql");
        final java.io.File f = selectedFile;
        DialegCarrega loading = new DialegCarrega(windowFrame(parent), I18n.t("dlg_backup_title"));
        loading.show();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                cd.copiaSegToSQL(f);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try { get(); JOptionPane.showMessageDialog(parent, I18n.t("dlg_backup_done", f.getAbsolutePath()),
                    I18n.t("dlg_backup_done_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }

    public void restaurarBD(Runnable onSuccess) {
        int confirm = JOptionPane.showConfirmDialog(parent,
            I18n.t("dlg_confirm_restore_msg"),
            I18n.t("dlg_confirm_restore_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        DialegCarrega loading = new DialegCarrega(windowFrame(parent), I18n.t("dlg_restore_title"));
        loading.show();
        final java.io.File selectedFile = fc.getSelectedFile();
        new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                cd.restaurarFromSQL(selectedFile);
                return null;
            }
            @Override protected void done() {
                loading.hide();
                try {
                    get();
                    SwingUtilities.invokeLater(onSuccess);
                    JOptionPane.showMessageDialog(parent,
                        I18n.t("dlg_restore_done_with_note", I18n.t("dlg_restore_done"), I18n.t("dlg_restore_done_cover_note")),
                        I18n.t("dlg_restore_done_title"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) { new DialegError(e).mostrarErrorMessage(); }
            }
        }.execute();
    }
}
