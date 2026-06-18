package persistencia;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fila de prèstec. isbn és un long primitiu (FK sempre present). */
public record PrestecRow(long isbn, String nomPersona, LocalDate dataPrestec, boolean retornat) {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static PrestecRow fromStrings(long isbn, String nomPersona, String dataPrestecStr, boolean retornat) {
        if (dataPrestecStr == null || dataPrestecStr.isBlank()) return new PrestecRow(isbn, nomPersona, null, retornat);
        try {
            return new PrestecRow(isbn, nomPersona, LocalDate.parse(dataPrestecStr.trim(), ISO), retornat);
        } catch (java.time.format.DateTimeParseException e) {
            return new PrestecRow(isbn, nomPersona, null, retornat);
        }
    }

    public long overdueDays(LocalDate asOf, int graceDays) {
        if (dataPrestec == null) return -1L;
        long days = java.time.temporal.ChronoUnit.DAYS.between(dataPrestec, asOf) - graceDays;
        return Math.max(0, days);
    }

    public Map<String, Object> toDisplayMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("isbn", isbn);
        m.put("persona", nomPersona);
        m.put("dataPrestec", dataPrestec != null ? dataPrestec.format(DISPLAY) : null);
        m.put("retornat", retornat);
        return m;
    }
}
