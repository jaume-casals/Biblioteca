package presentacio.panells;

import domini.Llista;
import herramienta.i18n.I18n;
import herramienta.ui.Tokens;
import herramienta.ui.UITheme;
import herramienta.ui.UtilitatsColor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import presentacio.panells.navegacio.ItemNavegacio;
import presentacio.util.GestorArrossegarSoltarPrestatgeria;
import presentacio.util.Icones;
import presentacio.util.UIComponents;

/**
 * Barra lateral de navegació.
 *
 * <p>Estructura de dalt a baix:
 * <ol>
 *   <li>Identitat de l'aplicació.</li>
 *   <li>Vistes ràpides (biblioteca, recents, en curs, llegits, desitjos).</li>
 *   <li>Prestatgeries, amb comptador de llibres i estat buit quan no n'hi ha.</li>
 *   <li>Accions: estadístiques, llibre aleatori, llistes, tema, opcions,
 *       sortida.</li>
 * </ol>
 *
 * <p>La vista activa es marca amb un fons tou <em>i</em> un indicador
 * vertical de color accent: el color sol no és prou per si mateix, i el
 * text en pes fort reforça el senyal.
 *
 * <p>Cap entrada de navegació apunta a una funcionalitat que no existeixi:
 * els préstamo i les etiquetes es gestionen dins el llibre i el filtre, no
 * tenen vista pròpia, i no apareixen com a opcions de navegació.
 */
