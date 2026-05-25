package herramienta;

/** Supported UI languages. Ordinal matches the column index used by {@link I18n#TABLE} arrays. */
public enum Lang {
    CA, ES, EN;

    public String code() { return name().toLowerCase(java.util.Locale.ROOT); }

    public static Lang fromCode(String code) {
        if (code == null) return CA;
        return switch (code.toLowerCase(java.util.Locale.ROOT)) {
            case "es" -> ES;
            case "en" -> EN;
            default -> CA;
        };
    }
}
