package persistencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Reading-session row for backup/export. Not wired in the Swing UI yet. */
public record LecturaRow(long isbn, LocalDate dataInici, LocalDate dataFi, int paginesLlegides) {

    /** ISO-8601 formatter for SQL serialization (DB stores DATE as 'YYYY-MM-DD'). */
    public static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Parse a DATE column ({@code YYYY-MM-DD}) into a {@code LocalDate}; null/empty → null. */
    public static LocalDate analitzarDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim(), ISO);
    }
}
