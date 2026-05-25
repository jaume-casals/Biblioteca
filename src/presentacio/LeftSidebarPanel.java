package presentacio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import domini.Llista;
import herramienta.ColorUtils;
import herramienta.I18n;
import herramienta.UITheme;

public class LeftSidebarPanel extends JPanel {

	private final JComboBox<Object> comboLlistes;
	private final Runnable onThemeChange;

	private JScrollPane shelvesScroll;
	private JPanel sidebarShelvesPanel;
	private final Map<Integer, JButton> sidebarShelfBtnMap = new HashMap<>();
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

	public LeftSidebarPanel(JComboBox<Object> comboLlistes, Runnable onThemeChange) {
		this.comboLlistes = comboLlistes;
		this.onThemeChange = onThemeChange;
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.SIDEBAR_BG);
		setPreferredSize(new Dimension(220, 0));
		setMinimumSize(new Dimension(220, 0));
		buildSidebar();
	}

	private void buildSidebar() {
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(UITheme.SIDEBAR_BG);

		JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 14));
		logoPanel.setBackground(UITheme.SIDEBAR_BG);
		logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		JLabel logo = new JLabel("biblioteca");
		logo.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
		logo.setForeground(UITheme.SIDEBAR_TEXT);
		logoPanel.add(logo);
		top.add(logoPanel);

		top.add(makeSidebarSep());

		top.add(makeSectionLabel(I18n.t("lbl_sidebar_nav")));

		btnTotsElsLlibres = makeSidebarBtn(I18n.t("btn_tots_els_llibres"));
		btnTotsElsLlibres.setToolTipText(I18n.t("tip_tots_els_llibres"));
		btnTotsElsLlibres.addActionListener(e -> {
			if (comboLlistes.getItemCount() > 0) comboLlistes.setSelectedIndex(0);
		});
		sidebarBtns.add(btnTotsElsLlibres);
		top.add(btnTotsElsLlibres);

		btnAfegitsRecentment = makeSidebarBtn(I18n.t("btn_afegits_recentment"));
		btnAfegitsRecentment.setToolTipText(I18n.t("tip_afegits_recentment"));
		sidebarBtns.add(btnAfegitsRecentment);
		top.add(btnAfegitsRecentment);

		btnLlegitsRecentment = makeSidebarBtn(I18n.t("btn_llegits_sidebar"));
		btnLlegitsRecentment.setToolTipText(I18n.t("tip_llegits_sidebar"));
		sidebarBtns.add(btnLlegitsRecentment);
		top.add(btnLlegitsRecentment);

		btnDesitjats = makeSidebarBtn(I18n.t("btn_desitjats_sidebar"));
		btnDesitjats.setToolTipText(I18n.t("tip_desitjats_sidebar"));
		sidebarBtns.add(btnDesitjats);
		top.add(btnDesitjats);

		btnEnCurs = makeSidebarBtn(I18n.t("btn_en_curs_sidebar"));
		btnEnCurs.setToolTipText(I18n.t("tip_en_curs_sidebar"));
		sidebarBtns.add(btnEnCurs);
		top.add(btnEnCurs);

		top.add(makeSidebarSep());

		top.add(makeSectionLabel(I18n.t("lbl_sidebar_lists")));

		add(top, BorderLayout.NORTH);

		sidebarShelvesPanel = new JPanel();
		sidebarShelvesPanel.setLayout(new BoxLayout(sidebarShelvesPanel, BoxLayout.Y_AXIS));
		sidebarShelvesPanel.setBackground(UITheme.SIDEBAR_BG);

		shelvesScroll = new JScrollPane(sidebarShelvesPanel);
		shelvesScroll.setBorder(null);
		shelvesScroll.getVerticalScrollBar().setUnitIncrement(12);
		shelvesScroll.setBackground(UITheme.SIDEBAR_BG);
		shelvesScroll.getViewport().setBackground(UITheme.SIDEBAR_BG);
		shelvesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(shelvesScroll, BorderLayout.CENTER);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
		bottom.setBackground(UITheme.SIDEBAR_BG);

		bottom.add(makeSidebarSep());

		btnEstadistiques = makeSidebarBtn(I18n.t("btn_estadistiques_sidebar"));
		btnEstadistiques.setToolTipText(I18n.t("tip_estadistiques_sidebar"));
		sidebarBtns.add(btnEstadistiques);
		bottom.add(btnEstadistiques);

		btnLlibreAleatori = makeSidebarBtn(I18n.t("btn_aleatori_sidebar"));
		btnLlibreAleatori.setToolTipText(I18n.t("tip_aleatori_sidebar"));
		sidebarBtns.add(btnLlibreAleatori);
		bottom.add(btnLlibreAleatori);

		bottom.add(makeSidebarSep());

		btnGestioLlistes = makeSidebarBtn(I18n.t("btn_gestio_llistes_sidebar"));
		btnGestioLlistes.setToolTipText(I18n.t("tip_gestio_llistes_sidebar"));
		sidebarBtns.add(btnGestioLlistes);
		bottom.add(btnGestioLlistes);

		bottom.add(makeSidebarSep());

		btnThemeToggle = makeSidebarBtn(I18n.t("btn_theme"));
		btnThemeToggle.setToolTipText(I18n.t("tip_mode_fosc"));
		btnThemeToggle.addActionListener(e -> {
			herramienta.UITheme.Theme[] themes = herramienta.UITheme.Theme.values();
			herramienta.UITheme.Theme next = themes[(UITheme.getTheme().ordinal() + 1) % themes.length];
			UITheme.setTheme(next);
			onThemeChange.run();
		});
		sidebarBtns.add(btnThemeToggle);
		bottom.add(btnThemeToggle);

		btnConfiguracio = makeSidebarBtn(I18n.t("btn_configuracio_sidebar"));
		btnConfiguracio.setToolTipText(I18n.t("tip_configuracio_sidebar"));
		sidebarBtns.add(btnConfiguracio);
		bottom.add(btnConfiguracio);

		btnSobre = makeSidebarBtn(I18n.t("btn_sobre_sidebar"));
		btnSobre.setToolTipText(I18n.t("tip_sobre_sidebar"));
		sidebarBtns.add(btnSobre);
		bottom.add(btnSobre);

		bottom.add(makeSidebarSep());

		btnSortir = makeSidebarBtn(I18n.t("btn_sortir_sidebar"));
		btnSortir.setForeground(new Color(0xFF8080));
		btnSortir.setToolTipText(I18n.t("tip_sortir_sidebar"));
		btnSortir.addActionListener(e -> {
			if (javax.swing.JOptionPane.showConfirmDialog(this,
					I18n.t("confirm_exit_msg"), I18n.t("confirm_exit_title"),
					javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION)
				System.exit(0);
		});
		sidebarBtns.add(btnSortir);
		bottom.add(btnSortir);

		bottom.add(Box.createVerticalStrut(10));

		add(bottom, BorderLayout.SOUTH);
	}

	public void rebuildSidebarShelves(List<Llista> llistes, Map<Integer, Integer> counts,
			java.util.function.BiConsumer<Integer, List<Long>> onDragToShelf,
			java.util.function.Consumer<Llista> onShelfRename) {
		Set<Integer> activeIds = new HashSet<>();
		for (Llista l : llistes) activeIds.add(l.getId());
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
			String label = l.getNom() + " (" + counts.getOrDefault(l.getId(), 0) + ")";
			JButton btn = sidebarShelfBtnMap.get(l.getId());
			if (btn != null) {
				btn.setText("  " + label);
				btn.getAccessibleContext().setAccessibleName(label);
				if (l.getColor() != null) {
					try {
						Color c = Color.decode(l.getColor());
						btn.setIcon(ColorUtils.colorSwatch(c));
						btn.setHorizontalTextPosition(JButton.RIGHT);
					} catch (Exception ignored) { btn.setIcon(null); }
				} else {
					btn.setIcon(null);
				}
			} else {
				btn = makeSidebarBtn("  " + label);
				btn.getAccessibleContext().setAccessibleName(label);
				if (l.getColor() != null) {
					try {
						Color c = Color.decode(l.getColor());
						btn.setIcon(ColorUtils.colorSwatch(c));
						btn.setHorizontalTextPosition(JButton.RIGHT);
					} catch (Exception ignored) {}
				}
				final int id = l.getId();
				btn.addActionListener(e -> {
					for (int i = 0; i < comboLlistes.getItemCount(); i++) {
						Object item = comboLlistes.getItemAt(i);
						if (item instanceof Llista && ((Llista) item).getId() == id) {
							comboLlistes.setSelectedIndex(i);
							break;
						}
					}
				});
				if (onShelfRename != null) {
					btn.addMouseListener(new java.awt.event.MouseAdapter() {
						@Override public void mouseClicked(java.awt.event.MouseEvent e) {
							if (e.getClickCount() == 2) onShelfRename.accept(l);
						}
					});
				}
				sidebarShelfBtnMap.put(l.getId(), btn);
				sidebarShelvesPanel.add(btn);
				ShelfDragDropHandler.attach(btn, l.getId(), onDragToShelf);
			}
		}
		sidebarShelvesPanel.revalidate();
		sidebarShelvesPanel.repaint();
	}

	public void applyTheme() {
		applyBgToNonButtons(this, UITheme.SIDEBAR_BG);
		for (JButton btn : sidebarBtns) {
			UITheme.styleSidebarButton(btn);
			btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
		}
		btnSortir.setForeground(new Color(0xFF8080));
		for (JButton btn : sidebarShelfBtnMap.values()) {
			UITheme.styleSidebarButton(btn);
			btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
		}
	}

	public void nameScrollBarButtons() {
		if (shelvesScroll != null) nameScrollBar(shelvesScroll.getVerticalScrollBar());
	}

	private JButton makeSidebarBtn(String text) {
		JButton btn = new JButton(text);
		UITheme.styleSidebarButton(btn);
		btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
		btn.setAlignmentX(Component.LEFT_ALIGNMENT);
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseEntered(java.awt.event.MouseEvent e) {
				if (!btn.getBackground().equals(UITheme.SIDEBAR_SEL_BG))
					btn.setBackground(new Color(
						Math.min(UITheme.SIDEBAR_BG.getRed()   + 20, 255),
						Math.min(UITheme.SIDEBAR_BG.getGreen() + 15, 255),
						Math.min(UITheme.SIDEBAR_BG.getBlue()  + 10, 255)));
			}
			@Override public void mouseExited(java.awt.event.MouseEvent e) {
				if (!btn.getBackground().equals(UITheme.SIDEBAR_SEL_BG))
					btn.setBackground(UITheme.SIDEBAR_BG);
			}
		});
		return btn;
	}

	private JPanel makeSidebarSep() {
		JPanel sep = new JPanel();
		sep.setBackground(new Color(
			Math.min(UITheme.SIDEBAR_BG.getRed()   + 30, 255),
			Math.min(UITheme.SIDEBAR_BG.getGreen() + 25, 255),
			Math.min(UITheme.SIDEBAR_BG.getBlue()  + 20, 255)));
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		sep.setPreferredSize(new Dimension(0, 1));
		sep.setAlignmentX(Component.LEFT_ALIGNMENT);
		return sep;
	}

	private JPanel makeSectionLabel(String text) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 6));
		p.setBackground(UITheme.SIDEBAR_BG);
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel lbl = new JLabel(text);
		lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
		lbl.setForeground(UITheme.SIDEBAR_TEXT_MID);
		p.add(lbl);
		return p;
	}

	private void applyBgToNonButtons(Component comp, Color bg) {
		if (comp instanceof JButton) return;
		comp.setBackground(bg);
		if (comp instanceof JScrollPane) {
			((JScrollPane) comp).getViewport().setBackground(bg);
		}
		if (comp instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) comp).getComponents()) {
				if (!(child instanceof javax.swing.JTable)) applyBgToNonButtons(child, bg);
			}
		}
	}

	private void nameScrollBar(javax.swing.JScrollBar sb) {
		Component[] comps = sb.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].getAccessibleContext().setAccessibleName(
				i == 0 ? I18n.t("acc_scroll_up") : I18n.t("acc_scroll_down"));
		}
	}

	public JButton getBtnAfegitsRecentment()  { return btnAfegitsRecentment; }
	public JButton getBtnLlegitsRecentment()  { return btnLlegitsRecentment; }
	public JButton getBtnDesitjats()          { return btnDesitjats; }
	public JButton getBtnEnCurs()             { return btnEnCurs; }
	public JButton getBtnEstadistiques()      { return btnEstadistiques; }
	public JButton getBtnLlibreAleatori()     { return btnLlibreAleatori; }
	public JButton getBtnGestioLlistes()      { return btnGestioLlistes; }
	public JButton getBtnConfiguracio()       { return btnConfiguracio; }
	public JButton getBtnSobre()              { return btnSobre; }
}
