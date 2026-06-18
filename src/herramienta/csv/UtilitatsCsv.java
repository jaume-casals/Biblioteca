package herramienta.csv;

import interficie.LectorLlibre;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UtilitatsCsv {

    private UtilitatsCsv() {}

    public static String[] analitzarLine(String line) {
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

    /** Retorna el valor retallat per al nom de columna donat, o "" si la columna
     *  no hi és o el seu índex excedeix la llargada de la fila. Nota: els consumidors
     *  no poden distingir una columna absent d'una columna amb valor buit — tots dos
     *  donen "". Per a comprovacions de presència de columna, usa primer
     *  hMap.containsKey(col). */
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

    /** Retorna el valor retallat analitzat com a double, o 0.0 si és null/buit/error d'anàlisi. */
    public static double analitzarDoubleOrZero(String s) {
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

    /** Analitza una cadena ISBN, convertint qualsevol ISBN-10 vàlid a ISBN-13.
     *  Delega a {@link herramienta.Isbn13Normalizer#toIsbn13(String)}
     *  (la implementació canònica) perquè la suma de verificació ISBN-10→13 i
     *  la tolerància a la X minúscula estiguin en un sol lloc — segons el finding
     *  LOW de tot.txt sobre les tres implementacions gairebé duplicades
     *  parseIsbn / normalizeIsbn13 / toIsbn13. Cau a un passthrough de només
     *  dígits per a entrades que no coincideixen amb la forma estricta del
     *  normalitzador, de manera que el camí d'importació CSV encara es pot
     *  recuperar d'exportacions poc habituals (el normalitzador retorna null
     *  per a aquestes, però el camí CSV prefereix un millor esforç abans que
     *  una fila fallida). */
    public static String analitzarIsbn(String raw) {
        String normalized = herramienta.Isbn13Normalizer.toIsbn13(raw);
        if (normalized != null) return normalized;
        // Caiguda best-effort: elimina els no dígits, retorna el que queda.
        // Es salta la X perquè les columnes SQL aigües són numèriques.
        return raw == null ? null : raw.replaceAll("[^0-9]", "");
    }

    /** Retorna cert si ja existeix un llibre amb l'ISBN donat a la biblioteca (omet en importar). */
    public static boolean existsInLibrary(LectorLlibre cd, long isbn) {
        try { cd.obtenirLlibre(isbn); return true; } catch (Exception e) { return false; }
    }
}
