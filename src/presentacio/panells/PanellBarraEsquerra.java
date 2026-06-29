package presentacio.panells;



import domini.Llista;
import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import herramienta.ui.UtilitatsColor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import presentacio.util.GestorArrossegarSoltarPrestatgeria;
import presentacio.util.UIComponents;



public class PanellBarraEsquerra extends JPanel {

	private final JComboBox<Object> comboLlistes;
	private final Runnable onThemeChange;

	private JScrollPane shelvesScroll;
	private JPanel sidebarShelvesPanel;
	// Capacitat 64 — cobreix biblioteques amb 20+ prestatgeries sense redimensionar.
	// El valor per defecte 16 provoca un rehash cap a ~13 entrades; el finding
	// MEDIUM de tot.txt ho va assenyalar com un pic innecessari durant la
	// construcció inicial de la barra lateral per a biblioteques mitjanes.
	private final Map<Integer, JButton> sidebarShelfBtnMap = new HashMap<>(64);
	// Els camps dels botons no poden ser `final` perquè s'assignen a
	// {@link #buildSidebar()}, que el constructor crida com a helper.
	// L'anàlisi d'assignació definida de Java no segueix les assignacions
	// dins de mètodes auxiliars, de manera que la paraula clau `final`
	// obligaria a inline la construcció sencera de la barra lateral dins
	// del constructor (refactor gran) o bé a moure la creació dels botons
	// als inicialitzadors de camp (la jerarquia del panell s'ha de muntar
	// primer — fora d'ordre). Registrat a tot.txt MEDIUM com una petita
	// olor coneguda.
	private JButton btnTotsElsLlibres;
	private final List<JButton> sidebarBtns = new ArrayList<>();

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

