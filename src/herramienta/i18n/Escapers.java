package herramienta.i18n;

/** Utilitats centralitzades d'escapament de cadenes usades pels camins d'exportació/serialització. */
public final class Escapers {
    private Escapers() {}

    /** HTML-escape: tracta & < > " '. */
    public static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Escapament SQL de cadena entre cometes simples (duplica el caràcter {@code '}). El consumidor afegeix les cometes exteriors. */
    public static String sql(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    /** Escapament de cadena JSON (tracta la barra invertida, les cometes i els caràcters de control). */
    public static String json(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"'  -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /** Cometes CSV segons RFC-4180 (duplica la {@code "} interna, embolcall en {@code "}). */
    public static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /** Com {@link #html} però converteix els salts de línia en {@code <br>}, per a missatges d'error Swing. */
    public static String htmlWithBreaks(String s) {
        if (s == null) return "";
        return html(s).replace("\n", "<br>");
    }
}
