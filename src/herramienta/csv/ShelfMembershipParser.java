package herramienta.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the native CSV "shelves" column: a string like
 * <pre>NameA|7.5|true;NameB|0|false</pre>
 * into {@link Entry} records.
 */
public final class ShelfMembershipParser {
    public record Entry(String name, double valoracio, boolean llegit) {}

    private ShelfMembershipParser() {}

    public static List<Entry> parse(String column) {
        List<Entry> out = new ArrayList<>();
        if (column == null || column.isBlank()) return out;
        for (String part : column.split(";")) {
            String[] p = part.split("\\|");
            if (p.length == 0 || p[0].isBlank()) continue;
            String name = p[0].trim();
            double val  = p.length > 1 ? parseDouble(p[1]) : 0.0;
            boolean rd  = p.length > 2 && parseBool(p[2]);
            out.add(new Entry(name, val, rd));
        }
        return out;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private static boolean parseBool(String s) {
        String t = s.trim().toLowerCase(java.util.Locale.ROOT);
        return t.equals("true") || t.equals("1") || t.equals("yes") || t.equals("y");
    }
}
