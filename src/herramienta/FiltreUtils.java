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

    public static String normalize(String s) {
        String nfd = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "").toLowerCase(java.util.Locale.ROOT);
    }
}
