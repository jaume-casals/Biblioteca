package domini;

/**
 * Sort-only carrier extracted from {@link LlibreFilter}; lets callers that only need to
 * change sort order send a focused value instead of a 17-field filter blob.
 *
 * @param column DB column name (validated by LlibreDao whitelist) or null for default ISBN.
 * @param asc    true for ascending, false for descending.
 */
public record SortSpec(String column, boolean asc) {
    public static SortSpec defaultIsbn() { return new SortSpec(null, true); }

    /** Apply this sort to a (possibly empty) filter, returning the same filter for chaining. */
    public LlibreFilter applyTo(LlibreFilter f) {
        f.sortColumn = column;
        f.sortAsc = asc;
        return f;
    }
}
