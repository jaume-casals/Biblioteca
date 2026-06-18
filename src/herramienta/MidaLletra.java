package herramienta;

public enum MidaLletra {
    SMALL("small", 11),
    MEDIUM("medium", 13),
    LARGE("large", 16);

    public final String key;
    public final int px;

    MidaLletra(String key, int px) { this.key = key; this.px = px; }

    public static MidaLletra fromKey(String k) {
        if (k == null) return MEDIUM;
        return switch (k.toLowerCase(java.util.Locale.ROOT)) {
            case "small"  -> SMALL;
            case "large"  -> LARGE;
            default       -> MEDIUM;
        };
    }
}
