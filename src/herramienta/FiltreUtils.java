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

    // Returns true if x contains s as a substring (case-insensitive)
    public static boolean matchString(String s, String x) {
        if (s == null || x == null) return false;
        String sl = s.toLowerCase();
        String xl = x.toLowerCase();
        if (sl.length() > xl.length()) return false;
        for (int i = 0; i <= xl.length() - sl.length(); ++i) {
            if (xl.substring(i, i + sl.length()).equals(sl)) return true;
        }
        return false;
    }
}
