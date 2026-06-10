package herramienta;

import herramienta.UITheme.Theme;

/**
 * Typed read/write view of UI-related {@link Config} keys.
 * Internally backed by the UI sub-store introduced with the B1 god-class
 * split.  Callers should prefer these typed setters over {@link Config}'s
 * generic ones — they group related configuration and make the
 * dependency from callers to a specific sub-domain explicit.
 */
public final class UiConfig {
    private UiConfig() {}

    // Read API
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
    public static double defaultValoracio() { return Config.getDefaultValoracio(); }

    // Write API — callers should prefer these over the static setters on Config.
    public static void setTheme(Theme t)             { Config.setTheme(t); }
    public static void setDarkMode(boolean dark)    { Config.setDarkMode(dark); }
    public static void setFontSize(String size)     { Config.setFontSize(size); }
    public static void setViewMode(String mode)     { Config.setViewMode(mode); }
    public static void setGalleryZoom(int zoom)     { Config.setGalleryZoom(zoom); }
    public static void setLang(String lang)         { Config.setLang(lang); }
    public static void setCurrency(String symbol)   { Config.setCurrencySymbol(symbol); }
    public static void setReadingGoal(int goal)     { Config.setReadingGoal(goal); }
    public static void setSortColumn(int col)       { Config.setSortColumn(col); }
    public static void setSortOrder(String order)   { Config.setSortOrder(order); }
    public static void setDefaultValoracio(double v){ Config.setDefaultValoracio(v); }
}
