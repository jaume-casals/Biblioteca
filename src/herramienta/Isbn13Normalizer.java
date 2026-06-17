package herramienta;

/** Converteix ISBN-10 / ISBN-10-amb-control-X a ISBN-13. Retorna una cadena només de dígits. */
public final class Isbn13Normalizer {
    private Isbn13Normalizer() {}

    /**
     * Retorna ISBN-13 per a qualsevol entrada raonable; null si no hi ha
     * cap dígit, o si la longitud netejada no és 10 (amb control X) o 13
     * (sense X). Els consumidors (p. ex.
     * {@code LlibreValidator.checkLlibre}) abans queien a
     * {@code Long.parseLong(digits)} amb un resultat de 12 dígits que
     * llançava sobre el caràcter no numèric final. La caiguda silenciosa
     * anterior amagava la fallada real (segons el finding LOW de tot.txt).
     */
    public static String toIsbn13(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // Posem en majúscules qualsevol 'x' abans d'eliminar els no
        // dígits, de manera que un enganxat com "ISBN 978019853110x"
        // (x minúscula com a dígit de control ISBN-10) conservi
        // l'X (segons el segon finding LOW de tot.txt sobre aquest
        // fitxer). Sense aquesta normalització, la regex [^0-9X]
        // descarta silenciosament la 'x' minúscula i l'ISBN cau a la
        // branca null.
        String digits = trimmed.toUpperCase().replaceAll("[^0-9X]", "");
        if (digits.length() == 10 && (Character.isDigit(digits.charAt(9)) || digits.charAt(9) == 'X')) {
            return convertToIsbn13("978" + digits.substring(0, 9));
        }
        if (digits.length() == 13) return digits;
        return null;
    }

    private static String convertToIsbn13(String base12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return base12 + ((10 - sum % 10) % 10);
    }
}
