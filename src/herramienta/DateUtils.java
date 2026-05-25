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

    /** Parse an ISO-8601 date string (yyyy-MM-dd). Returns null if input is null, blank, or unparseable. */
    public static java.time.LocalDate parseIsoDate(String date) {
        if (date == null || date.isBlank()) return null;
        try { return java.time.LocalDate.parse(date.trim()); }
        catch (java.time.format.DateTimeParseException e) { return null; }
    }

    /** Format an ISO date string for display. Converts "2023-05-10" to a locale-friendly format.
     *  Returns the original string if parsing fails. */
    public static String formatDateForDisplay(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "";
        java.time.LocalDate d = parseIsoDate(isoDate);
        if (d == null) return isoDate;
        return d.format(java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
    }
}
