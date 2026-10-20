package herramienta.ui;

/**
 * Tokens de disseny dependents del tema.
 *
 * <p>Aquesta classe és la font de veritat de les <em>mides</em>: espaiat,
 * radis, alçades de control i amplades de lectura. Els <em>colors</em> viuen
 * a {@link UITheme.Paleta}; els <em>tokens</em> de mida són hereves del tema
 * perquè un canvi de tema no ha d'alterar la jerarquia espacial de la
 * interfície.
 *
 * <p>Regles d'ús:
 * <ul>
 *   <li>Cap valor literal de mida escapat en els components: si cal una mida
 *       nova, s'afegeix aquí.</li>
 *   <li>Les mides són <em>lògiques</em>: es combinen amb la mida de lletra
 *       configurada mitjançant {@link #escalaLletra(int)} perquè el canvi de
 *       mida de lletra no deixi controls truncats.</li>
 *   <li>Resol la prohibició de mides fixes que trenquin l'escalat: cap
 *       control depèn d'una alçada fixa, sinó d'una <em>alçada mínima</em>.</li>
 * </ul>
 */
public final class Tokens {

    private Tokens() {}

    // ── Escala d'espaiat ─────────────────────────────────────────────────────
    /** Unitat base: 4 px. Tots els espaiats són múltiples d'aquesta unitat. */
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int SPACE_5 = 24;
    public static final int SPACE_6 = 32;

    // ── Radis ────────────────────────────────────────────────────────────────
    public static final int RADIUS_SM = 4;
    public static final int RADIUS_MD = 6;
    public static final int RADIUS_LG = 8;

    // ── Mides de control ────────────────────────────────────────────────────
    /** Alçada mínima d'un control d'una línia (botó, camp, combo). */
    public static final int CONTROL_HEIGHT = 34;
    /** Alçada mínima d'un control d'una línia amb lletra gran. */
    public static final int CONTROL_HEIGHT_LG = 38;
    /** Alçada d'una fila de taula compacta. */
    public static final int TABLE_ROW_HEIGHT = 30;
    /** Amplada de la columna de la barra lateral. */
    public static final int SIDEBAR_WIDTH = 244;
    /** Amplada mínima de la capçalera de la finestra. */
    public static final int HEADER_MIN_WIDTH = 520;
    /** Amplada de lectura dels formularis: evita línies de text excessivament llargues. */
    public static final int FORM_READ_WIDTH = 560;
    /** Amplada mínima d'una targeta de galeria. */
    public static final int CARD_MIN_WIDTH = 180;
    /** Amplada màxima d'una targeta de galeria. */
    public static final int CARD_MAX_WIDTH = 210;

    // ── Icones ───────────────────────────────────────────────────────────────
    public static final int ICON_SIZE = 16;
    public static final int ICON_SIZE_LG = 20;

    // ── Tipografia ───────────────────────────────────────────────────────────
    /** Mida base de la lletra, en punts, abans d'escalar. */
    public static final int FONT_SIZE_BASE = 14;
    /** Mida del títol de vista. */
    public static final int FONT_SIZE_TITLE = 20;
    /** Mida d'un encapçalament de secció. */
    public static final int FONT_SIZE_SECTION = 13;
    /** Mida del text auxiliar. */
    public static final int FONT_SIZE_SMALL = 12;
    /** Mida del text del comptador de la capçalera. */
    public static final int FONT_SIZE_COUNT = 13;

    // ── Escala de la mida de lletra ──────────────────────────────────────────

    /**
     * Multiplicador d'escala derivat de la mida de lletra configurada.
     *
     * <p>Es calcula a partir de la mida nominal en punts: 11 → 0,79;
     * 13 → 0,93; 16 → 1,14. Els valors es redonen a dues xifres perquè
     * el resultat sigui estable i predictable entre execucions.
     */
    public static double escalaLletra(int pxLletra) {
        if (pxLletra <= 0) return 1.0;
        return Math.round((pxLletra / (double) FONT_SIZE_BASE) * 100.0) / 100.0;
    }

    /**
     * Escala una mida de control segons la mida de lletra.
     *
     * <p>El resultat es limita a un factor d'1,6 perquè una lletra molt
     * gran no faci els controls disproportionats, i a un factor de 0,85
     * perquè la lletra petita no els faci il·legibles.
     */
    public static int escalaLletra(int pxLletra, int midaBase) {
        double factor = escalaLletra(pxLletra);
        if (factor > 1.6) factor = 1.6;
        if (factor < 0.85) factor = 0.85;
        return Math.max(1, (int) Math.round(midaBase * factor));
    }
}
