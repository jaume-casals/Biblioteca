package presentacio.renderers;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;

import domini.Llibre;
import herramienta.DialogoError;
import herramienta.I18n;
import herramienta.UITheme;
import interficie.BookWriter;
import presentacio.MainFrameControl;

/**
 * Cell editor for the "llegit" column. Only persists the {@code llegit}
 * flag — all other fields on the loaded {@code Llibre} are stale
 * (the table model only loads the light row, not descripcio/notes).
 * The user is expected to open the details dialog (which calls
 * {@code loadHeavyFields}) before editing the read flag if they want
 * the other fields refreshed (per the tot.txt MEDIUM finding).
 */
public class LlegitCheckBoxEditor extends AbstractCellEditor implements TableCellEditor {
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
    private final BookWriter cd;
    private final Consumer<Llibre> onUpdated;

    public LlegitCheckBoxEditor(BookWriter cd, Consumer<Llibre> onUpdated) {
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
                        Llibre l = MainFrameControl.getInstance().getLlibreIsbn(Long.parseLong(isbn));
                        if (l == null) return;
                        l.setLlegit(newLlegit);
                        cd.updateLlibre(l);
                        SwingUtilities.invokeLater(() -> onUpdated.accept(l));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> new DialogoError(ex).showErrorMessage());
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