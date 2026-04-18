package presentacio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;

import herramienta.UITheme;

public class MostrarBibliotecaPanel extends JPanel {

	private JTable jTableBilio;
	private JScrollPane scrollPaneJTable;
	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;
	private JPanel filterWrapper;
	private JButton btnSortir;
	private JButton btnThemeToggle;

	private JComboBox<String> comboBoxISBN;
	private JComboBox<String> comboBoxNom;
	private JComboBox<String> comboBoxAutor;
	private JTextField anyMin;
	private JTextField anyMax;
	private JTextField valoracioMin;
	private JTextField valoracioMax;
	private JTextField preuMin;
	private JTextField preuMax;
	private JCheckBox chckbxLlegit;
	private JCheckBox chckbxNoLlegit;

	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;
	private JButton btnNouLlibre;

	public MostrarBibliotecaPanel() {
		setLayout(new BorderLayout(8, 0));
		setBackground(UITheme.BG_MAIN);

		// ── Table ─────────────────────────────────────────────────────────────
		scrollPaneJTable = new JScrollPane();
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);

		jTableBilio = new JTable();
		jTableBilio.setDefaultEditor(Object.class, null);
		jTableBilio.setAutoCreateRowSorter(true);
		jTableBilio.getTableHeader().setReorderingAllowed(false);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		jTableBilio.setRowHeight(32);
		jTableBilio.setFont(UITheme.FONT_BASE);
		jTableBilio.setShowGrid(true);
		jTableBilio.setIntercellSpacing(new Dimension(0, 1));
		jTableBilio.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTableBilio.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
				JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
				lbl.setBackground(UITheme.HEADER_BG);
				lbl.setForeground(UITheme.HEADER_FG);
				lbl.setFont(UITheme.FONT_BOLD);
				lbl.setOpaque(true);
				lbl.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 2, 1, UITheme.BORDER_CLR),
					BorderFactory.createEmptyBorder(5, 10, 5, 10)
				));
				return lbl;
			}
		});
		scrollPaneJTable.setViewportView(jTableBilio);
		add(scrollPaneJTable, BorderLayout.CENTER);

		// ── Filter wrapper: scroll content + pinned buttons ───────────────────
		filterWrapper = new JPanel(new BorderLayout(0, 4));
		filterWrapper.setBackground(UITheme.BG_MAIN);
		filterWrapper.setPreferredSize(new Dimension(285, 0));
		filterWrapper.setMinimumSize(new Dimension(200, 0));
		add(filterWrapper, BorderLayout.WEST);

		scrolpaneFiltro = new JScrollPane();
		scrolpaneFiltro.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(UITheme.BORDER_CLR),
			"Filtres",
			TitledBorder.LEFT,
			TitledBorder.TOP,
			UITheme.FONT_BOLD,
			UITheme.TEXT_MID
		));
		filterWrapper.add(scrolpaneFiltro, BorderLayout.CENTER);

		panelFiltros = new JPanel();
		panelFiltros.setBackground(UITheme.BG_PANEL);
		panelFiltros.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panelFiltros.setLayout(null);
		panelFiltros.setPreferredSize(new Dimension(265, 674));
		scrolpaneFiltro.setViewportView(panelFiltros);

		// ── Llegit checkboxes ─────────────────────────────────────────────────
		chckbxLlegit = new JCheckBox("Llegit");
		chckbxLlegit.setFont(UITheme.FONT_BOLD);
		chckbxLlegit.setBackground(UITheme.BG_PANEL);
		chckbxLlegit.setForeground(UITheme.TEXT_DARK);
		chckbxLlegit.setToolTipText("Mostra només els llegits");
		chckbxLlegit.setBounds(10, 14, 230, 24);
		panelFiltros.add(chckbxLlegit);

		chckbxNoLlegit = new JCheckBox("No llegit");
		chckbxNoLlegit.setFont(UITheme.FONT_BOLD);
		chckbxNoLlegit.setBackground(UITheme.BG_PANEL);
		chckbxNoLlegit.setForeground(UITheme.TEXT_DARK);
		chckbxNoLlegit.setToolTipText("Mostra només els no llegits");
		chckbxNoLlegit.setBounds(10, 44, 230, 24);
		panelFiltros.add(chckbxNoLlegit);

		// ── ISBN ──────────────────────────────────────────────────────────────
		JLabel lblISBN = new JLabel("ISBN");
		lblISBN.setHorizontalAlignment(SwingConstants.LEFT);
		UITheme.styleLabel(lblISBN);
		lblISBN.setBounds(10, 82, 100, 22);
		panelFiltros.add(lblISBN);

		comboBoxISBN = new JComboBox<String>();
		comboBoxISBN.setFont(UITheme.FONT_BASE);
		comboBoxISBN.setToolTipText("Filtra per ISBN");
		comboBoxISBN.setBounds(10, 104, 245, 30);
		panelFiltros.add(comboBoxISBN);

		// ── Nom ───────────────────────────────────────────────────────────────
		JLabel lblNom = new JLabel("Nom");
		lblNom.setHorizontalAlignment(SwingConstants.LEFT);
		UITheme.styleLabel(lblNom);
		lblNom.setBounds(10, 148, 100, 22);
		panelFiltros.add(lblNom);

		comboBoxNom = new JComboBox<String>();
		comboBoxNom.setFont(UITheme.FONT_BASE);
		comboBoxNom.setToolTipText("Filtra per títol");
		comboBoxNom.setBounds(10, 170, 245, 30);
		panelFiltros.add(comboBoxNom);

		// ── Autor ─────────────────────────────────────────────────────────────
		JLabel lblAutor = new JLabel("Autor");
		lblAutor.setHorizontalAlignment(SwingConstants.LEFT);
		UITheme.styleLabel(lblAutor);
		lblAutor.setBounds(10, 214, 100, 22);
		panelFiltros.add(lblAutor);

		comboBoxAutor = new JComboBox<String>();
		comboBoxAutor.setFont(UITheme.FONT_BASE);
		comboBoxAutor.setToolTipText("Filtra per autor");
		comboBoxAutor.setBounds(10, 236, 245, 30);
		panelFiltros.add(comboBoxAutor);

		// ── Any (year range) ──────────────────────────────────────────────────
		JLabel lblAny = new JLabel("Any");
		UITheme.styleLabel(lblAny);
		lblAny.setBounds(10, 276, 100, 22);
		panelFiltros.add(lblAny);

		anyMin = new JTextField();
		anyMin.setColumns(5);
		UITheme.styleField(anyMin);
		anyMin.setToolTipText("Any mínim");
		anyMin.setBounds(10, 298, 108, 30);
		panelFiltros.add(anyMin);

		JLabel lblDashAny = new JLabel("–");
		lblDashAny.setHorizontalAlignment(SwingConstants.CENTER);
		lblDashAny.setForeground(UITheme.TEXT_MID);
		lblDashAny.setBounds(122, 298, 12, 30);
		panelFiltros.add(lblDashAny);

		anyMax = new JTextField();
		anyMax.setColumns(5);
		UITheme.styleField(anyMax);
		anyMax.setToolTipText("Any màxim");
		anyMax.setBounds(138, 298, 107, 30);
		panelFiltros.add(anyMax);

		// ── Valoració (range) ─────────────────────────────────────────────────
		JLabel lblValoracio = new JLabel("Valoració");
		UITheme.styleLabel(lblValoracio);
		lblValoracio.setBounds(10, 340, 100, 22);
		panelFiltros.add(lblValoracio);

		valoracioMin = new JTextField();
		valoracioMin.setColumns(5);
		UITheme.styleField(valoracioMin);
		valoracioMin.setToolTipText("Valoració mínima");
		valoracioMin.setBounds(10, 362, 108, 30);
		panelFiltros.add(valoracioMin);

		JLabel lblDashVal = new JLabel("–");
		lblDashVal.setHorizontalAlignment(SwingConstants.CENTER);
		lblDashVal.setForeground(UITheme.TEXT_MID);
		lblDashVal.setBounds(122, 362, 12, 30);
		panelFiltros.add(lblDashVal);

		valoracioMax = new JTextField();
		valoracioMax.setColumns(5);
		UITheme.styleField(valoracioMax);
		valoracioMax.setToolTipText("Valoració màxima");
		valoracioMax.setBounds(138, 362, 107, 30);
		panelFiltros.add(valoracioMax);

		// ── Preu (range) ──────────────────────────────────────────────────────
		JLabel lblPreu = new JLabel("Preu");
		UITheme.styleLabel(lblPreu);
		lblPreu.setBounds(10, 404, 100, 22);
		panelFiltros.add(lblPreu);

		preuMin = new JTextField();
		preuMin.setColumns(5);
		UITheme.styleField(preuMin);
		preuMin.setToolTipText("Preu mínim");
		preuMin.setBounds(10, 426, 108, 30);
		panelFiltros.add(preuMin);

		JLabel lblDashPreu = new JLabel("–");
		lblDashPreu.setHorizontalAlignment(SwingConstants.CENTER);
		lblDashPreu.setForeground(UITheme.TEXT_MID);
		lblDashPreu.setBounds(122, 426, 12, 30);
		panelFiltros.add(lblDashPreu);

		preuMax = new JTextField();
		preuMax.setColumns(5);
		UITheme.styleField(preuMax);
		preuMax.setToolTipText("Preu màxim");
		preuMax.setBounds(138, 426, 107, 30);
		panelFiltros.add(preuMax);

		// ── Action buttons ────────────────────────────────────────────────────
		JSeparator sep = new JSeparator();
		sep.setBounds(10, 468, 245, 2);
		panelFiltros.add(sep);

		bttnFiltrar = new JButton("Filtrar");
		bttnFiltrar.setBounds(10, 476, 245, 36);
		bttnFiltrar.setToolTipText("Aplicar filtres seleccionats");
		UITheme.styleAccentButton(bttnFiltrar);
		panelFiltros.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton("Treure filtres");
		bttnQuitarFiltros.setBounds(10, 520, 245, 36);
		bttnQuitarFiltros.setToolTipText("Treure tots els filtres aplicats");
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		panelFiltros.add(bttnQuitarFiltros);

		// ── Nou Llibre ────────────────────────────────────────────────────────
		JSeparator sep2 = new JSeparator();
		sep2.setBounds(10, 568, 245, 2);
		panelFiltros.add(sep2);

		btnNouLlibre = new JButton("+ Nou Llibre");
		UITheme.styleAccentButton(btnNouLlibre);
		btnNouLlibre.setBackground(new Color(0x27AE60));
		btnNouLlibre.setBounds(10, 576, 245, 36);
		btnNouLlibre.setToolTipText("Afegir un nou llibre (Ctrl+N)");
		panelFiltros.add(btnNouLlibre);

		// ── Dark/Light toggle ─────────────────────────────────────────────────
		JSeparator sep3 = new JSeparator();
		sep3.setBounds(10, 622, 245, 2);
		panelFiltros.add(sep3);

		btnThemeToggle = new JButton("Mode Fosc");
		UITheme.styleSecondaryButton(btnThemeToggle);
		btnThemeToggle.setBounds(10, 630, 245, 36);
		btnThemeToggle.setToolTipText("Canviar entre mode clar i fosc");
		btnThemeToggle.addActionListener(e -> {
			UITheme.setDark(!UITheme.isDark);
			applyTheme();
		});
		panelFiltros.add(btnThemeToggle);

		// ── Sortir pinned at bottom, outside scroll ───────────────────────────
		btnSortir = new JButton("Sortir");
		UITheme.styleSecondaryButton(btnSortir);
		btnSortir.setBackground(new Color(0xC0392B));
		btnSortir.setPreferredSize(new Dimension(0, 44));
		btnSortir.setToolTipText("Tancar l'aplicació");
		btnSortir.addActionListener(e -> System.exit(0));
		filterWrapper.add(btnSortir, BorderLayout.SOUTH);
	}

	// Re-applies all theme colors to this panel and propagates L&F to the window
	public void applyTheme() {
		setBackground(UITheme.BG_MAIN);
		filterWrapper.setBackground(UITheme.BG_MAIN);
		panelFiltros.setBackground(UITheme.BG_PANEL);
		scrolpaneFiltro.getViewport().setBackground(UITheme.BG_PANEL);
		scrolpaneFiltro.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(UITheme.BORDER_CLR),
			"Filtres", TitledBorder.LEFT, TitledBorder.TOP,
			UITheme.FONT_BOLD, UITheme.TEXT_MID
		));
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setForeground(UITheme.TEXT_DARK);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		UIManager.put("Table.alternateRowColor", UITheme.TABLE_ALT);

		for (Component c : panelFiltros.getComponents()) {
			if (c instanceof JCheckBox) {
				((JCheckBox) c).setBackground(UITheme.BG_PANEL);
				((JCheckBox) c).setForeground(UITheme.TEXT_DARK);
			}
			if (c instanceof JLabel) UITheme.styleLabel((JLabel) c);
			if (c instanceof JTextField) UITheme.styleField((JTextField) c);
		}

		// Re-apply window background before L&F update
		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.BG_MAIN);
		}

		// updateComponentTreeUI re-applies Nimbus (and resets BasicButtonUI), so
		// we re-style all custom buttons immediately after
		if (w != null) SwingUtilities.updateComponentTreeUI(w);

		UITheme.styleAccentButton(bttnFiltrar);
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		UITheme.styleAccentButton(btnNouLlibre);
		btnNouLlibre.setBackground(new Color(0x27AE60));
		UITheme.styleSecondaryButton(btnSortir);
		btnSortir.setBackground(new Color(0xC0392B));
		UITheme.styleSecondaryButton(btnThemeToggle);
		btnThemeToggle.setText(UITheme.isDark ? "Mode Clar" : "Mode Fosc");

		repaint();
	}

	public JButton getBtnNouLlibre() { return btnNouLlibre; }
	public JComboBox<String> getComboBoxISBN() { return comboBoxISBN; }
	public JComboBox<String> getComboBoxNom() { return comboBoxNom; }
	public JComboBox<String> getComboBoxAutor() { return comboBoxAutor; }
	public JTextField getAnyMin() { return anyMin; }
	public JTextField getAnyMax() { return anyMax; }
	public JTextField getValoracioMin() { return valoracioMin; }
	public JTextField getValoracioMax() { return valoracioMax; }
	public JTextField getPreuMin() { return preuMin; }
	public JTextField getPreuMax() { return preuMax; }
	public JTable getjTableBilio() { return jTableBilio; }
	public JPanel getPanelFiltros() { return panelFiltros; }
	public JScrollPane getScrollPaneJTable() { return scrollPaneJTable; }
	public JScrollPane getScrolpaneFiltro() { return scrolpaneFiltro; }
	public JCheckBox getchckbxLlegit() { return chckbxLlegit; }
	public JCheckBox getchckbxNoLlegit() { return chckbxNoLlegit; }
	public JButton getbtnFiltrar() { return bttnFiltrar; }
	public JButton getbttnQuitarFiltros() { return bttnQuitarFiltros; }
}
