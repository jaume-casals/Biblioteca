package presentacio.panells;

import com.formdev.flatlaf.FlatClientProperties;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import herramienta.i18n.I18n;
import herramienta.ui.Tokens;
import herramienta.ui.UITheme;
import presentacio.util.Icones;
import presentacio.util.UIComponents;

/**
 * Barra d'eines de la vista de contingut.
 *
 * <p>Ordre i jerarquia:
 * <ol>
 *   <li><b>Cerca</b> a l'esquerra, ample però acotat: és el camp més útil
 *       i no ha de cridar l'atenció més que la resta.</li>
 *   <li><b>Filtres</b> i <b>Ordenació</b> al costat: actuen sobre el
 *       contingut que es mostra.</li>
 *   <li><b>Selector Galeria / Taula</b> a la dreta, amb l'estat seleccionat
 *       visible, perquè l'usuari sempre sap quina vista té davant.</li>
 *   <li><b>Accions addicionals</b> en un menú desplegable: importació,
 *       exportació, còpies de seguretat i escaneig no han d'ocupar espai
 *       permanent a la barra.</li>
 * </ol>
 *
 * <p>El camp de cerca manté el text de suggeriment i un icone inicial: una
 * caixa de cerca sense cap pista no és identificable en una captura.
 */
public class PanelBarraSuperior extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField cercarBar = new JTextField();
    private final JToggleButton btnToggleFiltres = new JToggleButton();
    private final JButton btnOrdena = new JButton();
    // No és final: el crea el helper build(), que crida el constructor.
    private SelectorSegmentat selectorVista;
    private final JToggleButton btnAgruparSeries = new JToggleButton();
    private final JButton btnMesAccions = new JButton();
    private final JPopupMenu menuMes = new JPopupMenu();

    private final List<JMenuItem> itemsMes = new ArrayList<>();

    public PanelBarraSuperior() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UITheme.palette().bgPanel());
        build();
    }

    private void build() {
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(Tokens.SPACE_3, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_5)));

        JPanel fila = new JPanel(new BorderLayout(Tokens.SPACE_3, 0));
        fila.setBackground(UITheme.palette().bgPanel());
        fila.setOpaque(false);

        // ── Cerca ────────────────────────────────────────────────────────────
        JPanel cercaWrap = new JPanel(new BorderLayout());
        cercaWrap.setBackground(UITheme.palette().bgPanel());
        cercaWrap.setOpaque(false);
        cercarBar.setToolTipText(I18n.t("tip_search_bar"));
        cercarBar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
            Icones.icon("cerca", Tokens.ICON_SIZE, UITheme.palette().textMid()));
        cercarBar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
            I18n.t("search_placeholder"));
        UIComponents.styleField(cercarBar);
        cercarBar.setPreferredSize(new Dimension(420, Tokens.CONTROL_HEIGHT));
        cercarBar.setMinimumSize(new Dimension(180, Tokens.CONTROL_HEIGHT));
        cercaWrap.add(cercarBar, BorderLayout.CENTER);

        // ── Controles de la vista ────────────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tokens.SPACE_2, 0));
        controls.setBackground(UITheme.palette().bgPanel());
        controls.setOpaque(false);

        btnToggleFiltres.setText(I18n.t("btn_filters_label"));
        btnToggleFiltres.setIcon(Icones.icon("filtre", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnToggleFiltres.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        UIComponents.styleToggleButton(btnToggleFiltres);
        btnToggleFiltres.setFocusPainted(true);
        UIComponents.setAlçadaControl(btnToggleFiltres, Tokens.CONTROL_HEIGHT);
        controls.add(btnToggleFiltres);

        btnOrdena.setText(I18n.t("btn_sort"));
        btnOrdena.putClientProperty("textComplet", I18n.t("btn_sort"));
        btnOrdena.setIcon(Icones.icon("ordenacio", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnOrdena.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        UIComponents.styleSecondaryButton(btnOrdena);
        UIComponents.setAlçadaControl(btnOrdena, Tokens.CONTROL_HEIGHT);
        controls.add(btnOrdena);

        // Selector Galeria / Taula: dos segments exclusius amb l'estat
        // visible, de manera que l'usuari sempre sap quina vista té davant.
        selectorVista = new SelectorSegmentat(
            new String[] { I18n.t("btn_gallery_view"), I18n.t("btn_table_view") },
            new String[] { "galeria", "taula" });
        selectorVista.setSeleccionat(1);
        controls.add(selectorVista);

        btnAgruparSeries.setText(I18n.t("btn_group_series_lbl"));
        btnAgruparSeries.putClientProperty("textComplet", I18n.t("btn_group_series_lbl"));
        btnAgruparSeries.setIcon(Icones.icon("prestatgeries", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnAgruparSeries.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        btnAgruparSeries.setFocusPainted(true);
        UIComponents.setAlçadaControl(btnAgruparSeries, Tokens.CONTROL_HEIGHT);
        btnAgruparSeries.setFont(UITheme.fontBase());
        UIComponents.styleToggleButton(btnAgruparSeries);
        controls.add(btnAgruparSeries);

        btnMesAccions.setText(I18n.t("btn_more_actions"));
        btnMesAccions.putClientProperty("textComplet", I18n.t("btn_more_actions"));
        btnMesAccions.setIcon(Icones.icon("mes", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnMesAccions.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        UIComponents.styleSecondaryButton(btnMesAccions);
        UIComponents.setAlçadaControl(btnMesAccions, Tokens.CONTROL_HEIGHT);
        btnMesAccions.addActionListener(e -> menuMes.show(btnMesAccions, 0, btnMesAccions.getHeight()));
        controls.add(btnMesAccions);

        fila.add(cercaWrap, BorderLayout.CENTER);
        fila.add(controls, BorderLayout.EAST);
        add(fila, BorderLayout.CENTER);
    }

    // ── API pública ───────────────────────────────────────────────────────────

    public JTextField obtenirSearchBar() { return cercarBar; }
    public JToggleButton obtenirBtnToggleFiltres() { return btnToggleFiltres; }
    public JButton obtenirBtnOrdena() { return btnOrdena; }
    public JButton obtenirBtnVistaGaleria() { return selectorVista.obtenirBotó(0); }
    public JButton obtenirBtnVistaTaula() { return selectorVista.obtenirBotó(1); }
    public JToggleButton obtenirBtnAgruparSeries() { return btnAgruparSeries; }
    public JButton obtenirBtnMesAccions() { return btnMesAccions; }

    /** Marca la vista activa (galeria o taula) i actualitza el selector. */
    public void posarModeVista(boolean galeria) {
        selectorVista.setSeleccionat(galeria ? 0 : 1);
    }

    /** Marca si l'agrupació per sèrie està activa. */
    public void posarAgruparSeries(boolean actiu) {
        btnAgruparSeries.setSelected(actiu);
    }

    /**
     * Afegeix una acció al menú desplegable d'accions addicionals.
     *
     * <p>Les accions es deleguen als botons existents del calaix de filtres
     * mitjançant {@code doClick()}: així no hi ha dues implementacions de la
     * mateixa operació.
     */
    public void afegirAccioMes(String labelKey, Runnable accio) {
        JMenuItem item = new JMenuItem(I18n.t(labelKey));
        item.addActionListener((ActionEvent e) -> accio.run());
        itemsMes.add(item);
        menuMes.add(item);
    }

    /**
     * Aplica la densitat després de cada layout.
     *
     * <p>Es fa aquí i no en un esdeveniment de redimensionament perquè el
     * layout és l'únic moment en què l'amplada real és coneguda: un
     * esdeveniment pot arribar abans que el component estigui disposat.
     */
    @Override public void doLayout() {
        super.doLayout();
        int w = getWidth();
        if (w > 0) aplicaDensitat(w < 860);
    }

    /**
     * Aplica la densitat de la barra segons l'amplada disponible.
     *
     * @param compacta true per amagar les etiquetes dels botons secundaris
     */
    private void aplicaDensitat(boolean compacta) {
        if (densitatCompacta == compacta) return;
        densitatCompacta = compacta;
        // compacta == true vol dir "sense etiqueta": la bandera de
        // visibilitat és la inversa de la densitat.
        boolean ambEtiqueta = !compacta;
        posaEtiqueta(btnOrdena, ambEtiqueta);
        posaEtiqueta(btnAgruparSeries, ambEtiqueta);
        posaEtiqueta(btnMesAccions, ambEtiqueta);
        cercarBar.setMinimumSize(new Dimension(compacta ? 150 : 180, Tokens.CONTROL_HEIGHT));
        revalidate();
        repaint();
    }

    private static void posaEtiqueta(javax.swing.AbstractButton b, boolean visible) {
        if (visible) {
            Object complet = b.getClientProperty("textComplet");
            if (complet != null) b.setText(String.valueOf(complet));
        } else {
            b.setText("");
        }
    }

    private boolean densitatCompacta = false;

    public void aplicarTheme() {
        setBackground(UITheme.palette().bgPanel());
        // La vora inferior es dibuixa amb el color del moment: si es deixa
        // la que hi havia en construir, en mode fosc queda ratllada.
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()),
            BorderFactory.createEmptyBorder(Tokens.SPACE_3, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_5)));
        cercarBar.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
            Icones.icon("cerca", Tokens.ICON_SIZE, UITheme.palette().textMid()));
        UIComponents.styleField(cercarBar);
        UIComponents.styleToggleButton(btnToggleFiltres);
        UIComponents.styleToggleButton(btnAgruparSeries);
        UIComponents.styleSecondaryButton(btnOrdena);
        UIComponents.styleSecondaryButton(btnMesAccions);
        selectorVista.aplicarTheme();
        btnToggleFiltres.setIcon(Icones.icon("filtre", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnOrdena.setIcon(Icones.icon("ordenacio", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnMesAccions.setIcon(Icones.icon("mes", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        btnAgruparSeries.setIcon(Icones.icon("prestatgeries", Tokens.ICON_SIZE, UITheme.palette().textDark()));
        repaint();
    }
}
