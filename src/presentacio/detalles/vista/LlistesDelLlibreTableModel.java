package presentacio.detalles.vista;

import domini.Llista;
import herramienta.I18n;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class LlistesDelLlibreTableModel extends AbstractTableModel {

    static final int COL_NOM    = 0;
    static final int COL_VAL    = 1;
    static final int COL_LLEGIT = 2;

    private final ArrayList<Llista> rows = new ArrayList<>();

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return 3; }

    @Override public String getColumnName(int c) {
        return switch (c) {
            case COL_NOM -> I18n.t("col_list");
            case COL_VAL -> I18n.t("col_rating");
            case COL_LLEGIT -> I18n.t("col_read");
            default -> null;
        };
    }

    @Override public boolean isCellEditable(int r, int c) {
        return c == COL_VAL || c == COL_LLEGIT;
    }

    @Override public Class<?> getColumnClass(int c) {
        return c == COL_LLEGIT ? Boolean.class : String.class;
    }

    @Override public Object getValueAt(int r, int c) {
        Llista l = rows.get(r);
        return switch (c) {
            case COL_NOM -> l.obtenirNom();
            case COL_VAL -> String.format(java.util.Locale.ROOT, "%.1f", l.obtenirValoracioLlibre());
            case COL_LLEGIT -> l.obtenirLlegitLlibre();
            default -> null;
        };
    }

    @Override public void setValueAt(Object val, int r, int c) {
        Llista l = rows.get(r);
        if (c == COL_VAL) {
            try { l.posarValoracioLlibre(Double.parseDouble(val.toString())); } catch (NumberFormatException ignored) {}
        } else if (c == COL_LLEGIT) {
            l.posarLlegitLlibre(Boolean.TRUE.equals(val));
        }
        fireTableCellUpdated(r, c);
    }

    public void addRow(Llista l) {
        rows.add(l);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void removeRow(int index) {
        rows.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void setRows(ArrayList<Llista> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public Llista obtenirLlistaAt(int index) {
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }
}