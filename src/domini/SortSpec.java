package domini;

import java.util.Map;

/**
 * Pagination-aware sort specification, extracted from LlibreFilter so that
 * filter criteria stay pure data while sort is a separate pagination concern.
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

    private final String column;
    private final boolean ascending;

    public SortSpec(String column, boolean ascending) {
        if (column == null || !SQL_COLS.containsKey(column))
            throw new IllegalArgumentException("Unknown sort column: " + column
                + " (valid: " + SQL_COLS.keySet() + ")");
        this.column = column;
        this.ascending = ascending;
    }

    public static SortSpec defaultAsc() { return new SortSpec(COL_ISBN, true); }

    public String column() { return column; }
    public boolean ascending() { return ascending; }

    /** SQL ORDER BY expression fragment, e.g. "l.`ISBN` ASC" */
    public String toSql() {
        return SQL_COLS.getOrDefault(column, "l.`ISBN`") + (ascending ? " ASC" : " DESC");
    }
}
