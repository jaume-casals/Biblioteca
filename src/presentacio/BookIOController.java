package presentacio;

import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import interficie.BibliotecaWriter;

public class BookIOController {

    private final ImportController importCtrl;
    private final ExportController exportCtrl;
    private final BackupController backupCtrl;

    public BookIOController(Component parent, BibliotecaWriter cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.importCtrl = new ImportController(parent, cd, currentBooks, onDataChanged);
        this.exportCtrl = new ExportController(parent, cd, currentBooks, onDataChanged);
        this.backupCtrl = new BackupController(parent, cd, currentBooks, onDataChanged);
    }

    public void importarCSV() { importCtrl.importarCSV(); }
    public void importarCalibre() { importCtrl.importarCalibre(); }
    public void importarJSON() { importCtrl.importarJSON(); }
    public void escanejarISBN() { importCtrl.escanejarISBN(); }

    public void exportarCSV() { exportCtrl.exportarCSV(); }
    public void exportarJSON() { exportCtrl.exportarJSON(); }
    public void exportarHTML() { exportCtrl.exportarHTML(); }
    public void exportarPDF() { exportCtrl.exportarPDF(); }
    public void fetchMissingCovers(JButton fetchBtn) { exportCtrl.fetchMissingCovers(fetchBtn); }

    public void backupBD() { backupCtrl.backupBD(); }
    public void restaurarBD(Runnable onSuccess) { backupCtrl.restaurarBD(onSuccess); }

    static java.io.File pickFile(Component parent, String title, String desc, String... exts) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, exts));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        return fc.getSelectedFile();
    }
}
