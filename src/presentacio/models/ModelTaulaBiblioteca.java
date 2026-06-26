package presentacio.models;

import domini.Llibre;
import herramienta.config.Configuracio;
import herramienta.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;
import presentacio.controladors.ControladorTaula;



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

    private static final String[] COL_KEYS = {
        "col_cover", "col_isbn", "col_title", "col_author", "col_year",
        "col_rating", "col_price", "col_read", "col_progress", "col_details"
    };

    private static final List<Function<Llibre, Object>> COL_EXTRACTORS = List.of(
        l -> "",
        l -> l.obtenirISBN() != null ? String.valueOf(l.obtenirISBN()) : "",
        l -> l.obtenirDisplayNom(Configuracio.obtenirLang()),
        Llibre::obtenirAutor,
        Llibre::obtenirAny,
        Llibre::obtenirValoracio,
        Llibre::obtenirPreu,
        l -> Boolean.TRUE.equals(l.obtenirLlegit()) ? I18n.t("filter_read") : I18n.t("filter_unread"),
        l -> l.obtenirPaginesLlegides() + "/" + l.obtenirPagines(),
        l -> ""
    );

    private final List<Llibre> books = new ArrayList<>();

    @Override public int getRowCount() { return books.size(); }
    @Override public int getColumnCount() { return COL_TYPES.length; }

    @Override public String getColumnName(int column) {
        return column >= 0 && column < COL_KEYS.length ? I18n.t(COL_KEYS[column]) : "";
    }

    @Override public Class<?> getColumnClass(int column) {
        return column < COL_TYPES.length ? COL_TYPES[column] : Object.class;
    }

    @Override public boolean isCellEditable(int row, int column) { return column == COL_LLEGIT; }

    @Override public Object getValueAt(int row, int column) {
        if (row < 0 || row >= books.size() || column < 0 || column >= COL_EXTRACTORS.size()) return null;
        return COL_EXTRACTORS.get(column).apply(books.get(row));
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
    public List<Llibre> obtenirBooks() { return books; }
}

