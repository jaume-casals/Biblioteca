package domini;

import java.util.Comparator;
import java.util.Map;

/**
 * Pagination-aware sort specification, extracted from LlibreFilter so that
 * filter criteria stay pure data while sort is a separate pagination concern.
 *
 * <p>Owns the single source of truth for both SQL fragment and in-memory
 * comparator per sortable column. The facade's {@code SORT_BY} map is
 * derived from {@link #comparators()} so adding a new column requires
 * editing only this class.
 */
public final class SortSpec {
    public static final String COL_ISBN = "ISBN";
    public static final String COL_NOM = "nom";
    public static final String COL_ANY = "any";
    public static final String COL_VALORACIO = "valoracio";
    public static final String COL_PREU = "preu";

    private static final Map<String, String> SQL_COLS = Map.of(
        COL_ISBN, "l.`ISBN`",
        COL_NOM, "l.`nom`",
        COL_ANY, "l.`any`",
        COL_VALORACIO, "l.`valoracio`",
        COL_PREU, "l.`preu`"
    );

    /** ISBN-ascending comparator; nulls sort first to match binarySearch behavior. */
    public static final Comparator<Llibre> ISBN_COMPARATOR = (a, b) -> {
        Long ia = a.getISBN(), ib = b.getISBN();
        if (ia == null && ib == null) return 0;
        if (ia == null) return -1;
        if (ib == null) return 1;
        return ia.compareTo(ib);
    };

    private static final Map<String, Comparator<Llibre>> COMPARATORS = Map.of(
        COL_ISBN,      ISBN_COMPARATOR,
        COL_NOM,       Comparator.comparing(l -> l.getNom() != null ? l.getNom() : "", String.CASE_INSENSITIVE_ORDER),
        COL_ANY,       Comparator.comparing(l -> l.getAny() != null ? l.getAny() : 0),
        COL_VALORACIO, Comparator.comparing(l -> l.getValoracio() != null ? l.getValoracio() : 0.0),
        COL_PREU,      Comparator.comparing(l -> l.getPreu() != null ? l.getPreu() : 0.0)
    );

    private final String column;
    private final boolean ascending;

    /**
     * Unknown / null column names are tolerated and silently coerced to
     * {@link #COL_ISBN}. The fallback lives here (not in the call site)
     * so the policy of "unknown → ISBN" is in one place — see the
     * tot.txt MEDIUM finding on this class. ToSql() and
     * {@link #comparator(String)} both honour the fallback.
     */
    public SortSpec(String column, boolean ascending) {
        this.column = (column != null && SQL_COLS.containsKey(column)) ? column : COL_ISBN;
        this.ascending = ascending;
    }

    public static SortSpec defaultAsc() { return new SortSpec(COL_ISBN, true); }

    public String column() { return column; }
    public boolean ascending() { return ascending; }

    /** SQL ORDER BY expression fragment, e.g. "l.`ISBN` ASC" */
    public String toSql() {
        return SQL_COLS.getOrDefault(column, "l.`ISBN`") + (ascending ? " ASC" : " DESC");
    }

    /** In-memory comparator for the column, or null if column is unknown. */
    public static Comparator<Llibre> comparator(String column) {
        return COMPARATORS.get(column);
    }

    /** Read-only view of all in-memory comparators keyed by SortSpec column name. */
    public static Map<String, Comparator<Llibre>> comparators() {
        return COMPARATORS;
    }
}
