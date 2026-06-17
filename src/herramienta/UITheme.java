package herramienta;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class UITheme {

    private UITheme() {}

    /** Typed read-only view of the runtime theme palette.  A fresh snapshot
     *  is returned by {@link #palette()} on every call so the record reflects
     *  the current state of the mutable Color statics (which are still the
     *  single source of truth — they get re-assigned by {@link #setTheme}
     *  and the theme appliers).  Migration target: callers do
     *  {@code UITheme.palette().bgMain} instead of {@code UITheme.palette().bgMain()}. */
    public record Paleta(
            java.awt.Color bgMain,
            java.awt.Color bgPanel,
            java.awt.Color accent,
            java.awt.Color accentAlt,
            java.awt.Color textDark,
            java.awt.Color textMid,
            java.awt.Color borderClr,
            java.awt.Color headerBg,
            java.awt.Color headerFg,
            java.awt.Color tableGrid,
            java.awt.Color tableAlt,
            java.awt.Color secondaryBtnBg,
            java.awt.Color fieldBg,
            java.awt.Color nimbusBlueGrey,
            java.awt.Color danger,
            java.awt.Color green,
            java.awt.Color sidebarBg,
            java.awt.Color sidebarAccent,
            java.awt.Color sidebarHoverBg,
            java.awt.Color sidebarText,
            java.awt.Color sidebarTextMid,
            java.awt.Color sidebarSelBg,
            java.awt.Color cercarHighlightBg,
            java.awt.Color cercarHighlightFg) {}

    /** Snapshot the current Color state into an immutable Palette record.
     *  Callers should prefer {@code UITheme.palette().xxx} over the raw
     *  {@code UITheme.XXX} field accesses. */
    public static Paleta palette() {
        return new Paleta(BG_MAIN, BG_PANEL, ACCENT, ACCENT_ALT, TEXT_DARK, TEXT_MID,
                BORDER_CLR, HEADER_BG, HEADER_FG, TABLE_GRID, TABLE_ALT,
                SECONDARY_BTN_BG, FIELD_BG, NIMBUS_BLUE_GREY, DANGER, GREEN,
                SIDEBAR_BG, SIDEBAR_ACCENT, SIDEBAR_HOVER_BG, SIDEBAR_TEXT, SIDEBAR_TEXT_MID, SIDEBAR_SEL_BG,
                SEARCH_HIGHLIGHT_BG, SEARCH_HIGHLIGHT_FG);
    }

    public enum Tema {
        LIGHT(false),
        DARK( true),
        SEPIA(false),
        OCEAN(false);

        public final boolean dark;
        Tema(boolean dark) { this.dark = dark; }

        public String displayName() { return I18n.t("theme_" + name().toLowerCase()); }

        public String key() { return name().toLowerCase(); }

        public static Tema fromKey(String key) {
            if (key == null) return LIGHT;
            for (Tema t : values()) if (t.name().equalsIgnoreCase(key)) return t;
            return LIGHT;
        }
    }

    private static Tema currentTheme = Tema.LIGHT;

    public static Tema obtenirTheme() { return currentTheme; }
    public static boolean esDark() { return currentTheme.dark; }

    // ── Palette (updated by setDark) ─────────────────────────────────────────
    public static Color BG_MAIN;
    public static Color BG_PANEL;
    public static Color ACCENT;
    public static Color ACCENT_ALT;
    public static Color TEXT_DARK;
    public static Color TEXT_MID;
    public static Color BORDER_CLR;
    public static Color HEADER_BG;
    public static Color HEADER_FG;
    public static Color TABLE_GRID;
    public static Color TABLE_ALT;

    // Derivats — usats pels helpers d'estil de UIComponents (estil de la capa de panell).
    public static Color SECONDARY_BTN_BG;
    public static Color FIELD_BG;
    static Color NIMBUS_BLUE_GREY;

    // ── Fixed colors (theme-independent) ─────────────────────────────────────
    public static final Color DANGER = new Color(0xC0392B);
    public static final Color GREEN  = new Color(0x117A65);

    /** Search-highlight palette entries — used by the SearchHighlightRenderer.
     *  Theme-independent: the highlight should pop on any background, so the
     *  amber bg / black fg combo is shared across all themes. */
    public static final Color SEARCH_HIGHLIGHT_BG = new Color(0xF39C12);
    public static final Color SEARCH_HIGHLIGHT_FG = new Color(0x000000);

    // ── Sidebar colors (updated by setDark) ───────────────────────────────────
    public static Color SIDEBAR_BG;
    public static Color SIDEBAR_ACCENT;
    public static Color SIDEBAR_HOVER_BG;
    public static Color SIDEBAR_TEXT;
    public static Color SIDEBAR_TEXT_MID;
    public static Color SIDEBAR_SEL_BG;

    // ── Light palette (warm library theme) ───────────────────────────────────
    private static final Color L_BG_MAIN        = new Color(0xF5F3EE);
    private static final Color L_BG_PANEL       = new Color(0xFFFFFF);
    private static final Color L_ACCENT         = new Color(0x9B5E1A);
    private static final Color L_ACCENT_ALT     = new Color(0x7A4A14);
    private static final Color L_TEXT_DARK      = new Color(0x2C1810);
    private static final Color L_TEXT_MID       = new Color(0x7A6E66);
    private static final Color L_BORDER_CLR     = new Color(0xE5E0D8);
    private static final Color L_HEADER_BG      = new Color(0x2A2520);
    private static final Color L_HEADER_FG      = new Color(0xE8E0D8);
    private static final Color L_TABLE_GRID     = new Color(0xEDE8E1);
    private static final Color L_TABLE_ALT      = new Color(0xFAF8F5);
    private static final Color L_SECONDARY_BTN  = new Color(0xB8A89A);
    private static final Color L_FIELD_BG       = new Color(0xFDFCFA);
    private static final Color L_NIMBUS_BG      = new Color(0x9B5E1A);

    // ── Dark palette ──────────────────────────────────────────────────────────
    private static final Color D_BG_MAIN        = new Color(0x2B2B2B);
    private static final Color D_BG_PANEL       = new Color(0x3C3F41);
    private static final Color D_ACCENT         = new Color(0x589DF6);
    private static final Color D_ACCENT_ALT     = new Color(0x3A6DB5);
    private static final Color D_TEXT_DARK      = new Color(0xCCCCCC);
    private static final Color D_TEXT_MID       = new Color(0x999999);
    private static final Color D_BORDER_CLR     = new Color(0x565656);
    private static final Color D_HEADER_BG      = new Color(0x1E1E1E);
    private static final Color D_HEADER_FG      = new Color(0xCCCCCC);
    private static final Color D_TABLE_GRID     = new Color(0x4A4A4A);
    private static final Color D_TABLE_ALT      = new Color(0x353535);
    private static final Color D_SECONDARY_BTN  = new Color(0x3D4451);
    private static final Color D_FIELD_BG       = new Color(0x45474A);
    private static final Color D_NIMBUS_BG      = new Color(0x3D4451);

    // ── Sepia palette (warm parchment) ────────────────────────────────────────
    private static final Color SP_BG_MAIN       = new Color(0xF5EDD3);
    private static final Color SP_BG_PANEL      = new Color(0xFFF9F0);
    private static final Color SP_ACCENT        = new Color(0x8B4513);
    private static final Color SP_ACCENT_ALT    = new Color(0x6B3410);
    private static final Color SP_TEXT_DARK     = new Color(0x2C1A0E);
    private static final Color SP_TEXT_MID      = new Color(0x7A6050);
    private static final Color SP_BORDER_CLR    = new Color(0xDDD0B8);
    private static final Color SP_HEADER_BG     = new Color(0x3A2010);
    private static final Color SP_HEADER_FG     = new Color(0xF0E8D8);
    private static final Color SP_TABLE_GRID    = new Color(0xE8D8C0);
    private static final Color SP_TABLE_ALT     = new Color(0xFAF3E8);
    private static final Color SP_SECONDARY_BTN = new Color(0xB89878);
    private static final Color SP_FIELD_BG      = new Color(0xFFFDF5);
    private static final Color SP_NIMBUS_BG     = new Color(0x8B4513);

    // ── Ocean palette (cool blue-teal) ────────────────────────────────────────
    private static final Color OC_BG_MAIN       = new Color(0xEBF4F8);
    private static final Color OC_BG_PANEL      = new Color(0xFFFFFF);
    private static final Color OC_ACCENT        = new Color(0x0077A8);
    private static final Color OC_ACCENT_ALT    = new Color(0x005A80);
    private static final Color OC_TEXT_DARK     = new Color(0x0A2840);
    private static final Color OC_TEXT_MID      = new Color(0x4A7090);
    private static final Color OC_BORDER_CLR    = new Color(0xBDD8E8);
    private static final Color OC_HEADER_BG     = new Color(0x082030);
    private static final Color OC_HEADER_FG     = new Color(0xD0EAF8);
    private static final Color OC_TABLE_GRID    = new Color(0xCCE4F0);
    private static final Color OC_TABLE_ALT     = new Color(0xF2F9FD);
    private static final Color OC_SECONDARY_BTN = new Color(0x6898B8);
    private static final Color OC_FIELD_BG      = new Color(0xF5FBFF);
    private static final Color OC_NIMBUS_BG     = new Color(0x0077A8);

    // ── Fonts (rebuilt by rebuildFonts) ──────────────────────────────────────
    public static volatile Font FONT_LABEL = new Font("SansSerif", Font.BOLD,  12);
    public static volatile Font FONT_TITLE = new Font("SansSerif", Font.BOLD,  18);
    public static volatile Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    /**
     * Base font, rebuilt by {@link #rebuildFonts(String)} when the configured
     * font size changes. Held as {@code private static volatile} so external
     * code must go through {@link #fontBase()} to read the current value,
     * preventing races with {@code rebuildFonts} on the EDT.
     */
    private static volatile Font FONT_BASE  = new Font("SansSerif", Font.PLAIN, 13);

    /**
     * Bold base font, rebuilt by {@link #rebuildFonts(String)} when the
     * configured font size changes. Held as {@code private static volatile}
     * so external code must go through {@link #fontBold()} to read the
     * current value, preventing races with {@code rebuildFonts} on the EDT.
     */
    private static volatile Font FONT_BOLD  = new Font("SansSerif", Font.BOLD,  13);

    public static Font fontBase() { return FONT_BASE; }
    public static Font fontBold() { return FONT_BOLD; }

    static { posarTheme(Tema.LIGHT); }

    /** Pair with {@link Config}'s shutdown hook to release the cached Font
     *  instances at JVM exit (belt-and-suspenders for the static-state pattern). */
    private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    static {
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            main.ShutdownHooks.register(UITheme::shutdown);
        }
    }

    // ── Theme switching ───────────────────────────────────────────────────────
    public static void posarTheme(Tema t) {
        if (!java.awt.EventQueue.isDispatchThread() && !java.awt.GraphicsEnvironment.isHeadless())
            java.util.logging.Logger.getLogger(UITheme.class.getName())
                .warning("[UITheme] setTheme() called off EDT — theme changes must happen on the EDT");
        currentTheme = t;
        switch (t) {
            case DARK:
                SIDEBAR_BG       = new Color(0x1A1510);
                SIDEBAR_ACCENT   = new Color(0xC48F45);
                SIDEBAR_HOVER_BG = new Color(0x2E241A);
                SIDEBAR_TEXT     = new Color(0xE0D8D0);
                SIDEBAR_TEXT_MID = new Color(0x8B7E74);
                SIDEBAR_SEL_BG   = new Color(0x2A2010);
                BG_MAIN          = D_BG_MAIN;   BG_PANEL    = D_BG_PANEL;
                ACCENT           = D_ACCENT;    ACCENT_ALT  = D_ACCENT_ALT;
                TEXT_DARK        = D_TEXT_DARK; TEXT_MID    = D_TEXT_MID;
                BORDER_CLR       = D_BORDER_CLR;
                HEADER_BG        = D_HEADER_BG; HEADER_FG   = D_HEADER_FG;
                TABLE_GRID       = D_TABLE_GRID; TABLE_ALT  = D_TABLE_ALT;
                SECONDARY_BTN_BG = D_SECONDARY_BTN;
                FIELD_BG         = D_FIELD_BG;  NIMBUS_BLUE_GREY = D_NIMBUS_BG;
                break;
            case SEPIA:
                SIDEBAR_BG       = new Color(0x2A1800);
                SIDEBAR_ACCENT   = new Color(0xD4A040);
                SIDEBAR_HOVER_BG = new Color(0x3E270A);
                SIDEBAR_TEXT     = new Color(0xF0E8D8);
                SIDEBAR_TEXT_MID = new Color(0x9B8870);
                SIDEBAR_SEL_BG   = new Color(0x4A3020);
                BG_MAIN          = SP_BG_MAIN;  BG_PANEL    = SP_BG_PANEL;
                ACCENT           = SP_ACCENT;   ACCENT_ALT  = SP_ACCENT_ALT;
                TEXT_DARK        = SP_TEXT_DARK; TEXT_MID   = SP_TEXT_MID;
                BORDER_CLR       = SP_BORDER_CLR;
                HEADER_BG        = SP_HEADER_BG; HEADER_FG  = SP_HEADER_FG;
                TABLE_GRID       = SP_TABLE_GRID; TABLE_ALT = SP_TABLE_ALT;
                SECONDARY_BTN_BG = SP_SECONDARY_BTN;
                FIELD_BG         = SP_FIELD_BG; NIMBUS_BLUE_GREY = SP_NIMBUS_BG;
                break;
            case OCEAN:
                SIDEBAR_BG       = new Color(0x071828);
                SIDEBAR_ACCENT   = new Color(0x40C8E0);
                SIDEBAR_HOVER_BG = new Color(0x1B2732);
                SIDEBAR_TEXT     = new Color(0xC8E8F8);
                SIDEBAR_TEXT_MID = new Color(0x6898B0);
                SIDEBAR_SEL_BG   = new Color(0x102840);
                BG_MAIN          = OC_BG_MAIN;  BG_PANEL    = OC_BG_PANEL;
                ACCENT           = OC_ACCENT;   ACCENT_ALT  = OC_ACCENT_ALT;
                TEXT_DARK        = OC_TEXT_DARK; TEXT_MID   = OC_TEXT_MID;
                BORDER_CLR       = OC_BORDER_CLR;
                HEADER_BG        = OC_HEADER_BG; HEADER_FG   = OC_HEADER_FG;
                TABLE_GRID       = OC_TABLE_GRID; TABLE_ALT  = OC_TABLE_ALT;
                SECONDARY_BTN_BG = OC_SECONDARY_BTN;
                FIELD_BG         = OC_FIELD_BG;  NIMBUS_BLUE_GREY = OC_NIMBUS_BG;
                break;
            default: // LIGHT
                SIDEBAR_BG       = new Color(0x2A2520);
                SIDEBAR_ACCENT   = new Color(0xC48F45);
                SIDEBAR_HOVER_BG = new Color(0x3E342A);
                SIDEBAR_TEXT     = new Color(0xE8E0D8);
                SIDEBAR_TEXT_MID = new Color(0x9B8E84);
                SIDEBAR_SEL_BG   = new Color(0x3D3028);
                BG_MAIN          = L_BG_MAIN;   BG_PANEL    = L_BG_PANEL;
                ACCENT           = L_ACCENT;    ACCENT_ALT  = L_ACCENT_ALT;
                TEXT_DARK        = L_TEXT_DARK; TEXT_MID    = L_TEXT_MID;
                BORDER_CLR       = L_BORDER_CLR;
                HEADER_BG        = L_HEADER_BG; HEADER_FG   = L_HEADER_FG;
                TABLE_GRID       = L_TABLE_GRID; TABLE_ALT  = L_TABLE_ALT;
                SECONDARY_BTN_BG = L_SECONDARY_BTN;
                FIELD_BG         = L_FIELD_BG;  NIMBUS_BLUE_GREY = L_NIMBUS_BG;
                break;
        }
        aplicarUIManager();
    }

    /** Compat — maps to LIGHT or DARK. */
    public static void posarDark(boolean dark) {
        posarTheme(dark ? Tema.DARK : Tema.LIGHT);
    }

    public static void rebuildFonts(FontSize size) {
        if (!java.awt.EventQueue.isDispatchThread() && !java.awt.GraphicsEnvironment.isHeadless())
            java.util.logging.Logger.getLogger(UITheme.class.getName())
                .warning("[UITheme] rebuildFonts() called off EDT — font changes must happen on the EDT");
        int sz = size.px;
        FONT_BASE  = new Font("SansSerif", Font.PLAIN, sz);
        FONT_BOLD  = new Font("SansSerif", Font.BOLD,  sz);
        FONT_LABEL = new Font("SansSerif", Font.BOLD,  Math.max(9, sz - 1));
        FONT_TITLE = new Font("SansSerif", Font.BOLD,  sz + 5);
        FONT_SMALL = new Font("SansSerif", Font.PLAIN, Math.max(9, sz - 2));
        UIManager.put("defaultFont", FONT_BASE);
    }

    /** Backwards-compat overload — accepts the legacy "small"/"medium"/"large" key. */
    public static void rebuildFonts(String size) {
        rebuildFonts(FontSize.fromKey(size));
    }

    public static void aplicarUIManager() {
        UIManager.put("control",                   BG_MAIN);
        UIManager.put("text",                      TEXT_DARK);
        UIManager.put("nimbusBase",                ACCENT);
        UIManager.put("nimbusBlueGrey",            NIMBUS_BLUE_GREY);
        UIManager.put("nimbusFocus",               ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusSelectedText",        Color.WHITE);
        UIManager.put("Table.alternateRowColor",   TABLE_ALT);
        // Prevé el ClassCastException del pintor SynthBorder de Nimbus
        // als renderers de cel·la basats en label
        UIManager.put("Table.cellNoFocusBorder",      BorderFactory.createEmptyBorder(0, 2, 0, 2));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(0, 2, 0, 2));
        I18n.aplicarSwingOptionPane();
    }

    // Els helpers d'estil viuen a presentacio.UIComponents (moviment
    // de la sessió 11): la capa presentacio és l'única consumidora, i
    // la indirecta a través d'aquesta classe s'ha eliminat de manera
    // que UIComponents té la implementació real. Els dos camps
    // package-private (SECONDARY_BTN_BG, FIELD_BG) es queden aquí com a
    // font de la paleta de temes per a aquests helpers.

    public static ImageIcon scaledIcon(byte[] data, int size) {
        if (data == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_FAST));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static File chooseImageFile(Component parent) {
        String dir = Configuracio.obtenirDefaultImgDir();
        JFileChooser chooser = new JFileChooser(new File(dir).exists() ? dir : System.getProperty("user.home"));
        chooser.setFileFilter(new FileNameExtensionFilter("Imatges", "jpg", "jpeg", "png", "gif", "bmp", "webp"));
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile() : null;
    }

    /**
     * Release the cached Font instances so the JVM can reclaim them
     * sooner at shutdown. Called automatically once via a JVM shutdown
     * hook (paired with {@link Config}'s) and safe to call manually
     * (idempotent, no-op if already cleared).
     */
    public static synchronized void shutdown() {
        if (shutdownDone) return;
        shutdownDone = true;
        FONT_BASE  = null;
        FONT_BOLD  = null;
        FONT_LABEL = null;
        FONT_TITLE = null;
        FONT_SMALL = null;
    }
    private static volatile boolean shutdownDone = false;
}
