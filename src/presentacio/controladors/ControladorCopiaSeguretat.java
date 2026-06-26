package presentacio.controladors;

import domini.Llibre;
import herramienta.i18n.I18n;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import persistencia.contract.AdministradorBiblioteca;
import presentacio.dialegs.DialegCarrega;


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

    public void copiaSegBD() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("biblioteca_backup.sql"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        java.io.File selectedFile = fc.getSelectedFile();
        if (!selectedFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".sql")) selectedFile = new java.io.File(selectedFile.getPath() + ".sql");
        final java.io.File f = selectedFile;
        DialegCarrega.runAsync(parent, I18n.t("dlg_backup_title"),
            () -> { cd.copiaSegToSQL(f); return null; },
            v -> JOptionPane.showMessageDialog(parent, I18n.t("dlg_backup_done", f.getAbsolutePath()),
                I18n.t("dlg_backup_done_title"), JOptionPane.INFORMATION_MESSAGE));
    }

    public void restaurarBD(Runnable onSuccess) {
        int confirm = JOptionPane.showConfirmDialog(parent,
            I18n.t("dlg_confirm_restore_msg"),
            I18n.t("dlg_confirm_restore_title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQL files", "sql"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        final java.io.File selectedFile = fc.getSelectedFile();
        DialegCarrega.runAsync(parent, I18n.t("dlg_restore_title"),
            () -> { cd.restaurarFromSQL(selectedFile); return null; },
            v -> {
                SwingUtilities.invokeLater(onSuccess);
                JOptionPane.showMessageDialog(parent,
                    I18n.t("dlg_restore_done_with_note", I18n.t("dlg_restore_done"), I18n.t("dlg_restore_done_cover_note")),
                    I18n.t("dlg_restore_done_title"), JOptionPane.INFORMATION_MESSAGE);
            });
    }
}
