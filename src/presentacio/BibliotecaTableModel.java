package presentacio;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import domini.Llibre;
import herramienta.Config;
import herramienta.I18n;

/** Table model for the main library book table. */
public class BibliotecaTableModel extends AbstractTableModel {

    public static final int COL_COVER = 0;
    public static final int COL_ISBN = 1;
    public static final int COL_NOM = 2;
    public static final int COL_AUTOR = 3;
    public static final int COL_ANY = 4;
    public static final int COL_VALORACIO = 5;
    public static final int COL_PREU = 6;
    public static final int COL_LLEGIT = 7;
    public static final int COL_PROGRES = 8;
    public static final int COL_DETALLS = 9;

    private static final Class<?>[] COL_TYPES = {
        Object.class, String.class, String.class, String.class,
        Integer.class, Double.class, Double.class,
        String.class, String.class, Object.class
    };

    private final List<Llibre> books = new ArrayList<>();

    @Override public int getRowCount() { return books.size(); }
    @Override public int getColumnCount() { return COL_TYPES.length; }

    @Override public String getColumnName(int column) {
        return switch (column) {
            case COL_COVER -> I18n.t("col_cover");
            case COL_ISBN -> I18n.t("col_isbn");
            case COL_NOM -> I18n.t("col_title");
            case COL_AUTOR -> I18n.t("col_author");
            case COL_ANY -> I18n.t("col_year");
            case COL_VALORACIO -> I18n.t("col_rating");
            case COL_PREU -> I18n.t("col_price");
            case COL_LLEGIT -> I18n.t("col_read");
            case COL_PROGRES -> I18n.t("col_progress");
            case COL_DETALLS -> I18n.t("col_details");
            default -> "";
        };
    }

    @Override public Class<?> getColumnClass(int column) {
        return column < COL_TYPES.length ? COL_TYPES[column] : Object.class;
    }

    @Override public boolean isCellEditable(int row, int column) { return column == COL_LLEGIT; }

    @Override public Object getValueAt(int row, int column) {
        if (row < 0 || row >= books.size()) return null;
        Llibre l = books.get(row);
        return switch (column) {
            case COL_COVER -> "";
            case COL_ISBN -> l.getISBN() + "";
            case COL_NOM -> l.getDisplayNom(Config.getLang());
            case COL_AUTOR -> l.getAutor();
            case COL_ANY -> l.getAny();
            case COL_VALORACIO -> l.getValoracio();
            case COL_PREU -> l.getPreu();
            case COL_LLEGIT -> Boolean.TRUE.equals(l.getLlegit()) ? I18n.t("filter_read") : I18n.t("filter_unread");
            case COL_PROGRES -> l.getPagines() + "/" + l.getPaginesLlegides();
            case COL_DETALLS -> "";
            default -> null;
        };
    }

    public void setBooks(List<Llibre> llibres) {
        books.clear();
        if (llibres != null) books.addAll(llibres);
        fireTableDataChanged();
    }

    public Llibre getBookAt(int row) {
        return row >= 0 && row < books.size() ? books.get(row) : null;
    }

    /** Mutable backing list (package-private use by {@link TableController} only). */
    List<Llibre> getBooks() { return books; }

    private static Object[] rowToValues(Llibre l) {
        String estat = Boolean.TRUE.equals(l.getLlegit()) ? I18n.t("filter_read") : I18n.t("filter_unread");
        return new Object[] {
            "", l.getISBN() + "", l.getDisplayNom(Config.getLang()), l.getAutor(), l.getAny(),
            l.getValoracio(), l.getPreu(), estat, l.getPagines() + "/" + l.getPaginesLlegides(), ""
        };
    }
}
