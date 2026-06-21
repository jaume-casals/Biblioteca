package persistencia.row;

/** Fila de pertinença que uneix un llibre (isbn) amb una etiqueta (tagId). */
public record LlibreTagRow(long isbn, int tagId) implements RelationRow {}
