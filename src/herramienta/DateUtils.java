package herramienta;

public class DateUtils {

    private DateUtils() {}

    /** Extract year from a date string. Handles YYYY-MM-DD, YYYY/MM/DD, MM/YYYY, and embedded years. Returns 0 on failure. */
    public static int parseYear(String date) {
        if (date == null || date.isBlank()) return 0;
        // Try first 4 chars (YYYY-... format)
        if (date.length() >= 4) {
            try {
                int y = Integer.parseInt(date.substring(0, 4));
                if (y >= 1000 && y <= 2200) return y;
            } catch (NumberFormatException ignored) {}
        }
        // Fallback: find any 4-digit year in the string (handles MM/YYYY, Month YYYY, etc.)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(1[0-9]{3}|20[0-9]{2}|21[0-9]{2})").matcher(date);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /** Normalize date separators: replaces '/' with '-' to convert YYYY/MM/DD to YYYY-MM-DD. */
    public static String normalizeDate(String date) {
        return date == null ? null : date.replace('/', '-');
    }
}
