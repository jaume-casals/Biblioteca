package herramienta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Config {

    private Config() {}

    private static final Properties props = new Properties();

    /** Reload configuration from disk, overriding in-memory values. For tests. */
    public static void reload() {
        props.clear();
        File cfgFile = new File(System.getProperty("user.home") + "/.biblioteca/config.properties");
        if (cfgFile.exists()) {
            try (FileInputStream in = new FileInputStream(cfgFile)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Config reload failed: " + e.getMessage());
            }
        }
    }

    static {
        File f = currentFile();
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Config load failed: " + e.getMessage());
            }
        }
    }

    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("darkMode", "false"));
    }

    public static void setDarkMode(boolean dark) {
        props.setProperty("darkMode", String.valueOf(dark));
        save();
    }

    public static UITheme.Theme getTheme() {
        String key = props.getProperty("theme", null);
        if (key != null) return UITheme.Theme.fromKey(key);
        return isDarkMode() ? UITheme.Theme.DARK : UITheme.Theme.LIGHT;
    }

    public static void setTheme(UITheme.Theme t) {
        props.setProperty("theme", t.key());
        props.setProperty("darkMode", String.valueOf(t.dark));
        save();
    }

    public static String getDbHost() { return props.getProperty("dbHost", "localhost"); }
    public static void setDbHost(String host) { props.setProperty("dbHost", host); save(); }

    public static String getDbUser() { return props.getProperty("dbUser", "user"); }
    public static void setDbUser(String user) { props.setProperty("dbUser", user); save(); }

    public static String getDbPassword() { return props.getProperty("dbPassword", ""); }
    public static void setDbPassword(String pw) { props.setProperty("dbPassword", pw); save(); }

    private static final java.util.Set<String> VALID_FONT_SIZES = java.util.Set.of("small", "medium", "large");
    public static String getFontSize() { return props.getProperty("fontSize", "medium"); }
    public static void setFontSize(String size) {
        if (!VALID_FONT_SIZES.contains(size)) throw new IllegalArgumentException("Invalid fontSize: " + size + ". Must be small, medium or large.");
        props.setProperty("fontSize", size); save();
    }

    public static String getCurrencySymbol() { return props.getProperty("currencySymbol", "€"); }
    public static void setCurrencySymbol(String s) { props.setProperty("currencySymbol", s != null ? s : "€"); save(); }

    public static double getDefaultValoracio() {
        try { return Double.parseDouble(props.getProperty("defaultValoracio", "0.0")); }
        catch (NumberFormatException e) { return 0.0; }
    }
    public static void setDefaultValoracio(double v) {
        props.setProperty("defaultValoracio", String.valueOf(Math.max(0.0, Math.min(10.0, v))));
        save();
    }

    // ── Filter presets ────────────────────────────────────────────────────────
    private static final String[] PRESET_KEYS =
        {"nom","autor","isbn","anyMin","anyMax","valoracioMin","valoracioMax","preuMin","preuMax","llegit",
         "editorial","serie","idioma","format","tagId","llistaId"};

    public static int getPresetCount() { return parseInt(props.getProperty("presetCount"), 0); }
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
        if (i < 0 || i >= n) return;
        // Read all presets into memory, remove i-th, then re-write
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<Map<String, String>> values = new java.util.ArrayList<>();
        for (int j = 0; j < n; j++) {
            names.add(getPresetName(j));
            values.add(loadPreset(j));
        }
        names.remove(i);
        values.remove(i);
        // Remove all old preset keys
        for (int j = 0; j < n; j++) {
            props.remove("preset." + j + ".name");
            for (String k : PRESET_KEYS) props.remove("preset." + j + "." + k);
        }
        // Write remaining presets back
        for (int j = 0; j < names.size(); j++) {
            props.setProperty("preset." + j + ".name", names.get(j));
            for (Map.Entry<String,String> e : values.get(j).entrySet())
                props.setProperty("preset." + j + "." + e.getKey(), e.getValue());
        }
        props.setProperty("presetCount", String.valueOf(n - 1));
        save();
    }

    public static String getDefaultImgDir() {
        return props.getProperty("defaultImgDir", System.getProperty("user.home"));
    }
    public static void setDefaultImgDir(String dir) { props.setProperty("defaultImgDir", dir); save(); }

    /** Web API port. Default 7070. */
    public static int getApiPort() { return parseInt(props.getProperty("apiPort"), 7070); }
    public static void setApiPort(int port) { props.setProperty("apiPort", String.valueOf(port)); save(); }

    /** Last chosen startup mode ("swing" or "web"). Null if never set. */
    public static String getLastMode() { return props.getProperty("lastMode", null); }
    public static void setLastMode(String mode) {
        if (mode == null) props.remove("lastMode");
        else props.setProperty("lastMode", mode);
        save();
    }

    /** "ca" (default), "es", or "en". */
    public static String getLang() { return props.getProperty("lang", "ca"); }
    public static void setLang(String lang) { props.setProperty("lang", lang); save(); }

    /** "h2" (embedded, default) or "mariadb" (external server). */
    public static String getDbType() { return props.getProperty("dbType", "h2"); }
    public static void setDbType(String type) {
        props.setProperty("dbType", type);
        if ("h2".equals(type)) {
            props.setProperty("dbHost", "localhost");
            props.setProperty("dbUser", "user");
        }
        save();
    }

    /** Directory where automatic and manual backups are stored. */
    public static java.io.File getBackupDir() {
        return new java.io.File(System.getProperty("user.home"), ".biblioteca/backups");
    }

    /** Active H2 profile name (filename without .mv.db). Default "biblioteca". */
    public static String getDbProfile() { return props.getProperty("dbProfile", "biblioteca"); }
    public static void setDbProfile(String name) { props.setProperty("dbProfile", name); save(); }

    /** List all H2 profile files in ~/.biblioteca/. */
    public static java.util.List<String> listDbProfiles() {
        java.io.File dir = new java.io.File(System.getProperty("user.home") + "/.biblioteca");
        java.util.List<String> names = new java.util.ArrayList<>();
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    String n = f.getName();
                    if (n.endsWith(".mv.db")) {
                        String base = n.substring(0, n.length() - 6);
                        if (!names.contains(base)) names.add(base);
                    }
                }
            }
        }
        if (names.isEmpty()) names.add("biblioteca");
        java.util.Collections.sort(names);
        return names;
    }

    public static int getWindowX()      { return parseInt(props.getProperty("windowX"),      100);  }
    public static int getWindowY()      { return parseInt(props.getProperty("windowY"),      100);  }
    public static int getWindowWidth()  { return parseInt(props.getProperty("windowWidth"),  1280); }
    public static int getWindowHeight() { return parseInt(props.getProperty("windowHeight"), 800);  }
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
        return parseInt(props.getProperty("colWidth_" + col), defaultWidth);
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

    public static int getReadingGoal() { return parseInt(props.getProperty("readingGoal"), 0); }
    public static void setReadingGoal(int goal) { props.setProperty("readingGoal", String.valueOf(goal)); save(); }

    public static String getViewMode() {
        return props.getProperty("viewMode", "taula");
    }
    public static void setViewMode(String mode) { props.setProperty("viewMode", mode); save(); }

    /** Gallery zoom level 0–4 (default 2 = 140px wide cards) */
    public static int getGalleryZoom() { return parseInt(props.getProperty("galleryZoom"), 2); }
    public static void setGalleryZoom(int zoom) { props.setProperty("galleryZoom", String.valueOf(zoom)); save(); }

    /** Last sorted column index (-1 = none) */
    public static int getSortColumn() { return parseInt(props.getProperty("sortColumn"), -1); }
    public static void setSortColumn(int col) { props.setProperty("sortColumn", String.valueOf(col)); save(); }

    /** Last sort direction: "ASCENDING" or "DESCENDING" */
    public static String getSortOrder() { return props.getProperty("sortOrder", "ASCENDING"); }
    public static void setSortOrder(String order) { props.setProperty("sortOrder", order); save(); }

    private static int parseInt(String s) {
        return parseInt(s, 0);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static final java.util.concurrent.ScheduledExecutorService SAVE_SCHEDULER =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "config-save");
            t.setDaemon(true);
            return t;
        });
private static volatile boolean batchActive = false;

    public static void withBatch(Runnable action) {
        batchActive = true;
        try {
            action.run();
        } finally {
            batchActive = false;
            save();
        }
    }

    private static volatile java.util.concurrent.ScheduledFuture<?> pendingSave = null;

    private static File currentFile() {
        return new File(System.getProperty("user.home") + "/.biblioteca/config.properties");
    }

    private static void save() {
        if (pendingSave != null) pendingSave.cancel(false);
        pendingSave = SAVE_SCHEDULER.schedule(() -> {
            try {
                File f = currentFile();
                f.getParentFile().mkdirs();
                try (FileOutputStream out = new FileOutputStream(f)) {
                    props.store(out, "Biblioteca configuration");
                }
            } catch (IOException e) {
                System.err.println("Config save failed: " + e.getMessage());
            }
        }, 300, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
