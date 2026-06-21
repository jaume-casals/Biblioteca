package presentacio;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import domini.Llibre;
import herramienta.config.Configuracio;
import herramienta.i18n.I18n;

/** Model de taula per a la taula principal de llibres de la biblioteca. */
public class ModelTaulaBiblioteca extends AbstractTableModel {

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
            case COL_ISBN -> l.obtenirISBN() != null ? String.valueOf(l.obtenirISBN()) : "";
            case COL_NOM -> l.obtenirDisplayNom(Configuracio.obtenirLang());
            case COL_AUTOR -> l.obtenirAutor();
            case COL_ANY -> l.obtenirAny();
            case COL_VALORACIO -> l.obtenirValoracio();
            case COL_PREU -> l.obtenirPreu();
            case COL_LLEGIT -> Boolean.TRUE.equals(l.obtenirLlegit()) ? I18n.t("filter_read") : I18n.t("filter_unread");
            case COL_PROGRES -> l.obtenirPaginesLlegides() + "/" + l.obtenirPagines();
            case COL_DETALLS -> "";
            default -> null;
        };
    }

    public void posarBooks(List<Llibre> llibres) {
        books.clear();
        if (llibres != null) books.addAll(llibres);
        fireTableDataChanged();
    }

    public Llibre obtenirBookAt(int row) {
        return row >= 0 && row < books.size() ? books.get(row) : null;
    }

    /** Llista de suport mutable (ús package-private només per {@link ControladorTaula}). */
    List<Llibre> obtenirBooks() { return books; }
}
