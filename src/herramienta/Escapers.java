package herramienta;

/** Centralized string-escape utilities used by export/serialization paths. */
public final class Escapers {
    private Escapers() {}

    /** HTML-escape: handles & < > " '. */
    public static String html(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** SQL single-quote-string escape (doubles a {@code '} character). The caller adds outer quotes. */
    public static String sql(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    /** JSON string escape (handles backslash, quote, control chars). */
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

    /** CSV quoting per RFC-4180 (double internal {@code "}, wrap in {@code "}). */
    public static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
