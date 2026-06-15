package domini;

import java.util.Comparator;
import java.util.Map;

/**
 * Pagination-aware sort specification, extracted from LlibreFilter so that
 * filter criteria stay pure data while sort is a separate pagination concern.
 *
 * <p>Single source of truth for both SQL fragment and in-memory comparator
 * per sortable column. The {@link #COLUMNS} map is the canonical registry —
 * adding a new column is a one-line edit (per the tot.txt LOW finding on
 * the parallel {@code SQL_COLS} / {@code COMPARATORS} maps).
 */
public final class SortSpec {
    public static final String COL_ISBN = "ISBN";
    public static final String COL_NOM = "nom";
    public static final String COL_ANY = "any";
    public static final String COL_VALORACIO = "valoracio";
    public static final String COL_PREU = "preu";

    /** Canonical registry: one entry per sortable column. */
    public record Column(String key, String sqlFragment, Comparator<Llibre> cmp) {}

    /** ISBN-ascending comparator; nulls sort first to match binarySearch behavior. */
    public static final Comparator<Llibre> ISBN_COMPARATOR = (a, b) -> {
        Long ia = a.getISBN(), ib = b.getISBN();
        if (ia == null && ib == null) return 0;
        if (ia == null) return -1;
        if (ib == null) return 1;
        return ia.compareTo(ib);
    };

    private static final Map<String, Column> COLUMNS = Map.of(
        COL_ISBN,      new Column(COL_ISBN,      "l.`ISBN`",      ISBN_COMPARATOR),
        COL_NOM,       new Column(COL_NOM,       "l.`nom`",       Comparator.comparing(l -> l.getNom() != null ? l.getNom() : "", String.CASE_INSENSITIVE_ORDER)),
        COL_ANY,       new Column(COL_ANY,       "l.`any`",       Comparator.comparing(l -> l.getAny() != null ? l.getAny() : 0)),
        COL_VALORACIO, new Column(COL_VALORACIO, "l.`valoracio`", Comparator.comparing(l -> l.getValoracio() != null ? l.getValoracio() : 0.0)),
        COL_PREU,      new Column(COL_PREU,      "l.`preu`",      Comparator.comparing(l -> l.getPreu() != null ? l.getPreu() : 0.0))
    );

    private final String column;
    private final boolean ascending;

    /**
     * Unknown / null column names are tolerated and silently coerced to
     * {@link #COL_ISBN}. The fallback lives here (not in the call site)
     * so the policy of "unknown → ISBN" is in one place. toSql() and
     * {@link #comparator(String)} both honour the fallback.
     */
    public SortSpec(String column, boolean ascending) {
        this.column = (column != null && COLUMNS.containsKey(column)) ? column : COL_ISBN;
        this.ascending = ascending;
    }

    public static SortSpec defaultAsc() { return new SortSpec(COL_ISBN, true); }

    public String column() { return column; }
    public boolean ascending() { return ascending; }

    /** SQL ORDER BY expression fragment, e.g. "l.`ISBN` ASC" */
    public String toSql() {
        Column c = COLUMNS.get(column);
        return (c != null ? c.sqlFragment() : "l.`ISBN`") + (ascending ? " ASC" : " DESC");
    }

    /**
     * In-memory comparator for the column. Falls back to
     * {@link #ISBN_COMPARATOR} for unknown columns so callers don't
     * have to repeat the null-coalesce.
     */
    public static Comparator<Llibre> comparator(String column) {
        Column c = COLUMNS.get(column);
        return c != null ? c.cmp() : ISBN_COMPARATOR;
    }

    /** Read-only view of all in-memory comparators keyed by SortSpec column name. */
    public static Map<String, Comparator<Llibre>> comparators() {
        java.util.Map<String, Comparator<Llibre>> out = new java.util.HashMap<>();
        for (Column c : COLUMNS.values()) out.put(c.key(), c.cmp());
        return out;
    }

    /** Read-only view of the canonical column registry. */
    public static Map<String, Column> columns() { return COLUMNS; }
}
