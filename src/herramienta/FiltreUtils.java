package herramienta;

public class FiltreUtils {

    // Returns true if ISBNLlibre starts with the prefix ISBN (digit-count match)
    public static boolean matchISBN(Long ISBN, Long ISBNLlibre) {
        if (ISBN == null || ISBNLlibre == null) return false;
        long tamISBN   = ISBN > 9 ? (long)(Math.log10(ISBN) + 1) : 1;
        long tamLlibre = (long)(Math.log10(ISBNLlibre) + 1);
        long divisor   = (long) Math.pow(10, tamLlibre - tamISBN);
        return (ISBNLlibre / divisor) == ISBN;
    }

    // Returns true if x contains s as a substring (case-sensitive)
    public static boolean matchString(String s, String x) {
        if (s == null || x == null) return false;
        if (s.length() > x.length()) return false;
        for (int i = 0; i <= x.length() - s.length(); ++i) {
            if (x.substring(i, i + s.length()).equals(s)) return true;
        }
        return false;
    }
}
