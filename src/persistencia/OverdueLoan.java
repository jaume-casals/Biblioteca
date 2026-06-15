package persistencia;

import java.time.LocalDate;

/**
 * Row returned by {@link PrestecDao#getOverdue}: a currently-loaned book
 * whose loan is older than the threshold (and not yet returned).
 *
 * <p>Replaces the previous {@code List<Object[]>} three-element row
 * (nomPersona, nomLlibre, dataPrestec) with a typed record so callers
 * get compile-time field access and don't have to cast {@code row[0..2]}.
 */
public record OverdueLoan(String nomPersona, String nomLlibre, LocalDate dataPrestec) {

    private static final java.time.format.DateTimeFormatter DISPLAY =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static OverdueLoan fromStrings(String nomPersona, String nomLlibre, String dataPrestecStr) {
        LocalDate d = null;
        if (dataPrestecStr != null && !dataPrestecStr.isBlank()) {
            try { d = LocalDate.parse(dataPrestecStr.trim()); }
            catch (java.time.format.DateTimeParseException ignored) { d = null; }
        }
        return new OverdueLoan(nomPersona, nomLlibre, d);
    }

    public String dataPrestecDisplay() {
        return dataPrestec != null ? dataPrestec.format(DISPLAY) : "";
    }
}
