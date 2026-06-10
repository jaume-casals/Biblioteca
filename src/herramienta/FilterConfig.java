package herramienta;

/**
 * Typed read/write view of the small "filter / defaults" sub-store of
 * {@link Config} (currently just {@code defaultImgDir}; the preset
 * save/delete APIs remain on {@link Config} directly because they span
 * 17 keys at once).
 */
public final class FilterConfig {
    private FilterConfig() {}

    public static String defaultImgDir()           { return Config.getDefaultImgDir(); }
    public static void setDefaultImgDir(String dir) { Config.setDefaultImgDir(dir); }
}
