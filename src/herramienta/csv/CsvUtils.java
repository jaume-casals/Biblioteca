package herramienta.csv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CsvUtils {

    private CsvUtils() {}

    public static String[] parseLine(String line) {
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
                fields.add(sb.toString()); sb = new StringBuilder();
            } else {
                sb.append(ch);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

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
}
