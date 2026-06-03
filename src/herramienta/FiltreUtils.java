package herramienta;

import domini.Llibre;
import domini.LlibreFilter;
import java.util.Set;

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

    /**
     * Single source of truth for the in-memory filter predicate. SQL path in
     * {@code LlibreDao.search} mirrors this — when adding a new field to
     * {@link LlibreFilter}, also extend the SQL builder there.
     *
     * <p>{@code tagISBNs} and {@code llistaISBNs} are pre-computed ISBN sets
     * supplied by the caller (they require DB lookups) so this method stays
     * pure and side-effect-free.
     */
    public static boolean matches(Llibre l, LlibreFilter f, Set<Long> tagISBNs, Set<Long> llistaISBNs) {
        if (f.getAutor() != null && !matchString(f.getAutor(), l.getAutor())) return false;
        if (f.getNom()   != null && !matchString(f.getNom(),   l.getNom()))   return false;
        if (f.getIsbn()  != null && !matchISBN(f.getIsbn(), l.getISBN()))    return false;
        if (f.getAnyMin()       != null && (l.getAny()       == null || l.getAny()       < f.getAnyMin()))       return false;
        if (f.getAnyMax()       != null && (l.getAny()       == null || l.getAny()       > f.getAnyMax()))       return false;
        if (f.getValoracioMin() != null && (l.getValoracio() == null || l.getValoracio() < f.getValoracioMin())) return false;
        if (f.getValoracioMax() != null && (l.getValoracio() == null || l.getValoracio() > f.getValoracioMax())) return false;
        if (f.getPreuMin()      != null && (l.getPreu()      == null || l.getPreu()      < f.getPreuMin()))      return false;
        if (f.getPreuMax()      != null && (l.getPreu()      == null || l.getPreu()      > f.getPreuMax()))      return false;
        if (f.getLlegit()       != null && !f.getLlegit().equals(l.getLlegit())) return false;
        if (tagISBNs    != null && !tagISBNs.contains(l.getISBN()))    return false;
        if (llistaISBNs != null && !llistaISBNs.contains(l.getISBN())) return false;
        if (f.getEditorial() != null && !matchString(f.getEditorial(), l.getEditorial())) return false;
        if (f.getSerie()     != null && !matchString(f.getSerie(),     l.getSerie()))     return false;
        if (f.getFormat()    != null && !f.getFormat().equalsIgnoreCase(l.getFormat()))   return false;
        if (f.getIdioma()    != null && !matchString(f.getIdioma(),    l.getIdioma()))    return false;
        return true;
    }
}
