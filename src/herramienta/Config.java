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
 * <p>El magatzem intern està organitzat en 4 sub-maps (un per domini:
 * UI, DB, Window, Filter) que es carreguen i es serialitzen junts.  El
 * getter/setter API continua sent estàtic sobre {@code Config} per
 * compatibilitat — les classes {@link UiConfig}, {@link DbConfig},
 * {@link WindowConfig} en són vistes tipades.
 *
 * <p>Layout intern (sub-maps):
 * <ul>
 *   <li>{@code UI_STORE}: tema, fontSize, lang, currency, defaultValoracio, presets,
 *       readingGoal, viewMode, galleryZoom, sortColumn, sortOrder</li>
 *   <li>{@code DB_STORE}: dbHost, dbUser, dbPassword, dbType, dbProfile</li>
 *   <li>{@code WINDOW_STORE}: windowX/Y/Width/Height, windowMaximized, colWidth_*, colVisible_*</li>
 *   <li>{@code FILTER_STORE}: defaultImgDir</li>
 * </ul>
 */
public class Config {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Config.class.getName());

    private Config() {}

    private static final ConcurrentHashMap<String, String> UI_STORE     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> DB_STORE     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> WINDOW_STORE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> FILTER_STORE = new ConcurrentHashMap<>();

    /** Domain tag used to route a key to the right sub-store on read/write. */
    public enum Domain { UI, DB, WINDOW, FILTER }

    /** Internal: write a key into a specific domain store. */
    static void putIn(Domain d, String key, String val) {
        switch (d) {
            case UI:     UI_STORE.put(key, val); break;
            case DB:     DB_STORE.put(key, val); break;
            case WINDOW: WINDOW_STORE.put(key, val); break;
            case FILTER: FILTER_STORE.put(key, val); break;
        }
    }

    /** Internal: read a key from a specific domain store. */
    static String getFrom(Domain d, String key) {
        switch (d) {
            case UI:     return UI_STORE.get(key);
            case DB:     return DB_STORE.get(key);
            case WINDOW: return WINDOW_STORE.get(key);
            case FILTER: return FILTER_STORE.get(key);
        }
        return null;
    }

    /** Domain of a key.  Unknown keys land in the FILTER store and are logged
     *  so a typo (e.g. {@code colWidths_X} instead of {@code colWidth_X}) is visible. */
    private static Domain domainOf(String key) {
        if (UI_KEYS.contains(key))         return Domain.UI;
        if (DB_KEYS.contains(key))         return Domain.DB;
        if (WINDOW_KEYS.contains(key))     return Domain.WINDOW;
        if (FILTER_KEYS.contains(key))     return Domain.FILTER;
        if (key.startsWith("colWidth_") || key.startsWith("colVisible_")) return Domain.WINDOW;
        if (key.startsWith("preset."))     return Domain.UI;
        LOG.log(java.util.logging.Level.WARNING, "Config key \"{0}\" has no matching domain; routing to FILTER (possible typo?)", key);
        return Domain.FILTER;
    }

    private static final java.util.Set<String> UI_KEYS = java.util.Set.of(
        "theme", "darkMode", "fontSize", "lang", "currencySymbol",
        "defaultValoracio", "presetCount", "readingGoal", "viewMode",
        "galleryZoom", "sortColumn", "sortOrder");
    private static final java.util.Set<String> DB_KEYS = java.util.Set.of(
        "dbHost", "dbUser", "dbPassword", "dbType", "dbProfile");
    private static final java.util.Set<String> WINDOW_KEYS = java.util.Set.of(
        "windowX", "windowY", "windowWidth", "windowHeight", "windowMaximized");
    private static final java.util.Set<String> FILTER_KEYS = java.util.Set.of(
        "defaultImgDir");

    /** Composite view used by the existing file-load/save paths.  Iterating
     *  it walks all 4 sub-stores; puts route to the domain-aware store. */
    static final Map<String, String> props = new java.util.AbstractMap<String, String>() {
        @Override public String get(Object key) {
            if (key == null) return null;
            Domain d = domainOf((String) key);
            return getFrom(d, (String) key);
        }
        @Override public String put(String key, String val) {
            String prev = get(key);
            putIn(domainOf(key), key, val);
            return prev;
        }
        @Override public java.util.Set<java.util.Map.Entry<String, String>> entrySet() {
            java.util.Set<java.util.Map.Entry<String, String>> all = new java.util.HashSet<>();
            all.addAll(UI_STORE.entrySet());
            all.addAll(DB_STORE.entrySet());
            all.addAll(WINDOW_STORE.entrySet());
            all.addAll(FILTER_STORE.entrySet());
            return all;
        }
        @Override public int size() {
            return UI_STORE.size() + DB_STORE.size() + WINDOW_STORE.size() + FILTER_STORE.size();
        }
    };

    /**
     * Snapshot the composite key/value view as a real {@link Properties}.
     * Useful for callers that need the JDK {@code Properties} contract
     * (XML I/O, {@code System.getProperties()}-style iteration, etc.) but
     * don't want to interact with the 4 sub-stores directly.  Returns a
     * fresh, independent copy — mutations to the returned object do NOT
     * affect the live configuration and vice versa.
     */
    public static java.util.Properties toProperties() {
        java.util.Properties p = new java.util.Properties();
        p.putAll(props);
        return p;
    }

    /** Bulk-apply the entries of {@code src} into the live configuration,
     *  routing each key to its sub-store via {@link #domainOf(String)}.
     *  This is a write-through: the file save is NOT triggered here — call
     *  {@link #save()} or {@link #withBatch} if you also want a flush. */
    public static void fromProperties(java.util.Properties src) {
        for (String name : src.stringPropertyNames()) {
            props.put(name, src.getProperty(name));
        }
    }

    public static void reload() {
        props.clear();
        loadFromDisk();
    }

    static { loadFromDisk(); }

    private static void loadFromDisk() {
        File f = currentFile();
        if (!f.exists()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            java.util.Properties tmp = new java.util.Properties();
            tmp.load(in);
            tmp.forEach((k, v) -> props.put((String) k, (String) v));
        } catch (IOException e) {
            LOG.log(java.util.logging.Level.WARNING, "Config load failed", e);
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

    /**
     * Returns the DB password.  <b>Note:</b> the password is stored in
     * {@code ~/.biblioteca/config.properties} in plaintext.  This is
     * acceptable for a local single-user desktop app (config file
     * permissions are the user's responsibility) but should NOT be relied
     * on for any multi-user or networked deployment.
     * <p>
     * <b>Exposure surface:</b>
     * <ul>
     *   <li>Anyone with read access to {@code config.properties} sees the password.</li>
     *   <li>Backup tools that include the home directory will include the password.</li>
     *   <li>On shared hosts, any user on the same machine can read the file.</li>
     * </ul>
     * <b>Migration paths (not implemented — see tot.txt B2):</b>
     * <ul>
     *   <li>OS keystore (Keychain / Credential Manager / Secret Service) keyed by
     *       the local user account.</li>
     *   <li>{@code jasypt} or similar with a master password derived from the
     *       OS user — still recoverable, but obscures casual disk inspection.</li>
     *   <li>Switch to integrated auth where the DB driver picks up the OS user
     *       (MySQL {@code --skip-grant-tables}, MariaDB {@code unix_socket}).</li>
     * </ul>
     */
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

    public static void withBatch(Runnable action) {
        try {
            action.run();
        } finally {
            save();
        }
    }

    private static volatile ScheduledFuture<?> pendingSave = null;

    private static File currentFile() {
        return new File(System.getProperty("user.home") + "/.biblioteca/config.properties");
    }

    private static void save() {
        if (pendingSave != null) pendingSave.cancel(false);
        pendingSave = SAVE_SCHEDULER.schedule(() -> doSave(currentFile(), "Config save failed: "), 300, TimeUnit.MILLISECONDS);
    }

    public static void flushNow() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
            pendingSave = null;
        }
        doSave(currentFile(), "Config flush failed: ");
    }

    private static void doSave(File f, String errPrefix) {
        try {
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
            LOG.log(java.util.logging.Level.WARNING, errPrefix, e);
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flushNow();
            SAVE_SCHEDULER.shutdownNow();
        }, "config-shutdown"));
    }
}
