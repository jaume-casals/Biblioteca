package persistencia.row;

import java.time.LocalDate;

/** Fila de sessió de lectura per a còpia/exportació. Encara no connectada a la IU Swing. */
public record LecturaRow(long isbn, LocalDate dataInici, LocalDate dataFi, int paginesLlegides) {

    /** Formatejador ISO-8601 per a la serialització SQL (la BD emmagatzema DATE com a 'YYYY-MM-DD'). */
    public static final java.time.format.DateTimeFormatter ISO = RowDates.ISO;

    /** Analitza una columna DATE ({@code YYYY-MM-DD}) a {@code LocalDate}; null/buit → null. */
    public static LocalDate analitzarDateOrNull(String s) {
        return RowDates.parseOrNull(s);
    }
}
