package persistencia.row;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Formatadors i parsing de dates compartits entre files de persistència. */
public final class RowDates {

    private RowDates() {}

    public static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parseOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
