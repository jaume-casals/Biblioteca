package herramienta.csv;

import domini.Llibre;
import herramienta.I18n;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class NativeCsvStrategy implements CsvImportStrategy {

    private static final int COL_ISBN   = 0;
    private static final int COL_NOM   = 1;
    private static final int COL_AUTOR  = 2;
    private static final int COL_ANY    = 3;
    private static final int COL_DESCR  = 4;
    private static final int COL_VAL    = 5;
    private static final int COL_PREU   = 6;
    private static final int COL_LLEGIT = 7;
    private static final int COL_PORTADA = 8;
    private static final int COL_LLISTES = 9;

    @Override
    public boolean canHandle(String headerRow) {
        if (headerRow == null || headerRow.isBlank()) return false;
        String[] cols = CsvUtils.parseLine(headerRow);
        return cols.length >= 2;
    }

    @Override
    public boolean parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd)
            throws domini.BibliotecaException {
        if (c.length < 4) throw new domini.BibliotecaException(I18n.t("csv_row_too_short"));
        long isbn = Long.parseLong(c[COL_ISBN].trim());
        if (CsvUtils.existsInLibrary(cd, isbn)) return false;
        int any = 0;
        try { any = Integer.parseInt(c[COL_ANY].trim()); } catch (NumberFormatException ignored) {}
        Llibre l = LlibreValidator.checkLlibre(
            isbn, c[COL_NOM], c[COL_AUTOR],
            any,
            c.length > COL_DESCR ? c[COL_DESCR] : "",
            c.length > COL_VAL ? CsvUtils.parseDoubleOrZero(c[COL_VAL]) : 0.0,
            c.length > COL_PREU ? CsvUtils.parseDoubleOrZero(c[COL_PREU]) : 0.0,
            c.length > COL_LLEGIT ? parseBool(c[COL_LLEGIT].trim()) : false,
            c.length > COL_PORTADA ? c[COL_PORTADA] : "");
        cd.addLlibre(l);
        if (c.length > COL_LLISTES && !c[COL_LLISTES].isBlank()) {
            for (String entry : c[COL_LLISTES].split(";")) {
                String[] parts = entry.split("\\|", 3);
                if (parts.length < 1 || parts[0].isBlank()) continue;
                String nomLlista = parts[0].trim();
                double val = parts.length > 1 ? CsvUtils.parseDoubleOrZero(parts[1]) : 0.0;
                boolean llegitLl = parts.length > 2 && parseBool(parts[2].trim());
                domini.Llista llista = cd.getAllLlistes().stream()
                    .filter(ll -> ll.getNom().equals(nomLlista)).findFirst().orElse(null);
                if (llista == null) llista = cd.addLlista(nomLlista);
                cd.addLlibreToLlista(isbn, llista.getId(), val, llegitLl);
            }
        }
        return true;
    }

    private static boolean parseBool(String s) {
        if (s == null || s.isBlank()) return false;
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s);
    }
}
