package herramienta;

/** Idiomes de la IU suportats. L'ordinal coincideix amb l'índex de columna usat pels arrays {@link I18n#TAULA}. */
public enum Idioma {
    CA, ES, EN;

    public String code() { return name().toLowerCase(java.util.Locale.ROOT); }

    public static Idioma fromCode(String code) {
        if (code == null) return CA;
        return switch (code.toLowerCase(java.util.Locale.ROOT)) {
            case "es" -> ES;
            case "en" -> EN;
            default -> CA;
        };
    }
}
