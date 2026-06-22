package herramienta.config;

import herramienta.ui.UITheme;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
public class Configuracio {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Configuracio.class.getName());

    private Configuracio() {}

    /** Directori arrel de l'aplicació sota {@code $HOME}. Compartit amb
     *  {@link DialegError} i {@link ServeiCoberta} per evitar paths
     *  hardcoded duplicats. Retorna un camí nou a cada crida perquè els
     *  tests que posen {@code user.home} a {@code System} vegin el valor
     *  actualitzat (un {@code static final} seria inicialitzat un cop
     *  quan es carrega la classe i no es reevaluaria). */
    public static java.nio.file.Path bibliotecaDir() {
        return java.nio.file.Path.of(System.getProperty("user.home"), ".biblioteca");
    }

    private static final ConcurrentHashMap<String, String> UI_STORE     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> DB_STORE     = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> WINDOW_STORE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> FILTER_STORE = new ConcurrentHashMap<>();

    /** Emmagatzematge transitori de la contrasenya com a {@code char[]} (no a
     *  {@code props}) perquè la representació al heap no sigui mai una còpia
     *  immortal en forma de {@code String}. */
    private static volatile char[] dbPasswordChars = null;

    /** Etiqueta de domini usada per encaminar una clau al sub-store correcte en lectura/escriptura. */
    public enum Domini { UI, DB, WINDOW, FILTER }

    /** Intern: escriu una clau en un store de domini concret. */
    static void putIn(Domini d, String key, String val) {
        switch (d) {
            case UI:     UI_STORE.put(key, val); break;
            case DB:     DB_STORE.put(key, val); break;
            case WINDOW: WINDOW_STORE.put(key, val); break;
            case FILTER: FILTER_STORE.put(key, val); break;
        }
    }

    /** Intern: llegeix una clau d'un store de domini concret. */
    static String obtenirFrom(Domini d, String key) {
        switch (d) {
            case UI:     return UI_STORE.get(key);
            case DB:     return DB_STORE.get(key);
            case WINDOW: return WINDOW_STORE.get(key);
            case FILTER: return FILTER_STORE.get(key);
        }
        return null;
    }

    /** Domini d'una clau. Les claus desconegudes van al store FILTER i es
     *  registren perquè un error tipogràfic (p. ex. {@code colWidths_X} en
     *  lloc de {@code colWidth_X}) sigui visible. */
    private static Domini domainOf(String key) {
        if (UI_KEYS.contains(key))         return Domini.UI;
        if (DB_KEYS.contains(key))         return Domini.DB;
        if (WINDOW_KEYS.contains(key))     return Domini.WINDOW;
        if (FILTER_KEYS.contains(key))     return Domini.FILTER;
        if (key.startsWith("colWidth_") || key.startsWith("colVisible_")) return Domini.WINDOW;
        if (key.startsWith("preset."))     return Domini.UI;
        LOG.log(java.util.logging.Level.WARNING, "Config key \"{0}\" has no matching domain; routing to FILTER (possible typo?)", key);
        return Domini.FILTER;
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
        "defaultImgDir", "lastMode");

    /** Vista composta que usen els camins de càrrega/desada de fitxers
     *  existents. Iterar-la recorre els 4 sub-stores; els puts s'encaminen
     *  al store corresponent al domini. */
    static final Map<String, String> props = new java.util.AbstractMap<String, String>() {
        @Override public String get(Object key) {
            if (key == null) return null;
            Domini d = domainOf((String) key);
            return obtenirFrom(d, (String) key);
        }
        @Override public String put(String key, String val) {
            String prev = get(key);
            putIn(domainOf(key), key, val);
            return prev;
        }
        @Override public java.util.Set<java.util.Map.Entry<String, String>> entrySet() {
            java.util.Set<java.util.Map.Entry<String, String>> all = new java.util.LinkedHashSet<>();
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
     * Captura la vista composta de clau/valor com a {@link Properties}
     * real. Útil per a consumidors que necessiten el contracte
     * {@code Properties} del JDK (I/O XML, iteració estil
     * {@code System.getProperties()}, etc.) però no volen interactuar
     * directament amb els 4 sub-stores. Retorna una còpia independent
     * nova — les mutacions de l'objecte retornat NO afecten la
     * configuració viva ni a l'inrevés.
     */
    public static java.util.Properties toProperties() {
        java.util.Properties p = new java.util.Properties();
        p.putAll(props);
        return p;
    }

    /** Aplica en lot les entrades de {@code src} a la configuració viva,
     *  encaminant cada clau al seu sub-store via {@link #domainOf(String)}.
     *  Això és un write-through: aquí NO es desencadena el desat a
     *  fitxer — crida {@link #save()} o {@link #withBatch} si també
     *  vols un flush. */
    public static void fromProperties(java.util.Properties src) {
        for (String name : src.stringPropertyNames()) {
            props.put(name, src.getProperty(name));
        }
    }

    public static void reload() {
        props.clear();
        carregarFromDisk();
    }

    static { carregarFromDisk(); }

    private static void carregarFromDisk() {
        File f = currentFile();
        if (!f.exists()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            java.util.Properties tmp = new java.util.Properties();
            tmp.load(in);
            tmp.forEach((k, v) -> {
                String key = (String) k;
                String val = (String) v;
                if ("dbPassword".equals(key)) {
                    dbPasswordChars = val.toCharArray();
                } else {
                    props.put(key, val);
                }
            });
        } catch (IOException e) {
            LOG.log(java.util.logging.Level.WARNING, "Ha fallat la càrrega de la configuració", e);
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    public static boolean esDarkMode() {
        return Boolean.parseBoolean(props.getOrDefault("darkMode", "false"));
    }

    public static void posarDarkMode(boolean dark) {
        props.put("darkMode", String.valueOf(dark));
        save();
    }

    public static UITheme.Tema obtenirTheme() {
        String key = props.get("theme");
        if (key != null) return UITheme.Tema.fromKey(key);
        return esDarkMode() ? UITheme.Tema.DARK : UITheme.Tema.LIGHT;
    }

    public static void posarTheme(UITheme.Tema t) {
        props.put("theme", t.key());
        props.put("darkMode", String.valueOf(t.dark));
        save();
    }

    // ── DB ────────────────────────────────────────────────────────────────────
    public static String obtenirDbHost() { return props.getOrDefault("dbHost", "localhost"); }
    public static void posarDbHost(String host) { props.put("dbHost", host); save(); }

    public static String obtenirDbUser() { return props.getOrDefault("dbUser", "user"); }
    public static void posarDbUser(String user) { props.put("dbUser", user); save(); }

    /**
     * Retorna la contrasenya de la BBDD. <b>Nota:</b> la contrasenya
     * s'emmagatzema a {@code ~/.biblioteca/config.properties} en text
     * pla. Això és acceptable per a una aplicació d'escriptori local
     * d'un sol usuari (els permisos del fitxer de configuració són
     * responsabilitat de l'usuari), però NO s'hauria de confiar per a
     * cap desplegament multiusuari o en xarxa.
     * <p>
     * <b>Superfície d'exposició:</b>
     * <ul>
     *   <li>Qualsevol persona amb accés de lectura a {@code config.properties} veu la contrasenya.</li>
     *   <li>Les eines de còpia que inclouen el directori home inclouran la contrasenya.</li>
     *   <li>En hosts compartits, qualsevol usuari de la mateixa màquina pot llegir el fitxer.</li>
     * </ul>
     * <b>Camins de migració (no implementats — veure tot.txt B2):</b>
     * <ul>
     *   <li>Keystore del SO (Keychain / Credential Manager / Secret Service) indexat
     *       pel compte d'usuari local.</li>
     *   <li>{@code jasypt} o similar amb una contrasenya mestra derivada de
     *       l'usuari del SO — encara recuperable, però oculta la inspecció
     *       casual del disc.</li>
     *   <li>Canviar a auth integrada on el driver de la BBDD recull l'usuari del SO
     *       (MySQL {@code --skip-grant-tables}, MariaDB {@code unix_socket}).</li>
     * </ul>
     * <p>
     * <b>Heap leak:</b> Aquest mètode materialitza la contrasenya com a
     * {@code String} per satisfer la signatura pública. La {@code String}
     * resultant viu al heap fins a la propera GC i pot ser internada per
     * {@code String.intern()} o capturada per qualsevol heap-dumper.
     * Els consumidors que ho evitin haurien de cridar
     * {@link #obtenirDbPasswordChars()} i gestionar el {@code char[]} ells
     * mateixos (per exemple, passant-lo directament a
     * {@code JPasswordField(char[])}).
     */
    public static String obtenirDbPassword() {
        if (dbPasswordChars != null) return new String(dbPasswordChars);
        return "";
    }

    /** Retorna una còpia defensiva de la contrasenya com a {@code char[]}.
     *  El consumidor és responsable de posar l'array a zero (per exemple,
     *  amb {@link java.util.Arrays#fill(char[], char)}) quan hagi acabat,
     *  i NO ha de convertir-lo a {@code String}. */
    public static char[] obtenirDbPasswordChars() {
        char[] src = dbPasswordChars;
        return src == null ? new char[0] : src.clone();
    }

    /** Distingeix "no configurat" (retorna false) de "password buit" (true). */
    public static boolean teDbPassword() { return dbPasswordChars != null; }
    public static void posarDbPassword(String pw) {
        if (pw == null || pw.isEmpty()) {
            dbPasswordChars = null;
        } else {
            dbPasswordChars = pw.toCharArray();
        }
        save();
    }

    /** Variant que accepta la contrasenya com a {@code char[]} perquè el
     *  consumidor la pugui posar a zero després de la crida en lloc de
     *  deixar un {@code String} al heap. El {@code char[]} es clona a
     *  un camp privat i l'array del consumidor es posa a zero abans
     *  que el mètode retorni. El text pla al disc no queda afectat
     *  (això és només protecció al heap). */
    public static void posarDbPassword(char[] pw) {
        if (pw == null) {
            dbPasswordChars = null;
        } else {
            char[] copy = pw.clone();
            java.util.Arrays.fill(pw, '\0');
            dbPasswordChars = copy;
        }
        save();
    }

    private static final java.util.Set<String> VALID_FONT_SIZES = java.util.Set.of("small", "medium", "large");
    public static String obtenirFontSize() { return props.getOrDefault("fontSize", "medium"); }
    public static void posarFontSize(String size) {
        if (!VALID_FONT_SIZES.contains(size)) throw new IllegalArgumentException("Mida de lletra no vàlida: " + size + ". Ha de ser petita, mitjana o gran.");
        props.put("fontSize", size); save();
    }

    public static String getCurrencySymbol() { return props.getOrDefault("currencySymbol", "€"); }
    public static void setCurrencySymbol(String s) { props.put("currencySymbol", s != null ? s : "€"); save(); }

    public static double obtenirDefaultValoracio() {
        try { return Double.parseDouble(props.getOrDefault("defaultValoracio", "0.0")); }
        catch (NumberFormatException e) { return 0.0; }
    }
    public static void posarDefaultValoracio(double v) {
        props.put("defaultValoracio", String.valueOf(Math.max(0.0, Math.min(10.0, v))));
        save();
    }

    private static final String[] PRESET_KEYS =
        {"nom","autor","isbn","anyMin","anyMax","valoracioMin","valoracioMax","preuMin","preuMax","llegit",
         "editorial","serie","idioma","format","tagId","llistaId"};

    public static int obtenirPresetCount() { return parseInt(props.get("presetCount"), 0); }
    public static String obtenirPresetName(int i) { return props.getOrDefault("preset." + i + ".name", "Preset " + i); }

    public static Map<String, String> carregarPreset(int i) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : PRESET_KEYS) m.put(k, props.getOrDefault("preset." + i + "." + k, ""));
        return m;
    }

    public static void desarPreset(String name, Map<String, String> values) {
        int n = obtenirPresetCount();
        props.compute("preset." + n + ".name", (k, ov) -> name);
        for (Map.Entry<String, String> e : values.entrySet())
            props.put("preset." + n + "." + e.getKey(), e.getValue());
        props.put("presetCount", String.valueOf(n + 1));
        save();
    }

    public static void eliminarPreset(int i) {
        int n = obtenirPresetCount();
        if (i < 0 || i >= n) return;
        // Captura el nom + valors dels presets restants per endavant,
        // després esborra totes les claus preset.* (inclosa la ranura
        // eliminada) i reescriu la captura als nous índexs compactats.
        // La lectura basada en stream evita la trampa de "treu de dues
        // ArrayLists en paral·lel" de la versió anterior.
        java.util.stream.IntStream.range(0, n)
            .filter(j -> j != i)
            .forEach(srcIdx -> {
                int dstIdx = srcIdx < i ? srcIdx : srcIdx - 1;
                props.put("preset." + dstIdx + ".name", obtenirPresetName(srcIdx));
                for (Map.Entry<String, String> e : carregarPreset(srcIdx).entrySet())
                    props.put("preset." + dstIdx + "." + e.getKey(), e.getValue());
            });
        // Neteja la ranura de la cua perquè un preset futur no hereti
        // claus obsoletes.
        props.remove("preset." + (n - 1) + ".name");
        for (String k : PRESET_KEYS) props.remove("preset." + (n - 1) + "." + k);
        props.put("presetCount", String.valueOf(n - 1));
        save();
    }

    // ── Filter defaults ───────────────────────────────────────────────────────
    public static String obtenirDefaultImgDir() {
        return props.getOrDefault("defaultImgDir", System.getProperty("user.home"));
    }
    public static void posarDefaultImgDir(String dir) { props.put("defaultImgDir", dir); save(); }

    public static String obtenirLang() { return props.getOrDefault("lang", "ca"); }
    public static void posarLang(String lang) { props.put("lang", lang); save(); }

    public static String obtenirDbType() { return props.getOrDefault("dbType", "h2"); }
    public static void posarDbType(String type) {
        props.put("dbType", type);
        if ("h2".equals(type)) {
            // Inicialitza els valors per defecte de localhost/user només
            // si no n'hi ha cap de definit — altrament, en tornar de
            // MariaDB a H2, sobreescriuria un host/user de MariaDB no
            // per defecte que l'usuari pot voler conservar per a una
            // propera reconnexió.
            props.putIfAbsent("dbHost", "localhost");
            props.putIfAbsent("dbUser", "user");
        }
        save();
    }

    public static java.io.File obtenirBackupDir() {
        return bibliotecaDir().resolve("backups").toFile();
    }

    public static String obtenirDbProfile() { return props.getOrDefault("dbProfile", "biblioteca"); }
    public static void posarDbProfile(String name) {
        if (name != null && !name.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalArgumentException("invalid dbProfile: " + name);
        props.put("dbProfile", name);
        save();
    }

    public static java.util.List<String> listDbProfiles() {
        java.io.File dir = bibliotecaDir().toFile();
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
    public static int obtenirWindowX()      { return parseInt(props.get("windowX"),      100);  }
    public static int obtenirWindowY()      { return parseInt(props.get("windowY"),      100);  }
    public static int obtenirWindowWidth()  { return parseInt(props.get("windowWidth"),  1280); }
    public static int obtenirWindowHeight() { return parseInt(props.get("windowHeight"), 800);  }
    public static boolean esWindowMaximized() {
        return Boolean.parseBoolean(props.getOrDefault("windowMaximized", "true"));
    }

    public static void posarWindowBounds(int x, int y, int w, int h) {
        props.put("windowX",      String.valueOf(x));
        props.put("windowY",      String.valueOf(y));
        props.put("windowWidth",  String.valueOf(w));
        props.put("windowHeight", String.valueOf(h));
        save();
    }

    public static void posarWindowMaximized(boolean m) {
        props.put("windowMaximized", String.valueOf(m));
        save();
    }

    public static int obtenirColWidth(int col, int defaultWidth) {
        return parseInt(props.get("colWidth_" + col), defaultWidth);
    }

    public static void posarColWidths(int[] widths) {
        for (int i = 0; i < widths.length; i++)
            props.put("colWidth_" + i, String.valueOf(widths[i]));
        save();
    }

    public static boolean obtenirColVisible(int col) {
        return Boolean.parseBoolean(props.getOrDefault("colVisible_" + col, "true"));
    }

    public static void posarColVisible(int col, boolean visible) {
        props.put("colVisible_" + col, String.valueOf(visible));
        save();
    }

    public static int obtenirReadingGoal() { return parseInt(props.get("readingGoal"), 0); }
    public static void posarReadingGoal(int goal) { props.put("readingGoal", String.valueOf(goal)); save(); }

    public static String obtenirViewMode() {
        return props.getOrDefault("viewMode", "taula");
    }
    public static void posarViewMode(String mode) { props.put("viewMode", mode); save(); }

    public static int obtenirGalleryZoom() { return parseInt(props.get("galleryZoom"), 2); }
    public static void posarGalleryZoom(int zoom) { props.put("galleryZoom", String.valueOf(zoom)); save(); }

    public static int obtenirSortColumn() { return parseInt(props.get("sortColumn"), -1); }
    public static void posarSortColumn(int col) { props.put("sortColumn", String.valueOf(col)); save(); }

    public static String getSortOrder() { return props.getOrDefault("sortOrder", "ASCENDING"); }
    public static void posarSortOrder(String order) { props.put("sortOrder", order); save(); }

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
            // Fa servir flushNow (síncron) en lloc de save (debounce de
            // 300 ms) perquè el consumidor pugui confiar que l'estat al
            // disc coincideix amb l'estat en memòria quan el batch
            // retorna. El debounce s'heretava dels setXxx (escriptures
            // d'una sola clau des de l'EDT, on una espera de 300 ms és
            // invisible) però és incorrecte per a withBatch: la
            // implementació original obligava el consumidor a esperar
            // fins a 300 ms I deixava una finestra on la JVM podia
            // sortir amb un desat pendent.
            buidarNow();
        }
    }

    private static volatile ScheduledFuture<?> pendingSave = null;

    private static File currentFile() {
        return bibliotecaDir().resolve("config.properties").toFile();
    }

    private static void save() {
        synchronized (SAVE_SCHEDULER) {
            if (pendingSave != null) pendingSave.cancel(false);
            pendingSave = SAVE_SCHEDULER.schedule(() -> doSave(currentFile(), "Ha fallat el desament de la configuració: "), 300, TimeUnit.MILLISECONDS);
        }
    }

    public static void buidarNow() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
            pendingSave = null;
        }
        doSave(currentFile(), "Ha fallat el buidatge de la configuració: ");
    }

    private static void doSave(File f, String errPrefix) {
        try {
            f.getParentFile().mkdirs();
            java.util.Properties tmp = new java.util.Properties();
            props.forEach((k, v) -> tmp.put(k, v));
            if (dbPasswordChars != null) {
                tmp.put("dbPassword", new String(dbPasswordChars));
            }
            File tmpFile = new File(f.getParentFile(), f.getName() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                tmp.store(out, "Biblioteca configuration");
            }
            // Defensa en profunditat: el fitxer de configuració pot dur
            // la contrasenya de la BBDD en text pla (veure el Javadoc
            // de getDbPassword). En sistemes de fitxers POSIX forcem
            // 0600 (lectura/escriptura del propietari) perquè un
            // `ls -l` casual en un host compartit no l'exposi. El
            // Javadoc de getDbPassword reconeix el text pla; aquesta és
            // la mitigació de risc mínim fins que s'implementi el camí
            // de migració al keystore del SO. Els sistemes no-POSIX
            // (Windows FAT, SMB) llancen UnsupportedOperationException,
            // que capturem i ignorem — no hi ha equivalent portàtil.
            try {
                posarOwnerOnlyPerms(tmpFile.toPath());
            } catch (RuntimeException ignored) {}
            try {
                java.nio.file.Files.move(tmpFile.toPath(), f.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                // Reaplica després del moviment: ATOMIC_MOVE conserva
                // els permisos de la destinació en algunes plataformes,
                // de manera que els permisos que hem posat al tmpFile
                // poden no haver-se traslladat.
                try { posarOwnerOnlyPerms(f.toPath()); }
                catch (RuntimeException ignored) {}
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(tmpFile.toPath(), f.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                try { posarOwnerOnlyPerms(f.toPath()); }
                catch (RuntimeException ignored) {}
            }
        } catch (IOException e) {
            LOG.log(java.util.logging.Level.WARNING, errPrefix, e);
        }
    }

    /**
     * Estableix el fitxer a {@code 0600} (només lectura/escriptura del
     * propietari) en sistemes de fitxers POSIX. Llança
     * {@link java.nio.file.UnsupportedOperationException} en sistemes
     * no-POSIX (Windows, FAT, SMB) i {@link SecurityException} quan la
     * política de seguretat de la JVM nega la crida. IOException (rar —
     * només en una fallada real d'I/O) s'envolta com a
     * {@link RuntimeException} perquè el consumidor pugui fer servir un
     * sol {@code catch (RuntimeException)}.
     */
    private static void posarOwnerOnlyPerms(java.nio.file.Path path) {
        try {
            java.nio.file.Files.setPosixFilePermissions(path,
                java.util.EnumSet.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            buidarNow();
            SAVE_SCHEDULER.shutdownNow();
        }, "config-shutdown"));
    }
}
