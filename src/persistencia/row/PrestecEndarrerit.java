package persistencia.row;

import java.time.LocalDate;

import persistencia.dao.PrestecDao;
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

    public static PrestecEndarrerit fromStrings(String nomPersona, String nomLlibre, String dataPrestecStr) {
        return new PrestecEndarrerit(nomPersona, nomLlibre, RowDates.parseOrNull(dataPrestecStr));
    }

    public String dataPrestecDisplay() {
        return dataPrestec != null ? dataPrestec.format(RowDates.DISPLAY) : "";
    }
}
