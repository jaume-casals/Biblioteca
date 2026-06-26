package domini;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/**
 * Especificació d'ordenació conscient de la paginació, extreta de LlibreFilter perquè
 * els criteris de filtre es mantinguin com a dades pures mentre l'ordenació és una
 * preocupació de paginació separada.
 *
 * <p>Única font de veritat tant per al fragment SQL com per al comparador en memòria
 * de cada columna ordenable. El mapa {@link #COLUMNS} és el registre canònic —
 * afegir una columna nova és un canvi d'una sola línia (segons el finding LOW de
 * tot.txt sobre els mapes paral·lels {@code SQL_COLS} / {@code COMPARATORS}).
 */
public final class EspecificacioOrdenacio {
    public static final String COL_ISBN = "ISBN";
    public static final String COL_NOM = "nom";
    public static final String COL_ANY = "any";
    public static final String COL_VALORACIO = "valoracio";
    public static final String COL_PREU = "preu";

    /** Registre canònic: una entrada per columna ordenable. */
    public record Columna(String key, String sqlFragment, Comparator<Llibre> cmp) {}

    /** Comparador ISBN ascendent; els nuls s'ordenen primer per coincidir amb el comportament de binarySearch. */
    public static final Comparator<Llibre> ISBN_COMPARATOR = (a, b) -> {
        Long ia = a.obtenirISBN(), ib = b.obtenirISBN();
        if (ia == null && ib == null) return 0;
        if (ia == null) return -1;
        if (ib == null) return 1;
        return ia.compareTo(ib);
    };

    private static final Map<String, Columna> COLUMNS = Map.of(
        COL_ISBN,      new Columna(COL_ISBN,      "l.`ISBN`",      ISBN_COMPARATOR),
        COL_NOM,       new Columna(COL_NOM,       "l.`nom`",       Comparator.comparing(l -> l.obtenirNom() != null ? l.obtenirNom() : "", String.CASE_INSENSITIVE_ORDER)),
        COL_ANY,       new Columna(COL_ANY,       "l.`any`",       Comparator.comparing(l -> l.obtenirAny() != null ? l.obtenirAny() : 0)),
        COL_VALORACIO, new Columna(COL_VALORACIO, "l.`valoracio`", Comparator.comparing(l -> l.obtenirValoracio() != null ? l.obtenirValoracio() : 0.0)),
        COL_PREU,      new Columna(COL_PREU,      "l.`preu`",      Comparator.comparing(l -> l.obtenirPreu() != null ? l.obtenirPreu() : 0.0))
    );

    private final String column;
    private final boolean ascending;

/**
 * Els noms de columna desconeguts o nuls es toleren i es transformen silenciosament a
 * {@link #COL_ISBN}. El recau aquí (no al punt de crida) perquè la política
 * "desconegut → ISBN" estigui en un sol lloc. toSql() i
 * {@link #comparator(String)} respecten tots dos el recau.
 */
    public EspecificacioOrdenacio(String column, boolean ascending) {
        this.column = (column != null && COLUMNS.containsKey(column)) ? column : COL_ISBN;
        this.ascending = ascending;
    }

    public static EspecificacioOrdenacio defaultAsc() { return new EspecificacioOrdenacio(COL_ISBN, true); }

    public String column() { return column; }
    public boolean ascending() { return ascending; }

    private static <T> T pickColumn(String key, Function<Columna, T> getter, T isbnDefault) {
        Columna c = COLUMNS.get(key);
        return c != null ? getter.apply(c) : isbnDefault;
    }

    /** Fragment d'expressió SQL ORDER BY, p.ex. "l.`ISBN` ASC" */
    public String toSql() {
        return pickColumn(column, Columna::sqlFragment, "l.`ISBN`") + (ascending ? " ASC" : " DESC");
    }

/**
 * Comparador en memòria per a la columna. Recau a
 * {@link #ISBN_COMPARATOR} per a columnes desconegudes perquè els consumidors
 * no hagin de repetir el coalesce de nuls. Una columna nul·la es tracta com
 * a desconeguda (mateixa coerció que aplica el constructor).
 */
    public static Comparator<Llibre> comparator(String column) {
        if (column == null) return ISBN_COMPARATOR;
        return pickColumn(column, Columna::cmp, ISBN_COMPARATOR);
    }

    /** Mapa de comparadors en memòria calculat un sol cop; es consumeix a {@link DelegatLlibre#SORT_BY} */
    private static final Map<String, Comparator<Llibre>> COMPARATORS = comparisadorsLocked();

    private static Map<String, Comparator<Llibre>> comparisadorsLocked() {
        java.util.Map<String, Comparator<Llibre>> out = new java.util.HashMap<>();
        for (Columna c : COLUMNS.values()) out.put(c.key(), c.cmp());
        return out;
    }

    /** Vista de només lectura de tots els comparadors en memòria indexats pel nom de columna de l'SortSpec. */
    public static Map<String, Comparator<Llibre>> comparators() { return COMPARATORS; }

    /** Vista de només lectura del registre canònic de columnes. */
    public static Map<String, Columna> columns() { return COLUMNS; }
}
