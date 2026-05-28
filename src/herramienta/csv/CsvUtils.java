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
        String[] fields = new String[16];
        int count = 0;
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
                if (count == fields.length) fields = java.util.Arrays.copyOf(fields, fields.length * 2);
                fields[count++] = sb.toString(); sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        if (count == fields.length) fields = java.util.Arrays.copyOf(fields, fields.length + 1);
        fields[count] = sb.toString().trim();
        // Trim all parsed fields to avoid "Author " != "Author" mismatches
        for (int j = 0; j < count; j++) {
            if (fields[j] != null) fields[j] = fields[j].trim();
        }
        return count + 1 == fields.length ? fields : java.util.Arrays.copyOf(fields, count + 1);
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

    public static double parseDoubleOrZero(String s) {
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

    /** Parses an ISBN string, converting ISBN-10 with X check digit to ISBN-13. */
    public static String parseIsbn(String raw) {
        String s = raw.replaceAll("[^0-9Xx]", "");
        if (!s.isEmpty() && Character.toUpperCase(s.charAt(s.length() - 1)) == 'X' && s.length() == 10) {
            String base12 = "978" + s.substring(0, 9);
            int sum = 0;
            for (int i = 0; i < 12; i++) sum += (base12.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
            return base12 + (10 - sum % 10) % 10;
        }
        return s.replaceAll("[^0-9]", "");
    }

    /** Returns true if a book with the given ISBN already exists in the library (skip on import). */
    public static boolean existsInLibrary(BibliotecaWriter cd, long isbn) {
        try { cd.getLlibre(isbn); return true; } catch (Exception e) { return false; }
    }
}
