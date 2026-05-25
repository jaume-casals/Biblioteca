package persistencia;

/**
 * Marker for book-relation row records: {@link LlibreLlistaRow}, {@link LlibreTagRow},
 * {@link LlibreAutorRow}. Permits cross-cutting handlers (export/import, audit dumps)
 * to dispatch generically.
 */
public interface RelationRow {
    long isbn();
}
