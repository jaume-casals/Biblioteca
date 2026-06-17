package herramienta;

/**
 * Typed read/write view of the small "filter / defaults" sub-store of
 * {@link Config} (currently just {@code defaultImgDir}; the preset
 * save/delete APIs remain on {@link Config} directly because they span
 * 17 keys at once).
 */
public final class ConfiguracioFiltre {
    private ConfiguracioFiltre() {}

    public static String defaultImgDir()           { return Configuracio.obtenirDefaultImgDir(); }
    public static void posarDefaultImgDir(String dir) { Configuracio.posarDefaultImgDir(dir); }
}
