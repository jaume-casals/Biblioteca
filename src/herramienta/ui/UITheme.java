package herramienta.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import herramienta.config.Configuracio;
import herramienta.config.MidaLletra;
import herramienta.i18n.I18n;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Tokens de color i tipografia de la interfície.
 *
 * <p>Esta classe és la <em>font de veritat dels tokens de disseny</em>: cap
 * component no defineix colors literals. La paleta activa és un registre
 * immutable ({@link Paleta}) que canvia de forma atòmica quan es canvia de
 * tema, de manera que cap lector fora de l'EDT pot observar un estat
 * parcial.
 *
 * <p>Els components que apliquen aquests tokens viuen a
 * {@code presentacio.util.UIComponents}: la separació és deliberada —
 * els tokens descriuen <em>què</em> es veu, els components decideixen
 * <em>com</em> es dibuixa.
 *
 * <p>El Look &amp; Feel és FlatLaf (no Nimbus). Els tokens es projecten
 * sobre les claus del {@link UIManager} que FlatLaf llegeix, de manera que
 * els components estàndard de Swing hereten la paleta sense cap codi de
 * pintat propi.
 */
public class UITheme {

    private static final Logger LOG = Logger.getLogger(UITheme.class.getName());

    private UITheme() {}

    /**
     * Paleta semàntica activa.
     *
     * <p>Els noms dels components segueixen el vocabulari del producte
     * ({@code bgPanel}, {@code textDark}, {@code accent}…) i no el del
     * framework: així el canvi de Look &amp; Feel no toca el vocabulari.
     */
    public record Paleta(
            // superfícies
            java.awt.Color bgMain,
            java.awt.Color bgPanel,
            java.awt.Color bgElevated,
            java.awt.Color bgSunken,
            java.awt.Color bgSubtle,
            // text
            java.awt.Color textDark,
            java.awt.Color textMid,
            java.awt.Color textMuted,
            java.awt.Color textDisabled,
            java.awt.Color textOnAccent,
            // vores
            java.awt.Color borderClr,
            java.awt.Color borderStrong,
            java.awt.Color borderFocus,
            // accent
            java.awt.Color accent,
            java.awt.Color accentAlt,
            java.awt.Color accentHover,
            java.awt.Color accentPressed,
            java.awt.Color accentSoft,
            java.awt.Color accentBorder,
            // estat
            java.awt.Color danger,
            java.awt.Color dangerHover,
            java.awt.Color dangerSoft,
            java.awt.Color green,
            java.awt.Color greenSoft,
            java.awt.Color warning,
            java.awt.Color warningSoft,
            // controls i superfícies de control
            java.awt.Color secondaryBtnBg,
            java.awt.Color fieldBg,
            java.awt.Color headerBg,
            java.awt.Color headerFg,
            // taula
            java.awt.Color tableGrid,
            java.awt.Color tableAlt,
            // interacció
            java.awt.Color selectionBg,
            java.awt.Color selectionText,
            java.awt.Color hoverBg,
            java.awt.Color focusRing,
            // barra lateral
            java.awt.Color sidebarBg,
            java.awt.Color sidebarAccent,
            java.awt.Color sidebarHoverBg,
            java.awt.Color sidebarText,
            java.awt.Color sidebarTextMid,
            java.awt.Color sidebarSelBg,
            java.awt.Color sidebarSelText,
            // Cerca
            java.awt.Color cercarHighlightBg,
            java.awt.Color cercarHighlightFg) {}

