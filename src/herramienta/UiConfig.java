package herramienta;

import herramienta.UITheme.Theme;

public final class UiConfig {
    private UiConfig() {}
    public static Theme theme()        { return Config.getTheme(); }
    public static boolean darkMode()   { return Config.isDarkMode(); }
    public static String fontSize()    { return Config.getFontSize(); }
    public static String viewMode()    { return Config.getViewMode(); }
    public static int galleryZoom()    { return Config.getGalleryZoom(); }
}
