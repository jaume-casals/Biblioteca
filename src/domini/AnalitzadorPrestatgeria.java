package domini;

import herramienta.io.csv.UtilitatsCsv;
import persistencia.row.LlibreLlistaRow;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AnalitzadorPrestatgeria {

    private static final Logger LOG = Logger.getLogger(AnalitzadorPrestatgeria.class.getName());

    public record ShelfEntry(String nom, double valoracio, boolean llegit) {}

    public static List<ShelfEntry> analitzarShelfEntries(String raw) {
        List<ShelfEntry> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) return entries;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split("\\|", 3);
            // isBlank() no atrapa caràcters NUL / de control; fem servir
            // trim().isEmpty() perquè un nom com "\0\0\0" es tracti
            // correctament com a absent.
            if (parts.length < 1 || parts[0].trim().isEmpty()) continue;
            String nom = parts[0].trim();
            double val = parts.length > 1 ? UtilitatsCsv.analitzarDoubleOrZero(parts[1]) : 0.0;
            boolean llegit = parts.length > 2 && analitzarBool(parts[2].trim());
            entries.add(new ShelfEntry(nom, val, llegit));
        }
        return entries;
    }

    private static boolean analitzarBool(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    public static void exportarToCsv(File f, List<Llibre> view,
                                     Map<Integer, Llista> llistaById,
                                     Map<Long, List<LlibreLlistaRow>> llistaRows) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada,Llistes");
            for (Llibre l : view) {
                try {
                    List<LlibreLlistaRow> rows = llistaRows.getOrDefault(l.obtenirISBN(), Collections.emptyList());
                    StringBuilder llistesStr = new StringBuilder();
                    for (LlibreLlistaRow row : rows) {
                        Llista ll = llistaById.get(row.llistaId());
                        if (ll == null) continue;
                        if (llistesStr.length() > 0) llistesStr.append(';');
                        llistesStr.append(esc(ll.obtenirNom())).append('|')
                            .append(row.valoracio()).append('|').append(row.llegit());
                    }
                Integer any = l.obtenirAny() != null ? l.obtenirAny() : 0;
                Double valoracio = l.obtenirValoracio() != null ? l.obtenirValoracio() : 0.0;
                Double preu = l.obtenirPreu() != null ? l.obtenirPreu() : 0.0;
                boolean llegit = l.obtenirLlegit() != null && l.obtenirLlegit();
                pw.printf(java.util.Locale.ROOT, "%s,%s,%s,%d,%s,%.1f,%.2f,%b,%s,%s%n",
                    UtilitatsCsv.csvQ(l.obtenirISBN() == null ? "" : String.valueOf(l.obtenirISBN())),
                    UtilitatsCsv.csvQ(l.obtenirNom()),
                    UtilitatsCsv.csvQ(l.obtenirAutor()),
                    any,
                    UtilitatsCsv.csvQ(l.obtenirDescripcio()),
                    valoracio, preu, llegit,
                    UtilitatsCsv.csvQ(l.obtenirImatge()),
                    UtilitatsCsv.csvQ(llistesStr.toString()));
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "ShelfParser.exportToCsv: skipped ISBN " + l.obtenirISBN(), e);
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

    private AnalitzadorPrestatgeria() {}
}