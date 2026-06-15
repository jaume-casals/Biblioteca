package herramienta.csv;

import interficie.BibliotecaWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CsvUtils {

    private CsvUtils() {}

    public static String[] parseLine(String line) {
        if (line == null) return new String[0];
        if (line.indexOf('\r') >= 0) line = line.replace("\r", "");
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuote) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else if (ch == '"') {
                    inQuote = false;
                } else {
                    sb.append(ch);
                }
            } else if (ch == '"') {
                inQuote = true;
            } else if (ch == ',') {
                fields.add(sb.toString().trim()); sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    /** Returns the trimmed value for the given column name, or "" if the column is absent
     *  or its index exceeds the row length. Note: callers cannot distinguish a missing column
     *  from a column whose value is actually empty — both yield "". For column-presence checks,
     *  use hMap.containsKey(col) first. */
    public static String colVal(Map<String, Integer> hMap, String[] c, String col) {
        Integer idx = hMap.get(col);
        if (idx == null || idx >= c.length) return "";
        return c[idx].trim();
    }

    /** Com {@link #colVal} però distingeix "columna absent" de "valor buit".
     *  Retorna {@code Optional.empty()} si la columna no és al header o si
     *  l'índex excedeix la llargada de la fila; {@code Optional.of("")} si
     *  la columna existeix però el valor és buit. */
    public static java.util.Optional<String> colValOpt(Map<String, Integer> hMap, String[] c, String col) {
        Integer idx = hMap.get(col);
        if (idx == null || idx >= c.length) return java.util.Optional.empty();
        String v = c[idx];
        return java.util.Optional.of(v == null ? "" : v.trim());
    }

    /** Returns the trimmed value parsed as a double, or 0.0 on null/blank/parse failure. */
    public static double parseDoubleOrZero(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    public static Map<String, Integer> buildHeaderMap(String[] headers) {
        Map<String, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < headers.length; i++) map.put(headers[i].trim(), i);
        return map;
    }

    public static String csvQ(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /** Parses an ISBN string, converting any valid ISBN-10 to ISBN-13.
     *  Delegates to {@link herramienta.Isbn13Normalizer#toIsbn13(String)}
     *  (the canonical implementation) so the ISBN-10→13 checksum and
     *  the lowercase-X tolerance are in one place — per the tot.txt LOW
     *  finding on the three near-duplicate parseIsbn / normalizeIsbn13
     *  / toIsbn13 implementations. Falls back to a digit-only pass-through
     *  for inputs that don't match the normalizer's strict shape, so the
     *  CSV import path can still recover from unusual exports (the
     *  normalizer returns null for those, but the CSV path wants a best
     *  effort rather than a failed row). */
    public static String parseIsbn(String raw) {
        String normalized = herramienta.Isbn13Normalizer.toIsbn13(raw);
        if (normalized != null) return normalized;
        // Best-effort fallback: strip non-digits, return whatever's left.
        // Skips X because the downstream SQL columns are numeric.
        return raw == null ? null : raw.replaceAll("[^0-9]", "");
    }

    /** Returns true if a book with the given ISBN already exists in the library (skip on import). */
    public static boolean existsInLibrary(BibliotecaWriter cd, long isbn) {
        try { cd.getLlibre(isbn); return true; } catch (Exception e) { return false; }
    }
}
