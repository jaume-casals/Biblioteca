package herramienta;

import java.util.Map;

/** Typed view of preset-related {@link Config} keys. */
public final class PresetStore {
    private PresetStore() {}

    public static int count() { return Config.getPresetCount(); }
    public static String name(int i) { return Config.getPresetName(i); }
    public static void save(String name, Map<String, String> values) { Config.savePreset(name, values); }
    public static void delete(int i) { Config.deletePreset(i); }
}
