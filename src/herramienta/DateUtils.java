package herramienta;

public class DateUtils {

    private DateUtils() {}

    public static java.util.Optional<Integer> parseYear(String date) {
        if (date == null || date.isBlank()) return java.util.Optional.empty();
        if (date.length() >= 4) {
            try {
                int y = Integer.parseInt(date.substring(0, 4));
                if (y >= 1000 && y <= 2200) return java.util.Optional.of(y);
            } catch (NumberFormatException ignored) {}
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(1[0-9]{3}|20[0-9]{2}|21[0-9]{2})").matcher(date);
        return m.find() ? java.util.Optional.of(Integer.parseInt(m.group(1))) : java.util.Optional.empty();
    }

    public static String normalizeDate(String date) {
        return date == null ? null : date.replace('/', '-');
    }

    public static java.time.LocalDate parseIsoDate(String date) {
        if (date == null || date.isBlank()) return null;
        try { return java.time.LocalDate.parse(date.trim()); }
        catch (java.time.format.DateTimeParseException e) { return null; }
    }

    public static String formatDateForDisplay(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "";
        java.time.LocalDate d = parseIsoDate(isoDate);
        if (d == null) return isoDate;
        return d.format(java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"));
    }
}
