package persistencia;

/** Join row {@code llibre_autor}; used by {@link AutorDao} and backup export. */
public record LlibreAutorRow(long isbn, int autorId) implements RelationRow {}