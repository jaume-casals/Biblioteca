package herramienta;

public final class WindowConfig {
    private WindowConfig() {}
    public static int x()              { return Config.getWindowX(); }
    public static int y()              { return Config.getWindowY(); }
    public static int width()          { return Config.getWindowWidth(); }
    public static int height()         { return Config.getWindowHeight(); }
    public static boolean maximized()  { return Config.isWindowMaximized(); }
    public static void setBounds(int x, int y, int w, int h) { Config.setWindowBounds(x, y, w, h); }
    public static void setMaximized(boolean m) { Config.setWindowMaximized(m); }
}
