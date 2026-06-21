package herramienta.io.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Analitza la columna "shelves" del CSV natiu: una cadena com
 * <pre>NameA|7.5|true;NameB|0|false</pre>
 * en registres {@link Entrada}.
 */
public final class AnalitzadorPertinençaPrestatgeria {
    public record Entrada(String name, double valoracio, boolean llegit) {}

    private AnalitzadorPertinençaPrestatgeria() {}

    public static List<Entrada> parse(String column) {
        List<Entrada> out = new ArrayList<>();
        if (column == null || column.isBlank()) return out;
        for (String part : column.split(";")) {
            String[] p = part.split("\\|");
            if (p.length == 0 || p[0].isBlank()) continue;
            String name = p[0].trim();
            double val  = p.length > 1 ? parseDouble(p[1]) : 0.0;
            boolean rd  = p.length > 2 && analitzarBool(p[2]);
            out.add(new Entrada(name, val, rd));
        }
        return out;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private static boolean analitzarBool(String s) {
        String t = s.trim().toLowerCase(java.util.Locale.ROOT);
        return t.equals("true") || t.equals("1") || t.equals("yes") || t.equals("y");
    }
}
