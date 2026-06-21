package herramienta.config;

/** Mida de lletra de la interfície. Les tres mides canòniques viuen com
 *  a {@code public static final} perquè el consumidor pugui accedir als
 *  camps ({@code MidaLletra.SMALL.px}) sense la sobrecàrrega sintàctica
 *  dels accessors de {@code record}. */
public final class MidaLletra {
    public static final MidaLletra SMALL  = new MidaLletra("small",  11);
    public static final MidaLletra MEDIUM = new MidaLletra("medium", 13);
    public static final MidaLletra LARGE  = new MidaLletra("large",  16);

    public final String key;
    public final int px;

    private MidaLletra(String key, int px) { this.key = key; this.px = px; }

    public static MidaLletra fromKey(String k) {
        if (k == null) return MEDIUM;
        return switch (k.toLowerCase(java.util.Locale.ROOT)) {
            case "small"  -> SMALL;
            case "large"  -> LARGE;
            default       -> MEDIUM;
        };
    }
}
