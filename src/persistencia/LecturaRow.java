package persistencia;

/** Reading-session row for backup/export. Not wired in the Swing UI yet. */
public record LecturaRow(long isbn, String dataInici, String dataFi, int paginesLlegides) {}
