package herramienta.text;

import herramienta.i18n.I18n;
import java.util.List;

/**
 * Analitzadors compartits de text-a-nombre per a la capa de presentacio.
 *
 * Coexisteixen dos contractes:
 * <ul>
 *   <li><b>Best-effort</b> ({@code analitzarIntOrNull} / {@code analitzarLongOrNull} /
 *       {@code analitzarDoubleOrNull}): silenciós amb entrada incorrecta. Retorna
 *       un tipus encaixat perquè el consumidor pugui distingir "buit" de "zero".
 *       Usa en ordre, visualització o camins conscients del valor per defecte
 *       on un diàleg d'error no és desitjable.</li>
 *   <li><b>Estricte</b> ({@code parseInt} / {@code parseLong} / {@code parseDouble}):
 *       l'entrada buida es mapeja a {@code defaultValue}; l'entrada no
 *       numèrica afegeix un missatge traduït a {@code errors} i retorna
 *       {@code defaultValue}. Usa en camins de desat perquè un sol camp
 *       dolent avorti el desat abans que el model es muti.</li>
 * </ul>
 *
 * Substitueix els analitzadors paral·lels que abans vivien a
 * {@code ControladorPanellDetallsLlibre} i {@code ControladorDialegDesarLlibres}.
 */
public final class ParseHelpers {

    private ParseHelpers() {}

    public static Long analitzarLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Long.parseLong(t); }
        catch (NumberFormatException e) { return null; }
    }

    public static Integer analitzarIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) { return null; }
    }

    public static Double analitzarDoubleOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return null; }
    }

    public static long parseLong(String s, long defaultValue, String fieldKey, List<String> errors) {
        if (s == null) return defaultValue;
        String t = s.trim();
        if (t.isEmpty()) return defaultValue;
        try { return Long.parseLong(t); }
        catch (NumberFormatException e) {
            errors.add(I18n.t("val_must_be_int", I18n.t(fieldKey)));
            return defaultValue;
        }
    }

    public static int parseInt(String s, int defaultValue, String fieldKey, List<String> errors) {
        if (s == null) return defaultValue;
        String t = s.trim();
        if (t.isEmpty()) return defaultValue;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) {
            errors.add(I18n.t("val_must_be_int", I18n.t(fieldKey)));
            return defaultValue;
        }
    }

    public static double parseDouble(String s, double defaultValue, String fieldKey, List<String> errors) {
        if (s == null) return defaultValue;
        String t = s.trim();
        if (t.isEmpty()) return defaultValue;
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) {
            errors.add(I18n.t("val_must_be_number", I18n.t(fieldKey)));
            return defaultValue;
        }
    }
}