    /** Retorna la paleta de temes activa (registre immutable). */
    public static Paleta palette() {
        return CURRENT;
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

    private static volatile Tema currentTheme = Tema.LIGHT;

    public static Tema obtenirTheme() { return currentTheme; }
    public static boolean esDark() { return currentTheme.dark; }

    // ── Colors fixos (independents del tema) ─────────────────────────────────
    public static final Color DANGER = new Color(0xC62828);
    public static final Color GREEN  = new Color(0x1E7A55);

    /** Entrades de paleta per al destacat de cerca — usades pel RenderitzadorDestacatCerca.
     *  Independent del tema: el destacat ha de destacar sobre qualsevol fons. */
    public static final Color SEARCH_HIGHLIGHT_BG = new Color(0xFFE9A8);
    public static final Color SEARCH_HIGHLIGHT_FG = new Color(0x3A2B00);

    // ── Definició de les paletes ─────────────────────────────────────────────
    //
    // Cada tema es declara amb un constructor de claus (PaletaBuilder) perquè
    // els noms dels rols apareguin a la definició: llegir un color sense
    // saber quin paper fa és la causa número u de les incoherències visuals.

    private static Paleta buildPaleta(PaletaBuilder b) {
        return new Paleta(
                b.bgMain, b.bgPanel, b.bgElevated, b.bgSunken, b.bgSubtle,
                b.textDark, b.textMid, b.textMuted, b.textDisabled, b.textOnAccent,
                b.borderClr, b.borderStrong, b.borderFocus,
                b.accent, b.accentAlt, b.accentHover, b.accentPressed, b.accentSoft, b.accentBorder,
                b.danger, b.dangerHover, b.dangerSoft, b.green, b.greenSoft, b.warning, b.warningSoft,
                b.secondaryBtnBg, b.fieldBg, b.headerBg, b.headerFg,
                b.tableGrid, b.tableAlt,
                b.selectionBg, b.selectionText, b.hoverBg, b.focusRing,
                b.sidebarBg, b.sidebarAccent, b.sidebarHoverBg, b.sidebarText,
                b.sidebarTextMid, b.sidebarSelBg, b.sidebarSelText,
                b.cercarHighlightBg, b.cercarHighlightFg);
    }

    /** Constructor de paletes: cada rol s'assigna una vegada i amb nom. */
    private static final class PaletaBuilder {
        Color bgMain, bgPanel, bgElevated, bgSunken, bgSubtle;
        Color textDark, textMid, textMuted, textDisabled, textOnAccent;
        Color borderClr, borderStrong, borderFocus;
        Color accent, accentAlt, accentHover, accentPressed, accentSoft, accentBorder;
        Color danger, dangerHover, dangerSoft, green, greenSoft, warning, warningSoft;
        Color secondaryBtnBg, fieldBg, headerBg, headerFg;
        Color tableGrid, tableAlt;
        Color selectionBg, selectionText, hoverBg, focusRing;
        Color sidebarBg, sidebarAccent, sidebarHoverBg, sidebarText, sidebarTextMid,
              sidebarSelBg, sidebarSelText;
        Color cercarHighlightBg, cercarHighlightFg;

        PaletaBuilder bg(Color main, Color panel, Color elevated, Color sunken, Color subtle) {
            bgMain = main; bgPanel = panel; bgElevated = elevated; bgSunken = sunken; bgSubtle = subtle;
            return this;
        }
        PaletaBuilder text(Color primary, Color secondary, Color muted, Color disabled, Color onAccent) {
            textDark = primary; textMid = secondary; textMuted = muted;
            textDisabled = disabled; textOnAccent = onAccent;
            return this;
        }
        PaletaBuilder border(Color subtle, Color strong, Color focus) {
            borderClr = subtle; borderStrong = strong; borderFocus = focus;
            return this;
        }
        PaletaBuilder accent(Color base, Color alt, Color hover, Color pressed, Color soft, Color brd) {
            accent = base; accentAlt = alt; accentHover = hover; accentPressed = pressed;
            accentSoft = soft; accentBorder = brd;
            return this;
        }
        PaletaBuilder status(Color dangerBase, Color dangerHover, Color dangerSoft,
                             Color greenBase, Color greenSoft,
                             Color warningBase, Color warningSoft) {
            danger = dangerBase; this.dangerHover = dangerHover; this.dangerSoft = dangerSoft;
            green = greenBase; this.greenSoft = greenSoft;
            warning = warningBase; this.warningSoft = warningSoft;
            return this;
        }
        PaletaBuilder controls(Color secondaryBtn, Color field, Color headerBg, Color headerFg) {
            secondaryBtnBg = secondaryBtn; fieldBg = field;
            this.headerBg = headerBg; this.headerFg = headerFg;
            return this;
        }
        PaletaBuilder table(Color grid, Color alt) {
            tableGrid = grid; tableAlt = alt;
            return this;
        }
        PaletaBuilder interaction(Color selBg, Color selText, Color hover, Color ring) {
            selectionBg = selBg; selectionText = selText; hoverBg = hover; focusRing = ring;
            return this;
        }
        PaletaBuilder sidebar(Color bg, Color accentColor, Color hover, Color text,
                              Color textMid, Color selBg, Color selText) {
            sidebarBg = bg; sidebarAccent = accentColor; sidebarHoverBg = hover;
            sidebarText = text; sidebarTextMid = textMid; sidebarSelBg = selBg; sidebarSelText = selText;
            return this;
        }
        PaletaBuilder search(Color bg, Color fg) {
            cercarHighlightBg = bg; cercarHighlightFg = fg;
            return this;
        }
    }

    private static final EnumMap<Tema, Paleta> PALETTES = buildPalettes();

    private static EnumMap<Tema, Paleta> buildPalettes() {
        EnumMap<Tema, Paleta> m = new EnumMap<>(Tema.class);

        m.put(Tema.LIGHT, buildPaleta(new PaletaBuilder()
            .bg(rgb(0xF4F5F7), rgb(0xFFFFFF), rgb(0xFFFFFF), rgb(0xEDEFF2), rgb(0xF7F8FA))
            .text(rgb(0x141A26), rgb(0x333B4B), rgb(0x4A5262), rgb(0xA8AEBA), rgb(0xFFFFFF))
            .border(rgb(0xE2E5EA), rgb(0xCBD1DA), rgb(0x3358D4))
            .accent(rgb(0x3358D4), rgb(0x2A47B0), rgb(0x3D62DC), rgb(0x25439E), rgb(0xE8EDFB), rgb(0xC2CEF5))
            .status(rgb(0xC62828), rgb(0xB02020), rgb(0xFBEAEA),
                    rgb(0x1E7A55), rgb(0xE6F4EE),
                    rgb(0x9A6A12), rgb(0xFBF1DF))
            .controls(rgb(0xE9ECF1), rgb(0xFFFFFF), rgb(0xFFFFFF), rgb(0x1F2430))
            .table(rgb(0xE7EAEF), rgb(0xFAFBFC))
            .interaction(rgb(0xDCE4FB), rgb(0x1F2430), rgb(0xF0F2F6), rgb(0x3358D4))
            .sidebar(rgb(0xFFFFFF), rgb(0x3358D4), rgb(0xF1F3F7), rgb(0x1F2430),
                     rgb(0x6B7280), rgb(0xE8EDFB), rgb(0x24409E))
            .search(SEARCH_HIGHLIGHT_BG, SEARCH_HIGHLIGHT_FG)));

        m.put(Tema.DARK, buildPaleta(new PaletaBuilder()
            .bg(rgb(0x14161A), rgb(0x1B1E24), rgb(0x21252C), rgb(0x101216), rgb(0x191C22))
            .text(rgb(0xE6E9EF), rgb(0xA6ADBB), rgb(0x7C8494), rgb(0x5C6472), rgb(0xFFFFFF))
            .border(rgb(0x2A2F38), rgb(0x3A414D), rgb(0x6C93F0))
            .accent(rgb(0x6C93F0), rgb(0x5A81DC), rgb(0x7FA2F5), rgb(0x4E77DE), rgb(0x1E2740), rgb(0x2F3E63))
            .status(rgb(0xE05A5A), rgb(0xEE6B6B), rgb(0x34201F),
                    rgb(0x4CB98A), rgb(0x16302A),
                    rgb(0xD99A3D), rgb(0x322817))
            .controls(rgb(0x262B33), rgb(0x171A1F), rgb(0x1B1E24), rgb(0xE6E9EF))
            .table(rgb(0x262B33), rgb(0x1E2128))
            .interaction(rgb(0x223052), rgb(0xEAF0FF), rgb(0x21252C), rgb(0x6C93F0))
            .sidebar(rgb(0x171A1F), rgb(0x6C93F0), rgb(0x1E222A), rgb(0xE6E9EF),
                     rgb(0x9AA2B0), rgb(0x1E2740), rgb(0xC9D8FF))
            .search(rgb(0x6A4C00), rgb(0xFFF3D0))));

        m.put(Tema.SEPIA, buildPaleta(new PaletaBuilder()
            .bg(rgb(0xF6F1E8), rgb(0xFFFDF9), rgb(0xFFFFFF), rgb(0xEFE8DA), rgb(0xFAF6EE))
            .text(rgb(0x241E17), rgb(0x4C463B), rgb(0x5E564A), rgb(0xB3A996), rgb(0xFFFFFF))
            .border(rgb(0xE4DAC7), rgb(0xCFC0A6), rgb(0x8A5A20))
            .accent(rgb(0x8A5A20), rgb(0x6E4615), rgb(0x9C6A2C), rgb(0x6B4414), rgb(0xF4E8D6), rgb(0xE0CBA8))
            .status(rgb(0xB23A2E), rgb(0x9B2C22), rgb(0xF6E4E0),
                    rgb(0x2F6B4F), rgb(0xE3EEE7),
                    rgb(0x9A6A12), rgb(0xF5EBD6))
            .controls(rgb(0xEDE5D7), rgb(0xFFFDF9), rgb(0xFFFDF9), rgb(0x2A241C))
            .table(rgb(0xE9E0CF), rgb(0xFBF7F0))
            .interaction(rgb(0xF0E3CE), rgb(0x2A241C), rgb(0xF3EDE2), rgb(0x8A5A20))
            .sidebar(rgb(0xFFFDF9), rgb(0x8A5A20), rgb(0xF3EDE2), rgb(0x2A241C),
                     rgb(0x7A6E5E), rgb(0xF4E8D6), rgb(0x6E4615))
            .search(SEARCH_HIGHLIGHT_BG, SEARCH_HIGHLIGHT_FG)));

        m.put(Tema.OCEAN, buildPaleta(new PaletaBuilder()
            .bg(rgb(0xEDF3F7), rgb(0xFFFFFF), rgb(0xFFFFFF), rgb(0xE2ECF2), rgb(0xF5F9FC))
            .text(rgb(0x0C1D27), rgb(0x2F4E5D), rgb(0x45697A), rgb(0xA3BCC8), rgb(0xFFFFFF))
            .border(rgb(0xD5E4EC), rgb(0xB6CEDB), rgb(0x0E6E8C))
            .accent(rgb(0x0E6E8C), rgb(0x0A5770), rgb(0x1A7FA0), rgb(0x095A74), rgb(0xE1F0F6), rgb(0xB4D6E4))
            .status(rgb(0xB3261E), rgb(0x9C1F18), rgb(0xF9E7E6),
                    rgb(0x1F6F52), rgb(0xE3F0EA),
                    rgb(0x8A6014), rgb(0xF7EEDC))
            .controls(rgb(0xE5EFF4), rgb(0xFFFFFF), rgb(0xFFFFFF), rgb(0x10242F))
            .table(rgb(0xE1EBF1), rgb(0xF7FAFC))
            .interaction(rgb(0xD6E9F1), rgb(0x10242F), rgb(0xEFF5F9), rgb(0x0E6E8C))
            .sidebar(rgb(0xFFFFFF), rgb(0x0E6E8C), rgb(0xEFF5F9), rgb(0x10242F),
                     rgb(0x5A7C8D), rgb(0xE1F0F6), rgb(0x0A5770))
            .search(SEARCH_HIGHLIGHT_BG, SEARCH_HIGHLIGHT_FG)));

        return m;
    }

    private static Color rgb(int v) { return new Color(v); }

    /** Paleta de temes activa. Es canvia atòmicament per {@link #posarTheme}. */
    private static volatile Paleta CURRENT = PALETTES.get(Tema.LIGHT);

    // ── Fonts (rebuilt by rebuildFonts) ──────────────────────────────────────
    private static volatile Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    private static volatile Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);

