package presentacio.models;

import domini.Llista;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model de taula per a dades de prestatgeria (id, nom, recompte, color).
 * Extret de GestioLlistesDialog per poder reutilitzar-lo al
 * diàleg d'estadístiques o a qualsevol vista futura de llistes de
 * prestatgeries.
 */
public class ModelTaulaLlista extends AbstractTableModel {
    private static final String[] COLS = { "ID", "Nom", "Recompte", "Color" };
    private final List<Llista> rows;
    private Map<Integer, Integer> counts;

    public ModelTaulaLlista(List<Llista> rows) {
        this.rows = new ArrayList<>(rows);
        this.counts = Map.of();
    }

    public void setRows(List<Llista> newRows) { rows.clear(); rows.addAll(newRows); fireTableDataChanged(); }

    public void posarCounts(Map<Integer, Integer> counts) { this.counts = counts; fireTableDataChanged(); }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }
    @Override public Object getValueAt(int r, int c) {
        Llista l = rows.get(r);
        return switch (c) {
            case 0 -> l.obtenirId();
            case 1 -> l.obtenirNom();
            case 2 -> counts.getOrDefault(l.obtenirId(), 0);
            case 3 -> l.obtenirColor();
            default -> null;
        };
    }
}
