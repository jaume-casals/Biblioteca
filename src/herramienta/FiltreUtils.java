package herramienta;

public class FiltreUtils {

    private FiltreUtils() {}

    public static boolean matchISBN(Long prefix, Long isbn) {
        if (prefix == null || isbn == null) return false;
        return String.valueOf(isbn).startsWith(String.valueOf(prefix));
    }

    public static boolean matchString(String needle, String haystack) {
        if (needle == null || haystack == null) return false;
        return normalize(haystack).contains(normalize(needle));
    }

    /**
     * Cerca totes les paraules de {@code query} (separades per espais) a
     * {@code haystack}, ignorant ordre i posició. "tolkien ring" troba
     * "The Lord of the Rings". Paraules buides s'ignoren.
     */
    public static boolean matchStringContainsAll(String query, String haystack) {
        if (query == null || haystack == null) return false;
        String normHay = normalize(haystack);
        for (String word : normalize(query).split("\\s+")) {
            if (word.isEmpty()) continue;
            if (!normHay.contains(word)) return false;
        }
        return true;
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String nfd = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "").toLowerCase(java.util.Locale.ROOT);
    }
}
