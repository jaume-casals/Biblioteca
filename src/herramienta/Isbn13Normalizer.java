package herramienta;

/** Convert ISBN-10 / ISBN-10-with-X-check to ISBN-13. Returns digit-only string. */
public final class Isbn13Normalizer {
    private Isbn13Normalizer() {}

    /** Returns ISBN-13 (or original digits) for any reasonable input; null if no digits. */
    public static String toIsbn13(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (Character.toUpperCase(trimmed.charAt(trimmed.length() - 1)) == 'X') {
            String core = trimmed.substring(0, trimmed.length() - 1).replaceAll("[^0-9]", "");
            if (core.length() == 9) return convertToIsbn13("978" + core);
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (digits.length() == 10 && digits.charAt(0) == '0') return convertToIsbn13("978" + digits.substring(0, 9));
        return digits;
    }

    private static String convertToIsbn13(String base12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return base12 + ((10 - sum % 10) % 10);
    }
}
