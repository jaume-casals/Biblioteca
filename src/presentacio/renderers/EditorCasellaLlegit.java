package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.ui.DialegError;
import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import persistencia.contract.EscritorLlibre;
import presentacio.controladors.ControladorMarcPrincipal;

/**
 * Editor de cel·la per a la columna "llegit". Només persisteix el flag
 * {@code llegit} — tots els altres camps del {@code Llibre} carregat
 * estan obsolets (el model de taula només carrega la fila lleugera, no
 * pas descripcio/notes). S'espera que l'usuari obri el diàleg de detalls
 * (que crida {@code loadHeavyFields}) abans d'editar el flag de lectura
 * si vol que els altres camps es refresquin (segons la troballa MEDIUM
 * de tot.txt).
 */
public class EditorCasellaLlegit extends AbstractCellEditor implements TableCellEditor {
    private static final int COLUMNA_ISBN = 1;
    private static final java.util.concurrent.ExecutorService LLEGIT_EXEC =
        java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "llegit-update");
            t.setDaemon(true);
            return t;
        });
    static { main.ShutdownHooks.register(() -> LLEGIT_EXEC.shutdownNow()); }
    private final JCheckBox cb = new JCheckBox();
    private int editingRow = -1;
    private JTable editingTable = null;
    private final EscritorLlibre cd;
    private final Consumer<Llibre> onUpdated;

    public EditorCasellaLlegit(EscritorLlibre cd, Consumer<Llibre> onUpdated) {
        this.cd = cd;
        this.onUpdated = onUpdated;
        cb.setHorizontalAlignment(JCheckBox.CENTER);
        cb.setOpaque(true);
        cb.addActionListener(e -> {
            boolean newLlegit = cb.isSelected();
            int row = editingRow;
            JTable tbl = editingTable;
            String isbnStr = tbl != null && row >= 0 && row < tbl.getRowCount()
                ? (String) tbl.getValueAt(row, COLUMNA_ISBN) : null;
            fireEditingStopped();
            if (isbnStr != null) {
                String isbn = isbnStr;
                LLEGIT_EXEC.submit(() -> {
                    try {
                        Llibre l = ControladorMarcPrincipal.getInstance().obtenirLlibreIsbn(Long.parseLong(isbn));
                        if (l == null) return;
                        l.posarLlegit(newLlegit);
                        cd.actualitzarLlibre(l);
                        SwingUtilities.invokeLater(() -> onUpdated.accept(l));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> new DialegError(ex).mostrarErrorMessage());
                    }
                });
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int col) {
        editingRow = row;
        editingTable = table;
        cb.setSelected(I18n.t("filter_read").equals(value));
        cb.setBackground(UITheme.palette().accent());
        cb.setForeground(Color.WHITE);
        return cb;
    }

    @Override
    public Object getCellEditorValue() {
        return cb.isSelected() ? I18n.t("filter_read") : I18n.t("filter_unread");
    }
}