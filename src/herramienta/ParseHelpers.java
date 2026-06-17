package herramienta;

import java.util.List;

/**
 * Shared text-to-number parsers for the presentacio layer.
 *
 * Two contracts coexist:
 * <ul>
 *   <li><b>Best-effort</b> ({@code parseIntOrNull} / {@code parseLongOrNull} /
 *       {@code parseDoubleOrNull}): silent on bad input. Returns a boxed type so
 *       the caller can distinguish "empty" from "zero". Use in sort, display,
 *       or default-aware paths where an error dialog is undesirable.</li>
 *   <li><b>Strict</b> ({@code parseInt} / {@code parseLong} / {@code parseDouble}):
 *       empty input is mapped to {@code defaultValue}; non-numeric input
 *       appends a translated message to {@code errors} and returns
 *       {@code defaultValue}. Use in save paths so a single bad field
 *       aborts the save before the model is mutated.</li>
 * </ul>
 *
 * Replaces the parallel parsers that used to live in
 * {@code DetallesLlibrePanelControl} and {@code GuardarLlibresDialogoControl}.
 */
public final class ParseHelpers {

    private ParseHelpers() {}

    public static Long parseLongOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Long.parseLong(t); }
        catch (NumberFormatException e) { return null; }
    }

    public static Integer parseIntOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) { return null; }
    }

    public static Double parseDoubleOrNull(String s) {
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
