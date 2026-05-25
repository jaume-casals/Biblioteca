package herramienta.csv;

import domini.Llibre;
import herramienta.I18n;
import herramienta.LlibreValidator;
import interficie.BibliotecaWriter;

import java.util.Map;

public class NativeCsvStrategy implements CsvImportStrategy {

    @Override
    public boolean canHandle(String headerRow) {
        return true; // fallback
    }

    @Override
    public boolean parseLine(String[] c, Map<String, Integer> hMap, BibliotecaWriter cd) throws Exception {
        if (c.length < 4) throw new IllegalArgumentException(I18n.t("csv_row_too_short"));
        long isbn = Long.parseLong(c[0].trim());
        if (CsvUtils.existsInLibrary(cd, isbn)) return false;
        int any = 0;
        try { any = Integer.parseInt(c[3].trim()); } catch (NumberFormatException ignored) {}
        Llibre l = LlibreValidator.checkLlibre(
            isbn, c[1], c[2],
            any,
            c.length > 4 ? c[4] : "",
            c.length > 5 ? CsvUtils.parseDoubleOrZero(c[5]) : 0.0,
            c.length > 6 ? CsvUtils.parseDoubleOrZero(c[6]) : 0.0,
            c.length > 7 ? parseBool(c[7].trim()) : false,
            c.length > 8 ? c[8] : "");
        cd.addLlibre(l);
        if (c.length > 9 && !c[9].isBlank()) {
            for (String entry : c[9].split(";")) {
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
