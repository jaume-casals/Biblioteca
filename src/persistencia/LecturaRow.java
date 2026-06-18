package persistencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Fila de sessió de lectura per a còpia/exportació. Encara no connectada a la IU Swing. */
public record LecturaRow(long isbn, LocalDate dataInici, LocalDate dataFi, int paginesLlegides) {

    /** Formatejador ISO-8601 per a la serialització SQL (la BD emmagatzema DATE com a 'YYYY-MM-DD'). */
    public static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Analitza una columna DATE ({@code YYYY-MM-DD}) a {@code LocalDate}; null/buit → null. */
    public static LocalDate analitzarDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.trim(), ISO);
    }
}
