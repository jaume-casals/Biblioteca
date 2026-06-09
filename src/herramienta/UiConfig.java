package herramienta;

import herramienta.UITheme.Theme;

/**
 * Typed read-only view of UI-related {@link Config} keys.
 * Internally backed by the UI sub-store introduced with the B1 god-class
 * split.  Writes are still funnelled through {@link Config}'s static
 * setters for now (callers should prefer those — this class is for
 * discovery).
 */
public final class UiConfig {
    private UiConfig() {}
    public static Theme theme()        { return Config.getTheme(); }
    public static boolean darkMode()   { return Config.isDarkMode(); }
    public static String fontSize()    { return Config.getFontSize(); }
    public static String viewMode()    { return Config.getViewMode(); }
    public static int galleryZoom()    { return Config.getGalleryZoom(); }
    public static String lang()        { return Config.getLang(); }
    public static String currency()    { return Config.getCurrencySymbol(); }
    public static int readingGoal()    { return Config.getReadingGoal(); }
    public static int sortColumn()     { return Config.getSortColumn(); }
    public static String sortOrder()   { return Config.getSortOrder(); }
}
