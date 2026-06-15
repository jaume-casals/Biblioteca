package herramienta;

/** Convert ISBN-10 / ISBN-10-with-X-check to ISBN-13. Returns digit-only string. */
public final class Isbn13Normalizer {
    private Isbn13Normalizer() {}

    /**
     * Returns ISBN-13 for any reasonable input; null if no digits at all,
     * or if the stripped length is not 10 (with X check) or 13 (no X).
     * Callers (e.g. {@code LlibreValidator.checkLlibre}) used to fall
     * through to {@code Long.parseLong(digits)} on a 12-digit result
     * which threw on the trailing non-digit. The previous silent
     * fallback hid the real failure (per the tot.txt LOW finding).
     */
    public static String toIsbn13(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        // Uppercase any 'x' before stripping non-digits, so a paste
        // like "ISBN 978019853110x" (lowercase x as the ISBN-10
        // check digit) still keeps the X (per the second tot.txt
        // LOW finding on this file). Without this normalisation the
        // regex [^0-9X] silently drops the lowercase 'x' and the
        // ISBN falls through to the null branch.
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
