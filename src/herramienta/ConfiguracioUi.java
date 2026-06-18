package herramienta;

import herramienta.UITheme.Tema;

/**
 * Vista tipada de lectura/escriptura de les claus de {@link Config} relacionades
 * amb la IU. Internament recolzada pel sub-emmagatzematge UI introduït amb la
 * divisió de la classe-déu B1. Els consumidors haurien de preferir aquests
 * setters tipats sobre els genèrics de {@link Config} — agrupen la configuració
 * relacionada i fan explícita la dependència dels consumidors envers un
 * sub-domini concret.
 */
public final class ConfiguracioUi {
    private ConfiguracioUi() {}

    // API de lectura
    public static Tema theme()        { return Configuracio.obtenirTheme(); }
    public static boolean darkMode()   { return Configuracio.esDarkMode(); }
    public static String fontSize()    { return Configuracio.obtenirFontSize(); }
    public static String viewMode()    { return Configuracio.obtenirViewMode(); }
    public static int galleryZoom()    { return Configuracio.obtenirGalleryZoom(); }
    public static String lang()        { return Configuracio.obtenirLang(); }
    public static String currency()    { return Configuracio.getCurrencySymbol(); }
    public static int readingGoal()    { return Configuracio.obtenirReadingGoal(); }
    public static int ordenarColumn()     { return Configuracio.obtenirSortColumn(); }
    public static String ordenarOrder()   { return Configuracio.getSortOrder(); }
    public static double defaultValoracio() { return Configuracio.obtenirDefaultValoracio(); }

    // API d'escriptura — els consumidors haurien de preferir aquestes
    // abans que els setters estàtics de Config.
    public static void posarTheme(Tema t)             { Configuracio.posarTheme(t); }
    public static void posarDarkMode(boolean dark)    { Configuracio.posarDarkMode(dark); }
    public static void posarFontSize(String size)     { Configuracio.posarFontSize(size); }
    public static void posarViewMode(String mode)     { Configuracio.posarViewMode(mode); }
    public static void posarGalleryZoom(int zoom)     { Configuracio.posarGalleryZoom(zoom); }
    public static void posarLang(String lang)         { Configuracio.posarLang(lang); }
    public static void setCurrency(String symbol)   { Configuracio.setCurrencySymbol(symbol); }
    public static void posarReadingGoal(int goal)     { Configuracio.posarReadingGoal(goal); }
    public static void posarSortColumn(int col)       { Configuracio.posarSortColumn(col); }
    public static void posarSortOrder(String order)   { Configuracio.posarSortOrder(order); }
    public static void posarDefaultValoracio(double v){ Configuracio.posarDefaultValoracio(v); }
}
