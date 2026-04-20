package herramienta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    private static final File FILE = new File(
        System.getProperty("user.home") + "/.biblioteca/config.properties");

    private static final Properties props = new Properties();

    static {
        if (FILE.exists()) {
            try (FileInputStream in = new FileInputStream(FILE)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("darkMode", "false"));
    }

    public static void setDarkMode(boolean dark) {
        props.setProperty("darkMode", String.valueOf(dark));
        save();
    }

    public static String getDbHost() { return props.getProperty("dbHost", "localhost"); }
    public static void setDbHost(String host) { props.setProperty("dbHost", host); save(); }

    public static String getDbUser() { return props.getProperty("dbUser", "user"); }
    public static void setDbUser(String user) { props.setProperty("dbUser", user); save(); }

    public static String getDbPassword() { return props.getProperty("dbPassword", ""); }
    public static void setDbPassword(String pw) { props.setProperty("dbPassword", pw); save(); }

    public static String getFontSize() { return props.getProperty("fontSize", "medium"); }
    public static void setFontSize(String size) { props.setProperty("fontSize", size); save(); }

    // ── Filter presets ────────────────────────────────────────────────────────
    private static final String[] PRESET_KEYS =
        {"nom","autor","isbn","anyMin","anyMax","valoracioMin","valoracioMax","preuMin","preuMax","llegit"};

    public static int getPresetCount() { return parseInt(props.getProperty("presetCount", "0")); }
    public static String getPresetName(int i) { return props.getProperty("preset." + i + ".name", "Preset " + i); }

    public static Map<String, String> loadPreset(int i) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : PRESET_KEYS) m.put(k, props.getProperty("preset." + i + "." + k, ""));
        return m;
    }

    public static void savePreset(String name, Map<String, String> values) {
        int n = getPresetCount();
        props.setProperty("preset." + n + ".name", name);
        for (Map.Entry<String, String> e : values.entrySet())
            props.setProperty("preset." + n + "." + e.getKey(), e.getValue());
        props.setProperty("presetCount", String.valueOf(n + 1));
        save();
    }

    public static void deletePreset(int i) {
        int n = getPresetCount();
        for (int j = i; j < n - 1; j++) {
            props.setProperty("preset." + j + ".name", props.getProperty("preset." + (j+1) + ".name", ""));
            for (String k : PRESET_KEYS)
                props.setProperty("preset." + j + "." + k, props.getProperty("preset." + (j+1) + "." + k, ""));
        }
        for (String k : PRESET_KEYS) props.remove("preset." + (n-1) + "." + k);
        props.remove("preset." + (n-1) + ".name");
        props.setProperty("presetCount", String.valueOf(n - 1));
        save();
    }

    public static String getDefaultImgDir() {
        return props.getProperty("defaultImgDir", System.getProperty("user.home"));
    }
    public static void setDefaultImgDir(String dir) { props.setProperty("defaultImgDir", dir); save(); }

    /** "h2" (embedded, default) or "mariadb" (external server). */
    public static String getDbType() { return props.getProperty("dbType", "h2"); }
    public static void setDbType(String type) { props.setProperty("dbType", type); save(); }

    public static int getWindowX()      { return parseInt(props.getProperty("windowX",      "100"));  }
    public static int getWindowY()      { return parseInt(props.getProperty("windowY",      "100"));  }
    public static int getWindowWidth()  { return parseInt(props.getProperty("windowWidth",  "1280")); }
    public static int getWindowHeight() { return parseInt(props.getProperty("windowHeight", "800"));  }
    public static boolean isWindowMaximized() {
        return Boolean.parseBoolean(props.getProperty("windowMaximized", "true"));
    }

    public static void setWindowBounds(int x, int y, int w, int h) {
        props.setProperty("windowX",      String.valueOf(x));
        props.setProperty("windowY",      String.valueOf(y));
        props.setProperty("windowWidth",  String.valueOf(w));
        props.setProperty("windowHeight", String.valueOf(h));
        save();
    }

    public static void setWindowMaximized(boolean m) {
        props.setProperty("windowMaximized", String.valueOf(m));
        save();
    }

    public static int getColWidth(int col, int defaultWidth) {
        return parseInt(props.getProperty("colWidth_" + col, String.valueOf(defaultWidth)));
    }

    public static void setColWidths(int[] widths) {
        for (int i = 0; i < widths.length; i++)
            props.setProperty("colWidth_" + i, String.valueOf(widths[i]));
        save();
    }

    public static boolean getColVisible(int col) {
        return Boolean.parseBoolean(props.getProperty("colVisible_" + col, "true"));
    }

    public static void setColVisible(int col, boolean visible) {
        props.setProperty("colVisible_" + col, String.valueOf(visible));
        save();
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                props.store(out, "Biblioteca configuration");
            }
        } catch (IOException ignored) {}
    }
}
