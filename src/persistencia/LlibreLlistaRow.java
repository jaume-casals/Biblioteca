package persistencia;

/** Membership row joining a book (isbn) to a shelf (llistaId), with per-shelf rating and read flag. */
public record LlibreLlistaRow(long isbn, int llistaId, double valoracio, boolean llegit) implements RelationRow {}