    private static final Font FONT_FALLBACK = new Font("SansSerif", Font.PLAIN, 12);

    public static Font fontTitle() { Font f = FONT_TITLE; return f != null ? f : FONT_FALLBACK; }
    public static Font fontSmall() { Font f = FONT_SMALL; return f != null ? f : FONT_FALLBACK; }

    private static volatile Font FONT_BASE  = new Font("SansSerif", Font.PLAIN, 13);

    private static volatile Font FONT_BOLD  = new Font("SansSerif", Font.BOLD,  13);

    public static Font fontBase() { return FONT_BASE; }
    public static Font fontBold() { return FONT_BOLD; }

    static {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            posarTheme(Tema.LIGHT);
        }
    }

    private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    static {
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            main.ShutdownHooks.register(UITheme::shutdown);
        }
    }

    // ── Theme switching ───────────────────────────────────────────────────────

    /**
     * Canvia el tema i reinstala el Look &amp; Feel.
     *
     * <p>El Look &amp; Feel s'instal·la abans de projectar els tokens: la
     * instal·lació reinicialitza els valors per defecte del
     * {@link UIManager}, i projectar-hi els tokens després és l'única
     * ordre que deixa la interfície coherent.
     */
    public static void posarTheme(Tema t) {
        if (!java.awt.EventQueue.isDispatchThread() && !java.awt.GraphicsEnvironment.isHeadless())
            LOG.warning("[UITheme] posarTheme() called off EDT — theme changes must happen on the EDT");
        // No es baixo a l'EDT des d'aquí: l'inicialitzador estàtica de la
        // classe crida posarTheme() i blokcar l'EDT en aquell moment
        // deixa l'aplicació penjada.
        currentTheme = t;
        CURRENT = PALETTES.get(t);
        instalarLookAndFeel(t);
        aplicarUIManager();
    }

    /**
     * Instal·la FlatLaf com a Look &amp; Feel.
     *
     * <p>FlatLaf substitueix Nimbus: Nimbus dibuixa degradats, vores amb
     * relleu i una anatomia de controls que no coincideix amb la resta de
     * l'aplicació. FlatLaf és pla i permet sobreposar la paleta pròpia
     * mitjançant el {@link UIManager}.
     */
    private static void instalarLookAndFeel(Tema t) {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        try {
            if (t.dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No s'ha pogut instal·lar FlatLaf; es continua amb el LAF per defecte", e);
        }
    }

    public static void rebuildFonts(MidaLletra size) {
        if (!java.awt.EventQueue.isDispatchThread() && !java.awt.GraphicsEnvironment.isHeadless())
            LOG.warning("[UITheme] rebuildFonts() called off EDT — font changes must happen on the EDT");
        int sz = size.px;
        FONT_BASE  = new Font("SansSerif", Font.PLAIN, sz);
        FONT_BOLD  = new Font("SansSerif", Font.BOLD,  sz);
        FONT_TITLE = new Font("SansSerif", Font.BOLD,  sz + 5);
        FONT_SMALL = new Font("SansSerif", Font.PLAIN, Math.max(9, sz - 2));
        UIManager.put("defaultFont", FONT_BASE);
    }

    /** Sobrecàrrega de compatibilitat enrere — accepta la clau heretada "small"/"medium"/"large". */
    public static void rebuildFonts(String size) {
        rebuildFonts(MidaLletra.fromKey(size));
    }

    /**
     * Projecta els tokens sobre el {@link UIManager}.
     *
     * <p>S'aplica <em>després</em> d'instal·lar el Look &amp; Feel perquè
     * la instal·lació reinicialitza els valors per defecte. Les claus
     * coincideixen amb les que llegeix FlatLaf, de manera que els components
     * estàndard de Swing (botons, camps, combos, taules, barres de
     * desplaçament, menús) hereten la paleta sense cap pintor propi.
     */
    public static void aplicarUIManager() {
        Paleta p = CURRENT;

        // Superfícies
        UIManager.put("Panel.background",            p.bgPanel());
        UIManager.put("Panel.foreground",            p.textDark());
        UIManager.put("Viewport.background",         p.bgMain());
        // FlatLaf dibuixa una vora al voltant de cada JScrollPane. Després
        // d'un canvi de tema, updateComponentTreeUI la reinstal·la i la
        // interfície acaba amb caixes i marcs que no ha demanat ningú.
        //
        // Posar "null" no serveix: UIManager.remove és el que fa un put amb
        // valor null, i llavors torna el valor per defecte de FlatLaf — un
        // marc clar que en mode fosc es veia com una caixa blanca al voltant
        // del contingut. Cal posar-hi una vora buida explícita.
        UIManager.put("ScrollPane.border",           BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background",       p.bgMain());
        UIManager.put("SplitPane.background",        p.bgMain());
        UIManager.put("OptionPane.background",       p.bgPanel());
        UIManager.put("OptionPane.messageForeground", p.textDark());

        // Text
        UIManager.put("Label.foreground",            p.textDark());
        UIManager.put("Label.disabledForeground",    p.textDisabled());

        // Botons
        UIManager.put("Button.background",           p.bgPanel());
        UIManager.put("Button.foreground",           p.textDark());
        UIManager.put("Button.textColor",            p.textDark());
        UIManager.put("Button.disabledTextColor",    p.textDisabled());
        UIManager.put("Button.focusColor",           p.focusRing());
        UIManager.put("Button.select",               p.accent());

        // Camps de text
        UIManager.put("TextField.background",        p.fieldBg());
        UIManager.put("TextField.foreground",        p.textDark());
        UIManager.put("TextField.caretColor",        p.accent());
        UIManager.put("TextField.inactiveForeground", p.textMuted());
        UIManager.put("TextField.placeholderForeground", p.textMuted());
        UIManager.put("TextField.selectionBackground", p.accent());
        UIManager.put("TextField.selectionForeground", p.textOnAccent());
        UIManager.put("PasswordField.background",    p.fieldBg());
        UIManager.put("PasswordField.foreground",    p.textDark());
        UIManager.put("TextArea.background",         p.fieldBg());
        UIManager.put("TextArea.foreground",         p.textDark());
        UIManager.put("TextArea.caretColor",         p.accent());
        UIManager.put("TextArea.inactiveForeground", p.textMuted());

        // Combos i llistes
        UIManager.put("ComboBox.background",         p.fieldBg());
        UIManager.put("ComboBox.foreground",         p.textDark());
        UIManager.put("ComboBox.buttonBackground",   p.secondaryBtnBg());
        UIManager.put("ComboBox.buttonDarkShadow",   p.borderStrong());
        UIManager.put("ComboBox.buttonHighlight",    p.bgElevated());
        UIManager.put("ComboBox.popupBackground",    p.bgElevated());
        UIManager.put("ComboBox.disabledForeground", p.textDisabled());
        UIManager.put("List.background",             p.bgElevated());
        UIManager.put("List.foreground",             p.textDark());
        UIManager.put("List.selectionBackground",    p.selectionBg());
        UIManager.put("List.selectionForeground",    p.selectionText());

        // Caselles
        UIManager.put("CheckBox.background",         p.bgPanel());
        UIManager.put("CheckBox.foreground",         p.textDark());
        UIManager.put("CheckBox.textColor",          p.textDark());
        UIManager.put("CheckBox.disabledTextColor",  p.textDisabled());
        UIManager.put("RadioButton.background",      p.bgPanel());
        UIManager.put("RadioButton.foreground",      p.textDark());
        UIManager.put("RadioButton.textColor",       p.textDark());
        UIManager.put("RadioButton.disabledTextColor", p.textDisabled());

        // Taula
        UIManager.put("Table.background",            p.bgPanel());
        UIManager.put("Table.foreground",            p.textDark());
        UIManager.put("Table.gridColor",             p.tableGrid());
        UIManager.put("Table.showVerticalLines",     Boolean.FALSE);
        UIManager.put("Table.showHorizontalLines",   Boolean.TRUE);
        UIManager.put("Table.intercellSpacing",      new java.awt.Dimension(0, 1));
        UIManager.put("Table.selectionBackground",   p.selectionBg());
        UIManager.put("Table.selectionForeground",   p.selectionText());
        UIManager.put("TableHeader.background",      p.bgSunken());
        UIManager.put("TableHeader.foreground",      p.textMid());
        UIManager.put("TableHeader.sortIconBackground", p.bgSunken());

        // Barres de desplaçament
        UIManager.put("ScrollBar.background",        p.bgMain());
        UIManager.put("ScrollBar.thumbColor",        p.borderStrong());
        UIManager.put("ScrollBar.thumbShadowColor",  p.bgMain());
        UIManager.put("ScrollBar.width",             Integer.valueOf(11));
        UIManager.put("ScrollBar.thumbArcHeight",    Integer.valueOf(999));

        // Menús
        UIManager.put("MenuBar.background",          p.bgPanel());
        UIManager.put("MenuBar.foreground",          p.textDark());
        UIManager.put("Menu.background",             p.bgElevated());
        UIManager.put("Menu.foreground",             p.textDark());
        UIManager.put("MenuItem.background",         p.bgElevated());
        UIManager.put("MenuItem.foreground",         p.textDark());
        UIManager.put("MenuItem.selectionBackground", p.selectionBg());
        UIManager.put("MenuItem.selectionForeground", p.selectionText());
        UIManager.put("PopupMenu.background",        p.bgElevated());
        UIManager.put("PopupMenu.foreground",        p.textDark());
        UIManager.put("RadioButtonMenuItem.background", p.bgElevated());

        // Pestanyes
        UIManager.put("TabbedPane.background",       p.bgPanel());
        UIManager.put("TabbedPane.foreground",       p.textMid());
        UIManager.put("TabbedPane.selectedForeground", p.textDark());
        UIManager.put("TabbedPane.selectHighlight",  p.accent());
        UIManager.put("TabbedPane.contentAreaColor", p.bgPanel());

        // Progrés i altres
        UIManager.put("ProgressBar.background",      p.bgSunken());
        UIManager.put("ProgressBar.foreground",      p.accent());
        UIManager.put("ToolTip.background",          p.bgElevated());
        UIManager.put("ToolTip.foreground",          p.textDark());
        UIManager.put("ToolTip.border",              BorderFactory.createLineBorder(p.borderStrong()));
        UIManager.put("Separator.foreground",        p.borderClr());
        UIManager.put("Separator.background",        p.borderClr());
        UIManager.put("ToggleButton.background",     p.bgPanel());
        UIManager.put("ToggleButton.foreground",     p.textDark());

        // Geometria comuna: FlatLaf fa servir "Component.arc" per a tots els
        // components amb racons i "Button.arc" per als botons.
        UIManager.put("Component.arc",     Integer.valueOf(Tokens.RADIUS_MD));
        UIManager.put("Button.arc",        Integer.valueOf(Tokens.RADIUS_MD));
        UIManager.put("TextComponent.arc", Integer.valueOf(Tokens.RADIUS_MD));
        UIManager.put("CheckBox.arc",      Integer.valueOf(Tokens.RADIUS_SM));
        UIManager.put("ProgressBar.arc",   Integer.valueOf(Tokens.RADIUS_SM));
        UIManager.put("ScrollBar.arc",     Integer.valueOf(Tokens.RADIUS_MD));

        // FlatLaf: la instal·lació del LAF reinicialitza els valors per
        // defecte; updateUI() torna a llegir els tokens que s'acaben de
        // projectar sense perdre-los.
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                FlatLaf.updateUI();
            } catch (Exception e) {
                LOG.log(Level.FINE, "FlatLaf.updateUI() no disponible", e);
            }
        }

        I18n.aplicarSwingOptionPane();
    }

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
     * Allibera les instàncies de Font en caché perquè la JVM les pugui
     * recuperar més aviat en tancar. Es crida automàticament un cop via
     * un ganxo de tancada de la JVM i és segur cridar-la manualment.
     */
    public static synchronized void shutdown() {
        if (shutdownDone) return;
        shutdownDone = true;
        FONT_BASE  = null;
        FONT_BOLD  = null;
        FONT_TITLE = null;
        FONT_SMALL = null;
    }
    private static volatile boolean shutdownDone = false;
}
