package persistencia.row;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fila de prèstec. isbn és un long primitiu (FK sempre present). */
public record PrestecRow(long isbn, String nomPersona, LocalDate dataPrestec, boolean retornat) {

    public static PrestecRow fromStrings(long isbn, String nomPersona, String dataPrestecStr, boolean retornat) {
        return new PrestecRow(isbn, nomPersona, RowDates.parseOrNull(dataPrestecStr), retornat);
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
        m.put("dataPrestec", dataPrestec != null ? dataPrestec.format(RowDates.DISPLAY) : null);
        m.put("retornat", retornat);
        return m;
    }
}
