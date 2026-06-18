package persistencia;

import java.time.LocalDate;

/**
 * Fila retornada per {@link PrestecDao#obtenirEndarrerits}: un llibre
 * actualment prestat el prèstec del qual és més antic que el llindar
 * (i encara no retornat).
 *
 * <p>Substitueix l'anterior fila de tres elements {@code List<Object[]>}
 * (nomPersona, nomLlibre, dataPrestec) per un registre tipat perquè els
 * consumidors tinguin accés als camps en temps de compilació i no hagin
 * de fer cast a {@code row[0..2]}.
 */
public record PrestecEndarrerit(String nomPersona, String nomLlibre, LocalDate dataPrestec) {

    private static final java.time.format.DateTimeFormatter DISPLAY =
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static PrestecEndarrerit fromStrings(String nomPersona, String nomLlibre, String dataPrestecStr) {
        LocalDate d = null;
        if (dataPrestecStr != null && !dataPrestecStr.isBlank()) {
            try { d = LocalDate.parse(dataPrestecStr.trim()); }
            catch (java.time.format.DateTimeParseException ignored) { d = null; }
        }
        return new PrestecEndarrerit(nomPersona, nomLlibre, d);
    }

    public String dataPrestecDisplay() {
        return dataPrestec != null ? dataPrestec.format(DISPLAY) : "";
    }
}