public class PanellBarraEsquerra extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Clau de la vista "tots els llibres". */
    public static final String VISTA_TOTS = "tots";
    /** Clau de la vista "afegits recentment". */
    public static final String VISTA_RECENTS = "recents";
    /** Clau de la vista "en curs". */
    public static final String VISTA_EN_CURS = "en_curs";
    /** Clau de la vista "llegits". */
    public static final String VISTA_LLEGITS = "llegits";
    /** Clau de la vista "llista de desitjos". */
    public static final String VISTA_DESITJOS = "desitjos";
    /** Prefix de la clau d'una prestatgeria concreta. */
    public static final String PREFIX_PRESTATGERIA = "llista:";

    private final JComboBox<Object> comboLlistes;
    private final Runnable onThemeChange;

    private JScrollPane shelvesScroll;
    private JPanel sidebarShelvesPanel;
    private JPanel panellEstatBuit;
    private JButton botonCrearPrestatgeria;
    /** Icona i títol de la marca: es tornen a pintar en cada canvi de tema. */
    private JLabel brandIcona;
    private JLabel brandTitol;

    // Capacitat 64 — cobreix biblioteques amb 20+ prestatgeries sense redimensionar.
    private final Map<Integer, JButton> sidebarShelfBtnMap = new HashMap<>(64);
    private final Map<String, ItemNavegacio> itemsNav = new LinkedHashMap<>();
    private final List<JButton> sidebarBtns = new ArrayList<>();
    /** Icona de cada botó de la barra lateral: el nom del fitxer SVG. */
    private final Map<JButton, String> sidebarIcones = new LinkedHashMap<>();
    /** Etiquetes de secció: el seu color depèn del tema. */
    private final List<JLabel> sidebarSectionLabels = new ArrayList<>();

    private JButton btnAfegitsRecentment;
    private JButton btnLlegitsRecentment;
    private JButton btnDesitjats;
    private JButton btnEnCurs;
    private JButton btnEstadistiques;
    private JButton btnLlibreAleatori;
    private JButton btnGestioLlistes;
    private JButton btnThemeToggle;
    private JButton btnConfiguracio;
    private JButton btnSobre;
    private JButton btnSortir;

    private String clauActiva = VISTA_TOTS;

    public PanellBarraEsquerra(JComboBox<Object> comboLlistes, Runnable onThemeChange) {
        this.comboLlistes = comboLlistes;
        this.onThemeChange = onThemeChange;
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.palette().sidebarBg());
        setPreferredSize(new Dimension(Tokens.SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(Tokens.SIDEBAR_WIDTH, 0));
        buildSidebar();
        seleccionar(VISTA_TOTS);
    }

    private void buildSidebar() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(UITheme.palette().sidebarBg());

        top.add(makeBrand());

        top.add(makeSectionLabel(I18n.t("lbl_sidebar_nav")));

        JButton btnTots = addNavItem(top, VISTA_TOTS, "btn_tots_els_llibres", "tip_tots_els_llibres", "llibres");
        btnTots.addActionListener(e -> {
            if (comboLlistes.getItemCount() > 0) comboLlistes.setSelectedIndex(0);
        });
        // Etiqueta curta: la capçalera mostra el nom complet de la vista.
        btnAfegitsRecentment = addNavItem(top, VISTA_RECENTS, "nav_recently_added_short",
            "tip_afegits_recentment", "recent");
        btnEnCurs = addNavItem(top, VISTA_EN_CURS, "btn_en_curs_sidebar", "tip_en_curs_sidebar", "en-curs");
        btnLlegitsRecentment = addNavItem(top, VISTA_LLEGITS, "btn_llegits_sidebar", "tip_llegits_sidebar", "llegits");
        btnDesitjats = addNavItem(top, VISTA_DESITJOS, "btn_desitjats_sidebar", "tip_desitjats_sidebar", "desitjos");

        top.add(makeSectionLabel(I18n.t("lbl_sidebar_lists")));

        sidebarShelvesPanel = new JPanel();
        sidebarShelvesPanel.setLayout(new BoxLayout(sidebarShelvesPanel, BoxLayout.Y_AXIS));
        sidebarShelvesPanel.setBackground(UITheme.palette().sidebarBg());
        sidebarShelvesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panellEstatBuit = makeEmptyShelvesState();

        // Llista i estat buit viuen en una columna: l'estat buit queda just
        // sota de l'etiqueta de secció, no repartit per tota l'alçada.
        JPanel zonaLlista = new JPanel();
        zonaLlista.setLayout(new BoxLayout(zonaLlista, BoxLayout.Y_AXIS));
        zonaLlista.setBackground(UITheme.palette().sidebarBg());
        zonaLlista.setAlignmentX(Component.LEFT_ALIGNMENT);
        zonaLlista.add(sidebarShelvesPanel);
        zonaLlista.add(panellEstatBuit);

        // El bloc de navegació i la llista comparteixen el mateix desplaçament:
        // en una finestra baixa la barra no es parteix en dos trossos i el
        // contingut es pot desplaçar sencer en lloc de quedar retallat.
        JPanel contingutSidebar = new JPanel(new BorderLayout(0, 0));
        contingutSidebar.setBackground(UITheme.palette().sidebarBg());
        contingutSidebar.add(top, BorderLayout.NORTH);
        contingutSidebar.add(zonaLlista, BorderLayout.CENTER);

        shelvesScroll = new JScrollPane(contingutSidebar);
        shelvesScroll.setBorder(null);
        shelvesScroll.getVerticalScrollBar().setUnitIncrement(12);
        shelvesScroll.setBackground(UITheme.palette().sidebarBg());
        shelvesScroll.getViewport().setBackground(UITheme.palette().sidebarBg());
        shelvesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(shelvesScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(UITheme.palette().sidebarBg());

        bottom.add(makeSidebarSep());

        btnEstadistiques = addNavBtn(bottom, "btn_estadistiques_sidebar", "tip_estadistiques_sidebar", "estadistiques");
        btnLlibreAleatori = addNavBtn(bottom, "btn_aleatori_sidebar", "tip_aleatori_sidebar", "aleatori");
        btnGestioLlistes = addNavBtn(bottom, "btn_gestio_llistes_sidebar", "tip_gestio_llistes_sidebar", "carpeta-mes");

        bottom.add(makeSidebarSep());

        btnThemeToggle = addNavBtn(bottom, "btn_theme", "tip_mode_fosc",
            UITheme.esDark() ? "sol" : "luna");
        // El botó de tema canvia el tema: sense aquesta acció el botó era
        // decoratiu i el mode fosc no es podia activar des de la barra.
        btnThemeToggle.addActionListener(e -> alternarTema());
        btnConfiguracio = addNavBtn(bottom, "btn_configuracio_sidebar", "tip_configuracio_sidebar", "configuracio");
        btnSobre = addNavBtn(bottom, "btn_sobre_sidebar", "tip_sobre_sidebar", "info");

        bottom.add(makeSidebarSep());

        btnSortir = addNavBtn(bottom, "btn_sortir_sidebar", "tip_sortir_sidebar", null);
        btnSortir.addActionListener(e -> {
            if (javax.swing.JOptionPane.showConfirmDialog(this,
                    I18n.t("confirm_exit_msg"), I18n.t("confirm_exit_title"),
                    javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION)
                System.exit(0);
        });

        bottom.add(Box.createVerticalStrut(Tokens.SPACE_3));

        add(bottom, BorderLayout.SOUTH);

        // L'acció de l'estat buit és la mateixa que la gestió de llistes:
        // no hi ha una segona implementació del "crea prestatgeria".
        botonCrearPrestatgeria.addActionListener(e -> btnGestioLlistes.doClick());
    }

    /**
     * Canvia entre mode clar i fosc.
     *
     * <p>El tema s'aplica primer i després es refresca tota la pantalla: el
     * refresc és el que projecta els tokens nous sobre els components que ja
     * existien.
     */
    private void alternarTema() {
        UITheme.Tema actual = UITheme.obtenirTheme();
        UITheme.Tema next = actual == UITheme.Tema.DARK ? UITheme.Tema.LIGHT : UITheme.Tema.DARK;
        UITheme.posarTheme(next);
        if (onThemeChange != null) onThemeChange.run();
    }

    // ── Construcció de les peces ─────────────────────────────────────────────

    private JPanel makeBrand() {
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, Tokens.SPACE_2, 0));
        brand.setBackground(UITheme.palette().sidebarBg());
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tokens.SPACE_6 + Tokens.SPACE_4));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        // El marge del panell (8) + l'espai del FlowLayout (8) col·loquen
        // l'icona a la mateixa columna que les icones de navegació (16), i
        // el títol a la mateixa columna que les etiquetes (44).
        brand.setBorder(BorderFactory.createEmptyBorder(
            Tokens.SPACE_5, Tokens.SPACE_2, Tokens.SPACE_4, Tokens.SPACE_4));

        JLabel icona = new JLabel(Icones.icon("biblioteca", Tokens.ICON_SIZE_LG, UITheme.palette().sidebarAccent()));
        icona.getAccessibleContext().setAccessibleName(I18n.t("app_title"));
        brand.add(icona);
        brandIcona = icona;

        JLabel nom = new JLabel(I18n.t("app_title"));
        nom.setFont(UITheme.fontBold().deriveFont((float) (Tokens.FONT_SIZE_TITLE - 3)));
        nom.setForeground(UITheme.palette().sidebarText());
        brand.add(nom);
        brandTitol = nom;
        return brand;
    }

    /**
     * Estat buit de les prestatgeries.
     *
     * <p>Un rectangle buit no explica res: en lloc seu hi ha un text que
     * explica què passa i una acció per crear-ne una.
     *
     * <p>El panell és compact: el text i l'acció s'hi encadenen sense que
     * l'acció quedi estirada fins a omplir l'espai buit de la barra.
     */
    private JPanel makeEmptyShelvesState() {
        JPanel panell = new JPanel();
        panell.setLayout(new BoxLayout(panell, BoxLayout.Y_AXIS));
        panell.setBackground(UITheme.palette().sidebarBg());
        panell.setAlignmentX(Component.LEFT_ALIGNMENT);
        panell.setBorder(BorderFactory.createEmptyBorder(
            Tokens.SPACE_3, Tokens.SPACE_4, Tokens.SPACE_3, Tokens.SPACE_4));

        JLabel titol = new JLabel("<html><body style='width:150px'>"
            + I18n.t("sidebar_empty_shelves") + "</body></html>");
        titol.setFont(UITheme.fontSmall());
        titol.setForeground(UITheme.palette().sidebarTextMid());
        titol.setAlignmentX(Component.LEFT_ALIGNMENT);
        panell.add(titol);

        JButton crea = new JButton(I18n.t("sidebar_empty_shelves_action"));
        botonCrearPrestatgeria = crea;
        crea.setFont(UITheme.fontSmall());
        crea.setIcon(Icones.icon("afegir", Tokens.ICON_SIZE - 2, UITheme.palette().sidebarAccent()));
        crea.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        crea.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        crea.setContentAreaFilled(false);
        crea.setBorderPainted(false);
        crea.setFocusPainted(true);
        crea.setOpaque(false);
        crea.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        crea.setForeground(UITheme.palette().sidebarAccent());
        crea.setAlignmentX(Component.LEFT_ALIGNMENT);
        panell.add(crea);
        return panell;
    }

    private JButton addNavItem(JPanel container, String clau, String lblKey, String tipKey, String icona) {
        ItemNavegacio btn = new ItemNavegacio(I18n.t(lblKey), Icones.icon(icona, Tokens.ICON_SIZE, UITheme.palette().sidebarText()));
        btn.setToolTipText(I18n.t(tipKey));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemsNav.put(clau, btn);
        sidebarBtns.add(btn);
        sidebarIcones.put(btn, icona);
        container.add(btn);
        return btn;
    }

    private JButton addNavBtn(JPanel container, String lbl, String tip, String icona) {
        JButton btn = makeSidebarBtn(I18n.t(lbl),
            icona == null ? null : Icones.icon(icona, Tokens.ICON_SIZE, UITheme.palette().sidebarText()));
        btn.setToolTipText(I18n.t(tip));
        // Sense aquesta alineació el BoxLayout centra el botó i la columna
        // de icones queda desplaada respecte a la resta de la barra.
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarBtns.add(btn);
        if (icona != null) sidebarIcones.put(btn, icona);
        container.add(btn);
        return btn;
    }

    private JButton makeSidebarBtn(String text, javax.swing.Icon icona) {
        return new ItemNavegacio(text, icona);
    }

    // ── Prestatgeries ────────────────────────────────────────────────────────

    public void rebuildSidebarShelves(List<Llista> llistes, Map<Integer, Integer> counts,
            java.util.function.BiConsumer<Integer, List<Long>> onDragToShelf,
            java.util.function.Consumer<Llista> onShelfRename) {
        Set<Integer> activeIds = new HashSet<>();
        for (Llista l : llistes) activeIds.add(l.obtenirId());
        for (Iterator<Integer> it = sidebarShelfBtnMap.keySet().iterator(); it.hasNext(); ) {
            int id = it.next();
            if (!activeIds.contains(id)) {
                JButton old = sidebarShelfBtnMap.get(id);
                sidebarBtns.remove(old);
                sidebarShelvesPanel.remove(old);
                it.remove();
            }
        }
        for (Llista l : llistes) {
            int count = counts.getOrDefault(l.obtenirId(), 0);
            String label = l.obtenirNom();
            String accessible = label + " (" + count + ")";
            JButton btn = sidebarShelfBtnMap.get(l.obtenirId());
            if (btn != null) {
                if (btn instanceof ItemNavegacio item) item.setComptador(String.valueOf(count));
                btn.getAccessibleContext().setAccessibleName(accessible);
                applyShelfColor(btn, l.obtenirColor(), true);
            } else {
                ItemNavegacio item = new ItemNavegacio(label, null);
                item.setComptador(String.valueOf(count));
                item.getAccessibleContext().setAccessibleName(accessible);
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                applyShelfColor(item, l.obtenirColor(), false);
                final int id = l.obtenirId();
                item.addActionListener(e -> {
                    int idx = findLlistaIndex(id);
                    if (idx >= 0) comboLlistes.setSelectedIndex(idx);
                });
                if (onShelfRename != null) {
                    item.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (e.getClickCount() != 2) return;
                            Llista current = findLlista(id);
                            if (current != null) onShelfRename.accept(current);
                        }
                    });
                }
                sidebarShelfBtnMap.put(l.obtenirId(), item);
                sidebarBtns.add(item);
                sidebarShelvesPanel.add(item);
                GestorArrossegarSoltarPrestatgeria.attach(item, l.obtenirId(), onDragToShelf);
            }
        }
        panellEstatBuit.setVisible(llistes.isEmpty());
        sidebarShelvesPanel.setVisible(!llistes.isEmpty());
        sidebarShelvesPanel.revalidate();
        sidebarShelvesPanel.repaint();
        ajustaAlcades();
        actualizarSeleccio();
    }

    /**
     * Fixa l'alçada màxima dels blocs que no s'han d'estirar.
     *
     * <p>En una caixa vertical, un component amb alçada màxima il·limitada
     * ocupa tota la columna: la llista de prestatgeries i l'estat buit han
     * d'ocupar només l'alçada que els correspon, i la resta ha de quedar buida
     * a sota, no repartida.
     */
    private void ajustaAlcades() {
        if (sidebarShelvesPanel != null) {
            sidebarShelvesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                sidebarShelvesPanel.getPreferredSize().height));
        }
        if (panellEstatBuit != null) {
            panellEstatBuit.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                panellEstatBuit.getPreferredSize().height));
        }
    }

    private void applyShelfColor(JButton btn, String color, boolean clearOnError) {
        if (color != null) {
            try {
                Color c = Color.decode(color);
                btn.setIcon(UtilitatsColor.colorSwatch(c));
                btn.setHorizontalTextPosition(JButton.RIGHT);
            } catch (Exception ignored) {
                if (clearOnError) btn.setIcon(null);
            }
        } else if (clearOnError) {
            btn.setIcon(null);
        }
    }

    // ── Selecció de vista ────────────────────────────────────────────────────

    /**
     * Marca la vista activa.
     *
     * @param clau una de les claus {@code VISTA_*} o {@code PREFIX_PRESTATGERIA + id}
     */
    public void seleccionar(String clau) {
        clauActiva = clau == null ? VISTA_TOTS : clau;
        actualizarSeleccio();
    }


    private void actualizarSeleccio() {
        for (Map.Entry<String, ItemNavegacio> e : itemsNav.entrySet()) {
            e.getValue().setSeleccionat(e.getKey().equals(clauActiva));
        }
        // Les prestatgeries tenen la seva pròpia clau; el comptador i el
        // text es mostren sempre, però la selecció només n'assumeix una.
        for (Map.Entry<Integer, JButton> e : sidebarShelfBtnMap.entrySet()) {
            if (e.getValue() instanceof ItemNavegacio item) {
                item.setSeleccionat((PREFIX_PRESTATGERIA + e.getKey()).equals(clauActiva));
            }
        }
    }

    // ── Tema ─────────────────────────────────────────────────────────────────

    public void aplicarTheme() {
        aplicarBgToNonButtons(this, UITheme.palette().sidebarBg());
        // La marca repinta el seu títol i la seva icona: sense això, en mode
        // fosc el nom de l'aplicació es quedava amb el color del tema clar.
        if (brandTitol != null) brandTitol.setForeground(UITheme.palette().sidebarText());
        if (brandIcona != null) {
            brandIcona.setIcon(Icones.icon("biblioteca", Tokens.ICON_SIZE_LG,
                UITheme.palette().sidebarAccent()));
        }
        for (JLabel lbl : sidebarSectionLabels) {
            lbl.setForeground(UITheme.palette().sidebarTextMid());
        }
        for (Map.Entry<String, ItemNavegacio> e : itemsNav.entrySet()) {
            ItemNavegacio item = e.getValue();
            item.setIcon(Icones.icon(iconeDeClau(e.getKey()), Tokens.ICON_SIZE,
                item.esSeleccionat() ? UITheme.palette().sidebarSelText() : UITheme.palette().sidebarText()));
            item.setForeground(item.esSeleccionat()
                ? UITheme.palette().sidebarSelText()
                : UITheme.palette().sidebarText());
        }
        for (JButton btn : sidebarBtns) {
            if (btn instanceof ItemNavegacio item) {
                item.setForeground(item.esSeleccionat()
                    ? UITheme.palette().sidebarSelText()
                    : UITheme.palette().sidebarText());
                item.actualitzaAlcadaMaxima();
            }
            // Les icones es rasteritzen amb el color del moment: sense
            // repintar-les, en mode fosc es quedaven amb el color del tema
            // clar i es veien pràcticament invisibles.
            String nomIcona = sidebarIcones.get(btn);
            if (nomIcona != null) {
                btn.setIcon(Icones.icon(nomIcona, Tokens.ICON_SIZE, UITheme.palette().sidebarText()));
            }
        }
        // El botó de tema ensenya l'estat destí: lluna per passar a fosc,
        // sol per tornar a clar.
        btnThemeToggle.setIcon(Icones.icon(
            UITheme.esDark() ? "sol" : "luna", Tokens.ICON_SIZE, UITheme.palette().sidebarText()));
        btnSortir.setForeground(UITheme.palette().danger());
        if (panellEstatBuit != null) {
            panellEstatBuit.setBackground(UITheme.palette().sidebarBg());
            for (Component c : panellEstatBuit.getComponents()) {
                if (c instanceof JLabel lbl) lbl.setForeground(UITheme.palette().sidebarTextMid());
                else if (c instanceof JButton b) {
                    b.setIcon(Icones.icon("afegir", Tokens.ICON_SIZE - 2, UITheme.palette().sidebarAccent()));
                    b.setForeground(UITheme.palette().sidebarAccent());
                }
            }
        }
        actualizarSeleccio();
        ajustaAlcades();
        repaint();
    }

    private String iconeDeClau(String clau) {
        switch (clau) {
            case VISTA_RECENTS: return "recent";
            case VISTA_EN_CURS: return "en-curs";
            case VISTA_LLEGITS: return "llegits";
            case VISTA_DESITJOS: return "desitjos";
            default: return "llibres";
        }
    }

    public void nameScrollBarButtons() {
        if (shelvesScroll != null) UIComponents.nameScrollBar(shelvesScroll.getVerticalScrollBar());
    }

    private JPanel makeSidebarSep() {
        JPanel sep = new JPanel();
        sep.setBackground(UITheme.palette().borderClr());
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(0, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel makeSectionLabel(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, Tokens.SPACE_4, Tokens.SPACE_1));
        p.setBackground(UITheme.palette().sidebarBg());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tokens.SPACE_5));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontBold().deriveFont((float) (Tokens.FONT_SIZE_SECTION - 2)));
        lbl.setForeground(UITheme.palette().sidebarTextMid());
        sidebarSectionLabels.add(lbl);
        p.add(lbl);
        return p;
    }

    private void aplicarBgToNonButtons(Component comp, Color bg) {
        if (comp instanceof JButton) return;
        comp.setBackground(bg);
        if (comp instanceof JScrollPane) {
            ((JScrollPane) comp).getViewport().setBackground(bg);
        }
        if (comp instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) comp).getComponents()) {
                if (!(child instanceof javax.swing.JTable)) aplicarBgToNonButtons(child, bg);
            }
        }
    }

    private int findLlistaIndex(int id) {
        for (int i = 0; i < comboLlistes.getItemCount(); i++) {
            Object item = comboLlistes.getItemAt(i);
            if (item instanceof Llista ll && ll.obtenirId() == id) return i;
        }
        return -1;
    }

    private Llista findLlista(int id) {
        for (int i = 0; i < comboLlistes.getItemCount(); i++) {
            Object item = comboLlistes.getItemAt(i);
            if (item instanceof Llista ll && ll.obtenirId() == id) return ll;
        }
        return null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public JButton obtenirBtnAfegitsRecentment()  { return btnAfegitsRecentment; }
    public JButton obtenirBtnLlegitsRecentment()  { return btnLlegitsRecentment; }
    public JButton obtenirBtnDesitjats()          { return btnDesitjats; }
    public JButton obtenirBtnEnCurs()             { return btnEnCurs; }
    public JButton obtenirBtnEstadistiques()      { return btnEstadistiques; }
    public JButton obtenirBtnLlibreAleatori()     { return btnLlibreAleatori; }
    public JButton obtenirBtnGestioLlistes()      { return btnGestioLlistes; }
    public JButton obtenirBtnConfiguracio()       { return btnConfiguracio; }
    public JButton obtenirBtnSobre()              { return btnSobre; }
}
