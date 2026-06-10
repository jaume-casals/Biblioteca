package herramienta;

import java.util.List;

/**
 * Typed read/write view of window-geometry and table-column {@link Config} keys.
 * The column-width / column-visible keys live in the WINDOW sub-store
 * (see {@code Config.WINDOW_KEYS} plus the dynamic {@code colWidth_*} /
 * {@code colVisible_*} prefix rule).
 */
public final class WindowConfig {
    private WindowConfig() {}

    public static int x()              { return Config.getWindowX(); }
    public static int y()              { return Config.getWindowY(); }
    public static int width()          { return Config.getWindowWidth(); }
    public static int height()         { return Config.getWindowHeight(); }
    public static boolean maximized()  { return Config.isWindowMaximized(); }
    public static void setBounds(int x, int y, int w, int h) { Config.setWindowBounds(x, y, w, h); }
    public static void setMaximized(boolean m) { Config.setWindowMaximized(m); }

    public static int colWidth(int col, int defaultWidth)  { return Config.getColWidth(col, defaultWidth); }
    public static void setColWidths(int[] widths)          { Config.setColWidths(widths); }
    public static boolean colVisible(int col)              { return Config.getColVisible(col); }
    public static void setColVisible(int col, boolean v)  { Config.setColVisible(col, v); }

    public static List<String> listDbProfiles() { return Config.listDbProfiles(); }
}
