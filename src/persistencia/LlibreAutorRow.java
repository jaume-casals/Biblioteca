package persistencia;

/** Join row {@code llibre_autor}; used by {@link AutorDao}, {@link LibraryGraph}, backup. */
public record LlibreAutorRow(long isbn, int autorId) implements RelationRow {}