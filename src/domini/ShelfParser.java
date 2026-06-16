package domini;

import herramienta.csv.CsvUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ShelfParser {

    private static final Logger LOG = Logger.getLogger(ShelfParser.class.getName());

    public record ShelfEntry(String nom, double valoracio, boolean llegit) {}

    public static List<ShelfEntry> parseShelfEntries(String raw) {
        List<ShelfEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) return entries;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split("\\|", 3);
            // isBlank() doesn't catch NUL / control chars; use trim().isEmpty()
            // so a name like "\0\0\0" is correctly treated as missing.
            if (parts.length < 1 || parts[0].trim().isEmpty()) continue;
            String nom = parts[0].trim();
            double val = parts.length > 1 ? CsvUtils.parseDoubleOrZero(parts[1]) : 0.0;
            boolean llegit = parts.length > 2 && parseBool(parts[2].trim());
            entries.add(new ShelfEntry(nom, val, llegit));
        }
        return entries;
    }

    private static boolean parseBool(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public static void exportToCsv(File f, List<Llibre> view, interficie.ShelfReader cd) throws Exception {
        java.util.Map<Integer, Llista> llistaById = new java.util.HashMap<>();
        for (Llista ll : cd.getAllLlistes()) llistaById.put(ll.getId(), ll);
        java.util.Map<Long, List<persistencia.LlibreLlistaRow>> llistaRows = new java.util.HashMap<>();
        for (persistencia.LlibreLlistaRow row : cd.getAllLlibreLlistaRows())
            llistaRows.computeIfAbsent(row.isbn(), k -> new ArrayList<>()).add(row);

        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada,Llistes");
            for (Llibre l : view) {
                try {
                    List<persistencia.LlibreLlistaRow> rows = llistaRows.getOrDefault(l.getISBN(), java.util.Collections.emptyList());
                    StringBuilder llistesStr = new StringBuilder();
                    for (persistencia.LlibreLlistaRow row : rows) {
                        Llista ll = llistaById.get(row.llistaId());
                        if (ll == null) continue;
                        if (llistesStr.length() > 0) llistesStr.append(';');
                        llistesStr.append(esc(ll.getNom())).append('|')
                            .append(row.valoracio()).append('|').append(row.llegit());
                    }
                    pw.printf(java.util.Locale.ROOT, "\"%s\",\"%s\",\"%s\",%d,\"%s\",%.1f,%.2f,%b,\"%s\",\"%s\"%n",
                        l.getISBN(), esc(l.getNom()), esc(l.getAutor()), l.getAny(),
                        esc(l.getDescripcio()), l.getValoracio(), l.getPreu(), l.getLlegit(),
                        esc(l.getImatge()), llistesStr);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "ShelfParser.exportToCsv: skipped ISBN " + l.getISBN(), e);
                }
            }
        }
    }

    public static String joinShelfEntries(List<ShelfEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (ShelfEntry e : entries) {
            if (sb.length() > 0) sb.append(';');
            sb.append(esc(e.nom())).append('|').append(e.valoracio()).append('|').append(e.llegit());
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    private ShelfParser() {}
}