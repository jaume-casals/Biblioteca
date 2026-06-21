package persistencia.row;

/** Fila de pertinença que uneix un llibre (isbn) amb un prestatge (llistaId), amb valoració i marca de llegit per prestatge. */
public record LlibreLlistaRow(long isbn, int llistaId, double valoracio, boolean llegit) implements RelationRow {}