	public PanellBarraEsquerra(JComboBox<Object> comboLlistes, Runnable onThemeChange) {
		this.comboLlistes = comboLlistes;
		this.onThemeChange = onThemeChange;
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().sidebarBg());
		setPreferredSize(new Dimension(220, 0));
		setMinimumSize(new Dimension(220, 0));
		buildSidebar();
	}

	private void buildSidebar() {
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(UITheme.palette().sidebarBg());

		JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
		logoPanel.setBackground(UITheme.palette().sidebarBg());
		logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		JLabel logo = new JLabel("biblioteca");
		logo.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
		logo.setForeground(UITheme.palette().sidebarText());
		logoPanel.add(logo);
		top.add(logoPanel);

		top.add(makeSidebarSep());

		top.add(makeSectionLabel(I18n.t("lbl_sidebar_nav")));

		btnTotsElsLlibres = addNavBtn(top, "btn_tots_els_llibres", "tip_tots_els_llibres");
		btnTotsElsLlibres.addActionListener(e -> {
			if (comboLlistes.getItemCount() > 0) comboLlistes.setSelectedIndex(0);
		});

		btnAfegitsRecentment = addNavBtn(top, "btn_afegits_recentment", "tip_afegits_recentment");
		btnLlegitsRecentment = addNavBtn(top, "btn_llegits_sidebar", "tip_llegits_sidebar");
		btnDesitjats = addNavBtn(top, "btn_desitjats_sidebar", "tip_desitjats_sidebar");
		btnEnCurs = addNavBtn(top, "btn_en_curs_sidebar", "tip_en_curs_sidebar");

		top.add(makeSidebarSep());

		top.add(makeSectionLabel(I18n.t("lbl_sidebar_lists")));

		add(top, BorderLayout.NORTH);

		sidebarShelvesPanel = new JPanel();
		sidebarShelvesPanel.setLayout(new BoxLayout(sidebarShelvesPanel, BoxLayout.Y_AXIS));
		sidebarShelvesPanel.setBackground(UITheme.palette().sidebarBg());

		shelvesScroll = new JScrollPane(sidebarShelvesPanel);
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

		btnEstadistiques = addNavBtn(bottom, "btn_estadistiques_sidebar", "tip_estadistiques_sidebar");
		btnLlibreAleatori = addNavBtn(bottom, "btn_aleatori_sidebar", "tip_aleatori_sidebar");

		bottom.add(makeSidebarSep());

		btnGestioLlistes = addNavBtn(bottom, "btn_gestio_llistes_sidebar", "tip_gestio_llistes_sidebar");

		bottom.add(makeSidebarSep());

		btnThemeToggle = addNavBtn(bottom, "btn_theme", "tip_mode_fosc");
		btnThemeToggle.addActionListener(e -> {
			herramienta.ui.UITheme.Tema[] themes = herramienta.ui.UITheme.Tema.values();
			herramienta.ui.UITheme.Tema next = themes[(UITheme.obtenirTheme().ordinal() + 1) % themes.length];
			UITheme.posarTheme(next);
			onThemeChange.run();
		});

		btnConfiguracio = addNavBtn(bottom, "btn_configuracio_sidebar", "tip_configuracio_sidebar");
		btnSobre = addNavBtn(bottom, "btn_sobre_sidebar", "tip_sobre_sidebar");

		bottom.add(makeSidebarSep());

		btnSortir = addNavBtn(bottom, "btn_sortir_sidebar", "tip_sortir_sidebar");
		btnSortir.setForeground(UITheme.palette().danger());
		btnSortir.addActionListener(e -> {
			if (javax.swing.JOptionPane.showConfirmDialog(this,
					I18n.t("confirm_exit_msg"), I18n.t("confirm_exit_title"),
					javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION)
				System.exit(0);
		});

		bottom.add(Box.createVerticalStrut(10));

		add(bottom, BorderLayout.SOUTH);
	}

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
			String label = l.obtenirNom() + " (" + counts.getOrDefault(l.obtenirId(), 0) + ")";
			JButton btn = sidebarShelfBtnMap.get(l.obtenirId());
			if (btn != null) {
				btn.setText("  " + label);
				btn.getAccessibleContext().setAccessibleName(label);
				applyShelfColor(btn, l.obtenirColor(), true);
			} else {
				btn = makeSidebarBtn("  " + label);
				btn.getAccessibleContext().setAccessibleName(label);
				applyShelfColor(btn, l.obtenirColor(), false);
				final int id = l.obtenirId();
				btn.addActionListener(e -> {
					int idx = findLlistaIndex(id);
					if (idx >= 0) comboLlistes.setSelectedIndex(idx);
				});
				if (onShelfRename != null) {
					btn.addMouseListener(new java.awt.event.MouseAdapter() {
						@Override public void mouseClicked(java.awt.event.MouseEvent e) {
							if (e.getClickCount() != 2) return;
							Llista current = findLlista(id);
							if (current != null) onShelfRename.accept(current);
						}
					});
				}
				sidebarShelfBtnMap.put(l.obtenirId(), btn);
				sidebarShelvesPanel.add(btn);
				GestorArrossegarSoltarPrestatgeria.attach(btn, l.obtenirId(), onDragToShelf);
			}
		}
		sidebarShelvesPanel.revalidate();
		sidebarShelvesPanel.repaint();
	}

	public void aplicarTheme() {
		aplicarBgToNonButtons(this, UITheme.palette().sidebarBg());
		for (JButton btn : sidebarBtns) styleSidebarBtn(btn);
		btnSortir.setForeground(UITheme.palette().danger());
		for (JButton btn : sidebarShelfBtnMap.values()) styleSidebarBtn(btn);
	}

	public void nameScrollBarButtons() {
		if (shelvesScroll != null) UIComponents.nameScrollBar(shelvesScroll.getVerticalScrollBar());
	}

	private JButton addNavBtn(JPanel container, String lbl, String tip) {
		JButton btn = makeSidebarBtn(I18n.t(lbl));
		btn.setToolTipText(I18n.t(tip));
		sidebarBtns.add(btn);
		container.add(btn);
		return btn;
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

	private void styleSidebarBtn(JButton btn) {
		UIComponents.styleSidebarButton(btn);
		btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
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

	private JButton makeSidebarBtn(String text) {
		JButton btn = new JButton(text);
		styleSidebarBtn(btn);
		btn.setAlignmentX(Component.LEFT_ALIGNMENT);
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseEntered(java.awt.event.MouseEvent e) {
				if (!btn.getBackground().equals(UITheme.palette().sidebarSelBg()))
					btn.setBackground(UITheme.palette().sidebarHoverBg());
			}
			@Override public void mouseExited(java.awt.event.MouseEvent e) {
				if (!btn.getBackground().equals(UITheme.palette().sidebarSelBg()))
					btn.setBackground(UITheme.palette().sidebarBg());
			}
		});
		return btn;
	}

	private JPanel makeSidebarSep() {
		JPanel sep = new JPanel();
		sep.setBackground(new Color(
			Math.min(UITheme.palette().sidebarBg().getRed()   + 30, 255),
			Math.min(UITheme.palette().sidebarBg().getGreen() + 25, 255),
			Math.min(UITheme.palette().sidebarBg().getBlue()  + 20, 255)));
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		sep.setPreferredSize(new Dimension(0, 1));
		sep.setAlignmentX(Component.LEFT_ALIGNMENT);
		return sep;
	}

	private JPanel makeSectionLabel(String text) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 6));
		p.setBackground(UITheme.palette().sidebarBg());
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
		lbl.setForeground(UITheme.palette().sidebarTextMid());
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

