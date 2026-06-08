package herramienta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Façana estàtica sobre les propietats de configuració (tema, idioma, BBDD,
 * geometria de finestra, etc.).
 *
 * <p><b>TODO (refactor):</b> 303 línies, getters/setters plans per a 4
 * dominis diferents (UI, DB, Window, Filter). La refactorització natural
 * seria:
 * <ul>
 *   <li>{@code UIConfig}: tema, fontSize, idioma, currency, defaultValoracio, presets</li>
 *   <li>{@code DBConfig}: dbHost, dbUser, dbPassword, dbType, dbPath, profiles</li>
 *   <li>{@code WindowConfig}: windowX/Y/Width/Height, maximitzada, darrera pestanya</li>
 *   <li>{@code FilterConfig}: defaultImgDir, defaultSearchMode, presetCount</li>
 * </ul>
 * {@code Config} es queda com a façana que delega. Fins llavors, els
 * comentaris de secció més avall permeten navegar mentalment quin getter
 * pertany a quin sub-domini.
 */
public class Config {

    private Config() {}

    private static final ConcurrentHashMap<String, String> props = new ConcurrentHashMap<>();

    public static void reload() {
        props.clear();
        File cfgFile = currentFile();
        if (cfgFile.exists()) {
            try (FileInputStream in = new FileInputStream(cfgFile)) {
                java.util.Properties tmp = new java.util.Properties();
                tmp.load(in);
                tmp.forEach((k, v) -> props.put((String) k, (String) v));
            } catch (IOException e) {
                System.err.println("Config reload failed: " + e.getMessage());
            }
        }
    }

