package herramienta;

import java.util.List;

/**
 * Typed read/write view of window-geometry and table-column {@link Config} keys.
 * The column-width / column-visible keys live in the WINDOW sub-store
 * (see {@code Config.WINDOW_KEYS} plus the dynamic {@code colWidth_*} /
 * {@code colVisible_*} prefix rule).
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
