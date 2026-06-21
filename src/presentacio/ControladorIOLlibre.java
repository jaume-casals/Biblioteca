package presentacio;

import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;

import domini.Llibre;
import persistencia.contract.EscritorBiblioteca;

public class ControladorIOLlibre {

    private final ControladorImportacio importarCtrl;
    private final ControladorExportacio exportarCtrl;
    private final ControladorCopiaSeguretat copiaSegCtrl;

    public ControladorIOLlibre(Component parent, EscritorBiblioteca cd,
            Supplier<List<Llibre>> currentBooks, Runnable onDataChanged) {
        this.importarCtrl = new ControladorImportacio(parent, cd, currentBooks, onDataChanged);
        this.exportarCtrl = new ControladorExportacio(parent, cd, currentBooks, onDataChanged);
        this.copiaSegCtrl = new ControladorCopiaSeguretat(parent, cd, currentBooks, onDataChanged);
    }

    public void importarCSV() { importarCtrl.importarCSV(); }
    public void importarCalibre() { importarCtrl.importarCalibre(); }
    public void importarJSON() { importarCtrl.importarJSON(); }
    public void escanejarISBN() { importarCtrl.escanejarISBN(); }

    public void exportarCSV() { exportarCtrl.exportarCSV(); }
    public void exportarJSON() { exportarCtrl.exportarJSON(); }
    public void exportarHTML() { exportarCtrl.exportarHTML(); }
    public void exportarPDF() { exportarCtrl.exportarPDF(); }
    public void fetchMissingCovers(JButton fetchBtn) { exportarCtrl.fetchMissingCovers(fetchBtn); }

    public void copiaSegBD() { copiaSegCtrl.copiaSegBD(); }
    public void restaurarBD(Runnable onSuccess) { copiaSegCtrl.restaurarBD(onSuccess); }

    static java.io.File pickFile(Component parent, String title, String desc, String... exts) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, exts));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return null;
        return fc.getSelectedFile();
    }
}
