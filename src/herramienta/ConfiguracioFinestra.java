package herramienta;

import java.util.List;

/**
 * Vista tipada de lectura/escriptura de les claus de {@link Config} relacionades
 * amb la geometria de finestra i les columnes de la taula. Les claus
 * d'amplada/visibilitat de columna viuen al sub-emmagatzematge WINDOW
 * (veure {@code Config.WINDOW_KEYS} més la regla dinàmica de prefix
 * {@code colWidth_*} / {@code colVisible_*}).
 */
public final class ConfiguracioFinestra {
    private ConfiguracioFinestra() {}

    public static int x()              { return Configuracio.obtenirWindowX(); }
    public static int y()              { return Configuracio.obtenirWindowY(); }
    public static int width()          { return Configuracio.obtenirWindowWidth(); }
    public static int height()         { return Configuracio.obtenirWindowHeight(); }
    public static boolean maximized()  { return Configuracio.esWindowMaximized(); }
    public static void setBounds(int x, int y, int w, int h) { Configuracio.posarWindowBounds(x, y, w, h); }
    public static void posarMaximized(boolean m) { Configuracio.posarWindowMaximized(m); }

    public static int colWidth(int col, int defaultWidth)  { return Configuracio.obtenirColWidth(col, defaultWidth); }
    public static void posarColWidths(int[] widths)          { Configuracio.posarColWidths(widths); }
    public static boolean colVisible(int col)              { return Configuracio.obtenirColVisible(col); }
    public static void posarColVisible(int col, boolean v)  { Configuracio.posarColVisible(col, v); }

    public static List<String> listDbProfiles() { return Configuracio.listDbProfiles(); }
}
