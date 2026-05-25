package presentacio;

import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import herramienta.DialogoError;
import herramienta.I18n;
import interficie.BibliotecaWriter;

public class BackupController {

    private final Component parent;
    private final BibliotecaWriter cd;
    private final Supplier<List<Llibre>> currentBooks;
    private final Runnable onDataChanged;

    public BackupController(Component parent, BibliotecaWriter cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.parent = parent;
        this.cd = cd;
        this.currentBooks = currentBooks;
        this.onDataChanged = onDataChanged;
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