    static {
        File f = currentFile();
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                java.util.Properties tmp = new java.util.Properties();
                tmp.load(in);
                tmp.forEach((k, v) -> props.put((String) k, (String) v));
            } catch (IOException e) {
                System.err.println("Config load failed: " + e.getMessage());
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getOrDefault("darkMode", "false"));
    }

    public static void setDarkMode(boolean dark) {
        props.put("darkMode", String.valueOf(dark));
        save();
    }

    public static UITheme.Theme getTheme() {
        String key = props.get("theme");
        if (key != null) return UITheme.Theme.fromKey(key);
        return isDarkMode() ? UITheme.Theme.DARK : UITheme.Theme.LIGHT;
    }

    public static void setTheme(UITheme.Theme t) {
        props.put("theme", t.key());
        props.put("darkMode", String.valueOf(t.dark));
        save();
    }

    // ── DB ────────────────────────────────────────────────────────────────────
    public static String getDbHost() { return props.getOrDefault("dbHost", "localhost"); }
    public static void setDbHost(String host) { props.put("dbHost", host); save(); }

    public static String getDbUser() { return props.getOrDefault("dbUser", "user"); }
    public static void setDbUser(String user) { props.put("dbUser", user); save(); }

    public static String getDbPassword() { return props.getOrDefault("dbPassword", ""); }

    /** Distingeix "no configurat" (retorna false) de "password buit" (true). */
    public static boolean hasDbPassword() { return props.containsKey("dbPassword"); }
    public static void setDbPassword(String pw) { props.put("dbPassword", pw); save(); }

    private static final java.util.Set<String> VALID_FONT_SIZES = java.util.Set.of("small", "medium", "large");
    public static String getFontSize() { return props.getOrDefault("fontSize", "medium"); }
    public static void setFontSize(String size) {
        if (!VALID_FONT_SIZES.contains(size)) throw new IllegalArgumentException("Invalid fontSize: " + size + ". Must be small, medium or large.");
        props.put("fontSize", size); save();
    }

    public static String getCurrencySymbol() { return props.getOrDefault("currencySymbol", "€"); }
    public static void setCurrencySymbol(String s) { props.put("currencySymbol", s != null ? s : "€"); save(); }

    public static double getDefaultValoracio() {
        try { return Double.parseDouble(props.getOrDefault("defaultValoracio", "0.0")); }
        catch (NumberFormatException e) { return 0.0; }
    }
    public static void setDefaultValoracio(double v) {
        props.put("defaultValoracio", String.valueOf(Math.max(0.0, Math.min(10.0, v))));
        save();
    }

    private static final String[] PRESET_KEYS =
        {"nom","autor","isbn","anyMin","anyMax","valoracioMin","valoracioMax","preuMin","preuMax","llegit",
         "editorial","serie","idioma","format","tagId","llistaId"};

    public static int getPresetCount() { return parseInt(props.get("presetCount"), 0); }
    public static String getPresetName(int i) { return props.getOrDefault("preset." + i + ".name", "Preset " + i); }

    public static Map<String, String> loadPreset(int i) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : PRESET_KEYS) m.put(k, props.getOrDefault("preset." + i + "." + k, ""));
        return m;
    }

    public static void savePreset(String name, Map<String, String> values) {
        int n = getPresetCount();
        props.compute("preset." + n + ".name", (k, ov) -> name);
        for (Map.Entry<String, String> e : values.entrySet())
            props.put("preset." + n + "." + e.getKey(), e.getValue());
        props.put("presetCount", String.valueOf(n + 1));
        save();
    }

    public static void deletePreset(int i) {
        int n = getPresetCount();
        if (i < 0 || i >= n) return;
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<Map<String, String>> values = new java.util.ArrayList<>();
        for (int j = 0; j < n; j++) {
            names.add(getPresetName(j));
            values.add(loadPreset(j));
        }
        names.remove(i);
        values.remove(i);
        for (int j = 0; j < n; j++) {
            props.remove("preset." + j + ".name");
            for (String k : PRESET_KEYS) props.remove("preset." + j + "." + k);
        }
        for (int j = 0; j < names.size(); j++) {
            props.put("preset." + j + ".name", names.get(j));
            for (Map.Entry<String,String> e : values.get(j).entrySet())
                props.put("preset." + j + "." + e.getKey(), e.getValue());
        }
        props.put("presetCount", String.valueOf(n - 1));
        save();
    }

    // ── Filter defaults ───────────────────────────────────────────────────────
    public static String getDefaultImgDir() {
        return props.getOrDefault("defaultImgDir", System.getProperty("user.home"));
    }
    public static void setDefaultImgDir(String dir) { props.put("defaultImgDir", dir); save(); }

    public static String getLang() { return props.getOrDefault("lang", "ca"); }
    public static void setLang(String lang) { props.put("lang", lang); save(); }

    public static String getDbType() { return props.getOrDefault("dbType", "h2"); }
    public static void setDbType(String type) {
        props.put("dbType", type);
        if ("h2".equals(type)) {
            props.put("dbHost", "localhost");
            props.put("dbUser", "user");
        }
        save();
    }

    public static java.io.File getBackupDir() {
        return new java.io.File(System.getProperty("user.home"), ".biblioteca/backups");
    }

    public static String getDbProfile() { return props.getOrDefault("dbProfile", "biblioteca"); }
    public static void setDbProfile(String name) { props.put("dbProfile", name); save(); }

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

    // ── Window / geometry ────────────────────────────────────────────────────
    public static int getWindowX()      { return parseInt(props.get("windowX"),      100);  }
    public static int getWindowY()      { return parseInt(props.get("windowY"),      100);  }
    public static int getWindowWidth()  { return parseInt(props.get("windowWidth"),  1280); }
    public static int getWindowHeight() { return parseInt(props.get("windowHeight"), 800);  }
    public static boolean isWindowMaximized() {
        return Boolean.parseBoolean(props.getOrDefault("windowMaximized", "true"));
    }

    public static void setWindowBounds(int x, int y, int w, int h) {
        props.put("windowX",      String.valueOf(x));
        props.put("windowY",      String.valueOf(y));
        props.put("windowWidth",  String.valueOf(w));
        props.put("windowHeight", String.valueOf(h));
        save();
    }

    public static void setWindowMaximized(boolean m) {
        props.put("windowMaximized", String.valueOf(m));
        save();
    }

    public static int getColWidth(int col, int defaultWidth) {
        return parseInt(props.get("colWidth_" + col), defaultWidth);
    }

    public static void setColWidths(int[] widths) {
        for (int i = 0; i < widths.length; i++)
            props.put("colWidth_" + i, String.valueOf(widths[i]));
        save();
    }

    public static boolean getColVisible(int col) {
        return Boolean.parseBoolean(props.getOrDefault("colVisible_" + col, "true"));
    }

    public static void setColVisible(int col, boolean visible) {
        props.put("colVisible_" + col, String.valueOf(visible));
        save();
    }

    public static int getReadingGoal() { return parseInt(props.get("readingGoal"), 0); }
    public static void setReadingGoal(int goal) { props.put("readingGoal", String.valueOf(goal)); save(); }

    public static String getViewMode() {
        return props.getOrDefault("viewMode", "taula");
    }
    public static void setViewMode(String mode) { props.put("viewMode", mode); save(); }

    public static int getGalleryZoom() { return parseInt(props.get("galleryZoom"), 2); }
    public static void setGalleryZoom(int zoom) { props.put("galleryZoom", String.valueOf(zoom)); save(); }

    public static int getSortColumn() { return parseInt(props.get("sortColumn"), -1); }
    public static void setSortColumn(int col) { props.put("sortColumn", String.valueOf(col)); save(); }

    public static String getSortOrder() { return props.getOrDefault("sortOrder", "ASCENDING"); }
    public static void setSortOrder(String order) { props.put("sortOrder", order); save(); }

    private static int parseInt(String s) {
        return parseInt(s, 0);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static final ScheduledExecutorService SAVE_SCHEDULER =
        Executors.newSingleThreadScheduledExecutor(r -> {
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

    private static volatile ScheduledFuture<?> pendingSave = null;

    private static File currentFile() {
        return new File(System.getProperty("user.home") + "/.biblioteca/config.properties");
    }

    private static void save() {
        if (pendingSave != null) pendingSave.cancel(false);
        pendingSave = SAVE_SCHEDULER.schedule(() -> {
            try {
                File f = currentFile();
                f.getParentFile().mkdirs();
                java.util.Properties tmp = new java.util.Properties();
                props.forEach((k, v) -> tmp.put(k, v));
                File tmpFile = new File(f.getParentFile(), f.getName() + ".tmp");
                try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                    tmp.store(out, "Biblioteca configuration");
                }
                java.nio.file.Files.move(tmpFile.toPath(), f.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                System.err.println("Config save failed: " + e.getMessage());
            }
        }, 300, TimeUnit.MILLISECONDS);
    }

    public static void flushNow() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
            pendingSave = null;
        }
        try {
            File f = currentFile();
            f.getParentFile().mkdirs();
            java.util.Properties tmp = new java.util.Properties();
            props.forEach((k, v) -> tmp.put(k, v));
            File tmpFile = new File(f.getParentFile(), f.getName() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                tmp.store(out, "Biblioteca configuration");
            }
            java.nio.file.Files.move(tmpFile.toPath(), f.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Config flush failed: " + e.getMessage());
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flushNow();
            SAVE_SCHEDULER.shutdownNow();
        }, "config-shutdown"));
    }
}
