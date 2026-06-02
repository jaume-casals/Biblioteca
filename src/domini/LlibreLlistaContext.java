package domini;

/**
 * Per-book context for a shelf membership: which shelf, which book, and the
 * shelf-local values (rating, read flag) that only make sense in the context
 * of that specific (book, shelf) pair. Use this instead of putting the
 * per-book fields on {@link Llista} directly — the shelf itself has no
 * rating, only the (book, shelf) join does.
 */
public record LlibreLlistaContext(
    int isbn,
    int llistaId,
    String nom,
    int ordre,
    String color,
    Double valoracio,
    Boolean llegit
) {
    public static LlibreLlistaContext of(int isbn, int llistaId, String nom, int ordre, String color,
                                         Double valoracio, Boolean llegit) {
        return new LlibreLlistaContext(isbn, llistaId, nom, ordre, color, valoracio, llegit);
    }
}
