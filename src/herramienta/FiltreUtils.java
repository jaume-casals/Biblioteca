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
        if (f.obtenirAutor() != null && !matchString(f.obtenirAutor(), l.obtenirAutor())) return false;
        if (f.obtenirNom()   != null && !matchString(f.obtenirNom(),   l.obtenirNom()))   return false;
        if (f.obtenirIsbn()  != null && !matchISBN(f.obtenirIsbn(), l.obtenirISBN()))    return false;
        if (f.obtenirAnyMin()       != null && (l.obtenirAny()       == null || l.obtenirAny()       < f.obtenirAnyMin()))       return false;
        if (f.obtenirAnyMax()       != null && (l.obtenirAny()       == null || l.obtenirAny()       > f.obtenirAnyMax()))       return false;
        if (f.obtenirValoracioMin() != null && (l.obtenirValoracio() == null || l.obtenirValoracio() < f.obtenirValoracioMin())) return false;
        if (f.obtenirValoracioMax() != null && (l.obtenirValoracio() == null || l.obtenirValoracio() > f.obtenirValoracioMax())) return false;
        if (f.obtenirPreuMin()      != null && (l.obtenirPreu()      == null || l.obtenirPreu()      < f.obtenirPreuMin()))      return false;
        if (f.obtenirPreuMax()      != null && (l.obtenirPreu()      == null || l.obtenirPreu()      > f.obtenirPreuMax()))      return false;
        if (f.obtenirLlegit()       != null && !f.obtenirLlegit().equals(l.obtenirLlegit())) return false;
        if (tagISBNs    != null && !tagISBNs.contains(l.obtenirISBN()))    return false;
        if (llistaISBNs != null && !llistaISBNs.contains(l.obtenirISBN())) return false;
        if (f.obtenirEditorial() != null && !matchString(f.obtenirEditorial(), l.obtenirEditorial())) return false;
        if (f.obtenirSerie()     != null && !matchString(f.obtenirSerie(),     l.obtenirSerie()))     return false;
        if (f.getFormat()    != null && !f.getFormat().equalsIgnoreCase(l.getFormat()))   return false;
        if (f.obtenirIdioma()    != null && !matchString(f.obtenirIdioma(),    l.obtenirIdioma()))    return false;
        return true;
    }
}
