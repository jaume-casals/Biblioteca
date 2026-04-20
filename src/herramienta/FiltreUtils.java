package herramienta;

public class FiltreUtils {

    public static boolean matchISBN(Long prefix, Long isbn) {
        if (prefix == null || isbn == null) return false;
        return isbn.toString().startsWith(prefix.toString());
    }

    public static boolean matchString(String needle, String haystack) {
        if (needle == null || haystack == null) return false;
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }
}
