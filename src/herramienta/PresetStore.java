package herramienta;

import java.util.Map;

/** Vista tipada de les claus de {@link Config} relacionades amb els presets. */
public final class PresetStore {
    private PresetStore() {}

    public static int count() { return Configuracio.obtenirPresetCount(); }
    public static String name(int i) { return Configuracio.obtenirPresetName(i); }
    public static void save(String name, Map<String, String> values) { Configuracio.desarPreset(name, values); }
    public static void delete(int i) { Configuracio.eliminarPreset(i); }
}
