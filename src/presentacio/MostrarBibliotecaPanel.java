package presentacio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
	private JPanel paginationPanel;
	private JButton btnPaginaAnterior;
	private JButton btnPaginaSeguent;
	private JLabel lblPagina;
	private JButton btnSortir;
	private JButton btnThemeToggle;

	private JTextField searchBar;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField anyMin;
	private JTextField anyMax;
	private JTextField valoracioMin;
	private JTextField valoracioMax;
	private JTextField preuMin;
	private JTextField preuMax;
	private JCheckBox chckbxLlegit;
	private JCheckBox chckbxNoLlegit;

	private JComboBox<Object> comboLlistes;
	private JButton btnGestioLlistes;

	private JComboBox<String> comboPresets;
	private JButton btnCarregaPreset;
	private JButton btnDesaPreset;
	private JButton btnEsborraPreset;

	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;
	private JButton btnNouLlibre;
	private JButton btnAfegitsRecentment;
	private JButton btnLlegitsRecentment;
	private JButton btnExportCSV;
	private JButton btnImportarCSV;
	private JButton btnEscanejarISBN;
	private JButton btnEstadistiques;
	private JButton btnLlibreAleatori;
	private JButton btnBackupBD;
	private JButton btnRestaurarBD;
	private JButton btnConfiguracio;

	public MostrarBibliotecaPanel() {
		setLayout(new BorderLayout(8, 0));
		setBackground(UITheme.BG_MAIN);

		// ── North panel: search bar + shelf selector ───────────────────────────
		JPanel northPanel = new JPanel(new BorderLayout(0, 3));
		northPanel.setBackground(UITheme.BG_MAIN);
		northPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

		// Row 1: search bar
		JPanel searchRow = new JPanel(new BorderLayout(6, 0));
		searchRow.setBackground(UITheme.BG_MAIN);
		JLabel searchIcon = new JLabel("Cerca:");
		UITheme.styleLabel(searchIcon);
		searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
		searchRow.add(searchIcon, BorderLayout.WEST);
		searchBar = new JTextField();
		UITheme.styleField(searchBar);
		searchBar.setToolTipText("Cerca ràpida per qualsevol camp (ISBN, nom, autor, any...)");
		searchRow.add(searchBar, BorderLayout.CENTER);
		northPanel.add(searchRow, BorderLayout.NORTH);

		// Row 2: shelf selector
		JPanel shelfRow = new JPanel(new BorderLayout(6, 0));
		shelfRow.setBackground(UITheme.BG_MAIN);
		JLabel lblLlista = new JLabel("Llista:");
		UITheme.styleLabel(lblLlista);
		lblLlista.setPreferredSize(new Dimension(45, 28));
		shelfRow.add(lblLlista, BorderLayout.WEST);
		comboLlistes = new JComboBox<>();
		shelfRow.add(comboLlistes, BorderLayout.CENTER);
		btnGestioLlistes = new JButton("Gestionar llistes");
		UITheme.styleSecondaryButton(btnGestioLlistes);
		shelfRow.add(btnGestioLlistes, BorderLayout.EAST);
		northPanel.add(shelfRow, BorderLayout.CENTER);

		// Row 3: quick-filter buttons
		JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		quickRow.setBackground(UITheme.BG_MAIN);
		btnAfegitsRecentment = new JButton("Afegits recentment");
		UITheme.styleSecondaryButton(btnAfegitsRecentment);
		btnAfegitsRecentment.setToolTipText("Mostra els 20 últims llibres afegits");
		quickRow.add(btnAfegitsRecentment);
		btnLlegitsRecentment = new JButton("Llegits");
		UITheme.styleSecondaryButton(btnLlegitsRecentment);
		btnLlegitsRecentment.setToolTipText("Mostra tots els llibres marcats com a llegits");
		quickRow.add(btnLlegitsRecentment);
		northPanel.add(quickRow, BorderLayout.SOUTH);

		add(northPanel, BorderLayout.NORTH);

		// ── Table ─────────────────────────────────────────────────────────────
		scrollPaneJTable = new JScrollPane();
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);

		jTableBilio = new JTable() {
			@Override
			public String getToolTipText(java.awt.event.MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				if (row < 0 || col < 0) return null;
				Object val = getValueAt(row, col);
				if (val == null) return null;
				String text = val.toString();
				if (text.isBlank()) return null;
				java.awt.Rectangle r = getCellRect(row, col, false);
				java.awt.FontMetrics fm = getFontMetrics(getFont());
				return fm.stringWidth(text) > r.width ? text : null;
			}
		};
		jTableBilio.setDefaultEditor(Object.class, null);
		jTableBilio.setAutoCreateRowSorter(true);
		jTableBilio.getTableHeader().setReorderingAllowed(false);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setSelectionBackground(UITheme.ACCENT);
		jTableBilio.setSelectionForeground(Color.WHITE);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		jTableBilio.setRowHeight(32);
		jTableBilio.setFont(UITheme.FONT_BASE);
		jTableBilio.setShowGrid(true);
		jTableBilio.setIntercellSpacing(new Dimension(0, 1));
		jTableBilio.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		String[] headerTips = {"Número ISBN del llibre", "Títol del llibre", "Nom de l'autor",
			"Any de publicació", "Valoració de 0 a 10", "Preu en euros", "Estat de lectura (clic per canviar)", "Obrir detalls"};
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
				String text = v != null ? v.toString() : "";
				javax.swing.RowSorter<?> sorter = t.getRowSorter();
				if (sorter != null) {
					java.util.List<? extends javax.swing.RowSorter.SortKey> keys = sorter.getSortKeys();
					if (!keys.isEmpty() && keys.get(0).getColumn() == c) {
						text += keys.get(0).getSortOrder() == javax.swing.SortOrder.ASCENDING ? "  ▲" : "  ▼";
					}
				}
				lbl.setText(text);
				lbl.setToolTipText(c < headerTips.length ? headerTips[c] : null);
				return lbl;
			}
		});
		scrollPaneJTable.setViewportView(jTableBilio);

		// Pagination bar below table
		paginationPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 12, 4));
		paginationPanel.setBackground(UITheme.BG_MAIN);
		btnPaginaAnterior = new JButton("‹ Anterior");
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		lblPagina = new JLabel("Pàgina 1 / 1");
		UITheme.styleLabel(lblPagina);
		btnPaginaSeguent = new JButton("Seguent ›");
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		paginationPanel.add(btnPaginaAnterior);
		paginationPanel.add(lblPagina);
		paginationPanel.add(btnPaginaSeguent);
		paginationPanel.setVisible(false);

		JPanel centerWrapper = new JPanel(new BorderLayout(0, 2));
		centerWrapper.setBackground(UITheme.BG_MAIN);
		centerWrapper.add(scrollPaneJTable, BorderLayout.CENTER);
		centerWrapper.add(paginationPanel, BorderLayout.SOUTH);
		add(centerWrapper, BorderLayout.CENTER);

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
		panelFiltros.setPreferredSize(new Dimension(265, 1068));
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

		textISBN = new JTextField();
		UITheme.styleField(textISBN);
		textISBN.setToolTipText("Filtra per ISBN (cerca parcial)");
		textISBN.setBounds(10, 104, 245, 30);
		panelFiltros.add(textISBN);

		// ── Nom ───────────────────────────────────────────────────────────────
		JLabel lblNom = new JLabel("Nom");
		lblNom.setHorizontalAlignment(SwingConstants.LEFT);
		UITheme.styleLabel(lblNom);
		lblNom.setBounds(10, 148, 100, 22);
		panelFiltros.add(lblNom);

		textNom = new JTextField();
		UITheme.styleField(textNom);
		textNom.setToolTipText("Filtra per títol (cerca parcial)");
		textNom.setBounds(10, 170, 245, 30);
		panelFiltros.add(textNom);

		// ── Autor ─────────────────────────────────────────────────────────────
		JLabel lblAutor = new JLabel("Autor");
		lblAutor.setHorizontalAlignment(SwingConstants.LEFT);
		UITheme.styleLabel(lblAutor);
		lblAutor.setBounds(10, 214, 100, 22);
		panelFiltros.add(lblAutor);

		textAutor = new JTextField();
		UITheme.styleField(textAutor);
		textAutor.setToolTipText("Filtra per autor (cerca parcial)");
		textAutor.setBounds(10, 236, 245, 30);
		panelFiltros.add(textAutor);

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

		// ── Export CSV ────────────────────────────────────────────────────────
		JSeparator sep4 = new JSeparator();
		sep4.setBounds(10, 674, 245, 2);
		panelFiltros.add(sep4);

		btnExportCSV = new JButton("Exportar CSV");
		UITheme.styleSecondaryButton(btnExportCSV);
		btnExportCSV.setBounds(10, 682, 245, 36);
		btnExportCSV.setToolTipText("Exportar la llista actual a CSV");
		panelFiltros.add(btnExportCSV);

		// ── Import CSV + Barcode ─────────────────────────────────────────────
		JSeparator sep5 = new JSeparator();
		sep5.setBounds(10, 726, 245, 2);
		panelFiltros.add(sep5);

		btnImportarCSV = new JButton("Importar CSV");
		UITheme.styleSecondaryButton(btnImportarCSV);
		btnImportarCSV.setBounds(10, 734, 245, 36);
		btnImportarCSV.setToolTipText("Importar llibres des d'un fitxer CSV");
		panelFiltros.add(btnImportarCSV);

		btnEscanejarISBN = new JButton("Escanejar ISBN");
		UITheme.styleSecondaryButton(btnEscanejarISBN);
		btnEscanejarISBN.setBounds(10, 778, 245, 36);
		btnEscanejarISBN.setToolTipText("Introduir ISBN i auto-omplir dades d'OpenLibrary");
		panelFiltros.add(btnEscanejarISBN);

		// ── Statistics ────────────────────────────────────────────────────────
		JSeparator sep6 = new JSeparator();
		sep6.setBounds(10, 822, 245, 2);
		panelFiltros.add(sep6);

		btnEstadistiques = new JButton("Estadístiques");
		UITheme.styleSecondaryButton(btnEstadistiques);
		btnEstadistiques.setBounds(10, 830, 245, 36);
		btnEstadistiques.setToolTipText("Mostrar estadístiques de la biblioteca");
		panelFiltros.add(btnEstadistiques);

		btnLlibreAleatori = new JButton("Llibre Aleatori");
		UITheme.styleSecondaryButton(btnLlibreAleatori);
		btnLlibreAleatori.setBounds(10, 874, 245, 36);
		btnLlibreAleatori.setToolTipText("Tria un llibre no llegit a l'atzar de la vista actual");
		panelFiltros.add(btnLlibreAleatori);

		// ── Backup / Restore ─────────────────────────────────────────────────
		JSeparator sep7 = new JSeparator();
		sep7.setBounds(10, 918, 245, 2);
		panelFiltros.add(sep7);

		btnBackupBD = new JButton("Backup BD");
		UITheme.styleSecondaryButton(btnBackupBD);
		btnBackupBD.setBounds(10, 926, 245, 36);
		btnBackupBD.setToolTipText("Exportar tota la base de dades a un fitxer SQL");
		panelFiltros.add(btnBackupBD);

		btnRestaurarBD = new JButton("Restaurar BD");
		UITheme.styleSecondaryButton(btnRestaurarBD);
		btnRestaurarBD.setBounds(10, 970, 245, 36);
		btnRestaurarBD.setToolTipText("Restaurar la base de dades des d'un fitxer SQL de backup");
		panelFiltros.add(btnRestaurarBD);

		// ── Settings ─────────────────────────────────────────────────────────
		JSeparator sep8 = new JSeparator();
		sep8.setBounds(10, 1014, 245, 2);
		panelFiltros.add(sep8);

		btnConfiguracio = new JButton("Configuració");
		UITheme.styleSecondaryButton(btnConfiguracio);
		btnConfiguracio.setBounds(10, 1022, 245, 36);
		btnConfiguracio.setToolTipText("Configuració: BD, carpeta d'imatges...");
		panelFiltros.add(btnConfiguracio);

		// ── Preset bar: always visible above filter scroll ────────────────────
		JPanel presetBar = new JPanel(null);
		presetBar.setBackground(UITheme.BG_MAIN);
		presetBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CLR));
		presetBar.setPreferredSize(new Dimension(0, 66));

		JLabel lblPresets = new JLabel("Presets:");
		UITheme.styleLabel(lblPresets);
		lblPresets.setBounds(4, 6, 54, 20);
		presetBar.add(lblPresets);

		comboPresets = new JComboBox<>();
		comboPresets.setFont(UITheme.FONT_BASE);
		comboPresets.setToolTipText("Selecciona un preset guardat");
		comboPresets.setBounds(58, 4, 135, 26);
		presetBar.add(comboPresets);

		btnCarregaPreset = new JButton("Carrega");
		UITheme.styleAccentButton(btnCarregaPreset);
		btnCarregaPreset.setToolTipText("Aplica el preset seleccionat als filtres");
		btnCarregaPreset.setBounds(197, 4, 83, 26);
		presetBar.add(btnCarregaPreset);

		btnDesaPreset = new JButton("Desa filtre");
		UITheme.styleSecondaryButton(btnDesaPreset);
		btnDesaPreset.setToolTipText("Guarda el filtre actual com a preset");
		btnDesaPreset.setBounds(4, 36, 130, 26);
		presetBar.add(btnDesaPreset);

		btnEsborraPreset = new JButton("Esborra preset");
		UITheme.styleSecondaryButton(btnEsborraPreset);
		btnEsborraPreset.setToolTipText("Elimina el preset seleccionat");
		btnEsborraPreset.setBounds(138, 36, 142, 26);
		presetBar.add(btnEsborraPreset);

		filterWrapper.add(presetBar, BorderLayout.NORTH);

		// ── Sortir pinned at bottom, outside scroll ───────────────────────────
		btnSortir = new JButton("Sortir");
		UITheme.styleSecondaryButton(btnSortir);
		btnSortir.setBackground(new Color(0xC0392B));
		btnSortir.setPreferredSize(new Dimension(0, 44));
		btnSortir.setToolTipText("Tancar l'aplicació");
		btnSortir.addActionListener(e -> {
			if (javax.swing.JOptionPane.showConfirmDialog(this,
					"Sortir de l'aplicació?", "Confirmar sortida",
					javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION)
				System.exit(0);
		});
		filterWrapper.add(btnSortir, BorderLayout.SOUTH);
	}

	public void applyTheme() {
		UITheme.rebuildFonts(herramienta.Config.getFontSize());
		herramienta.Config.setDarkMode(UITheme.isDark);
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
		jTableBilio.setSelectionBackground(UITheme.ACCENT);
		jTableBilio.setSelectionForeground(java.awt.Color.WHITE);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		UIManager.put("Table.alternateRowColor", UITheme.TABLE_ALT);
		UITheme.styleField(searchBar);

		for (Component c : panelFiltros.getComponents()) {
			if (c instanceof JCheckBox) {
				((JCheckBox) c).setBackground(UITheme.BG_PANEL);
				((JCheckBox) c).setForeground(UITheme.TEXT_DARK);
			}
			if (c instanceof JLabel) UITheme.styleLabel((JLabel) c);
			if (c instanceof JTextField) UITheme.styleField((JTextField) c);
		}

		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.BG_MAIN);
		}

		// Reinstall Nimbus so it re-derives colors from updated UIManager keys.
		// Without this, Nimbus uses its internally cached palette from startup.
		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
			UITheme.applyUIManager(); // re-apply our keys on top of fresh L&F defaults
		} catch (Exception ignored) {}
		if (w != null) SwingUtilities.updateComponentTreeUI(w);

		UITheme.styleAccentButton(bttnFiltrar);
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		UITheme.styleAccentButton(btnNouLlibre);
		btnNouLlibre.setBackground(new Color(0x27AE60));
		UITheme.styleSecondaryButton(btnSortir);
		btnSortir.setBackground(new Color(0xC0392B));
		UITheme.styleSecondaryButton(btnThemeToggle);
		btnThemeToggle.setText(UITheme.isDark ? "Mode Clar" : "Mode Fosc");
		UITheme.styleSecondaryButton(btnExportCSV);
		UITheme.styleSecondaryButton(btnImportarCSV);
		UITheme.styleSecondaryButton(btnEscanejarISBN);
		UITheme.styleSecondaryButton(btnEstadistiques);
		UITheme.styleSecondaryButton(btnLlibreAleatori);
		UITheme.styleSecondaryButton(btnBackupBD);
		UITheme.styleSecondaryButton(btnRestaurarBD);
		UITheme.styleSecondaryButton(btnConfiguracio);
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		UITheme.styleLabel(lblPagina);
		paginationPanel.setBackground(UITheme.BG_MAIN);
		UITheme.styleSecondaryButton(btnGestioLlistes);
		UITheme.styleSecondaryButton(btnAfegitsRecentment);
		UITheme.styleSecondaryButton(btnLlegitsRecentment);
		UITheme.styleAccentButton(btnCarregaPreset);
		UITheme.styleSecondaryButton(btnDesaPreset);
		UITheme.styleSecondaryButton(btnEsborraPreset);
		comboPresets.setFont(UITheme.FONT_BASE);

		repaint();
	}

	public JButton getBtnPaginaAnterior() { return btnPaginaAnterior; }
	public JButton getBtnPaginaSeguent() { return btnPaginaSeguent; }
	public JLabel getLblPagina() { return lblPagina; }
	public JPanel getPaginationPanel() { return paginationPanel; }

	public JButton getBtnNouLlibre() { return btnNouLlibre; }
	public JButton getBtnExportCSV() { return btnExportCSV; }
	public JButton getBtnImportarCSV() { return btnImportarCSV; }
	public JButton getBtnEscanejarISBN() { return btnEscanejarISBN; }
	public JButton getBtnEstadistiques() { return btnEstadistiques; }
	public JButton getBtnLlibreAleatori() { return btnLlibreAleatori; }
	public JButton getBtnBackupBD() { return btnBackupBD; }
	public JButton getBtnRestaurarBD() { return btnRestaurarBD; }
	public JButton getBtnConfiguracio() { return btnConfiguracio; }
	public JTextField getSearchBar() { return searchBar; }
	public JTextField getTextISBN() { return textISBN; }
	public JTextField getTextNom() { return textNom; }
	public JTextField getTextAutor() { return textAutor; }
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
	public JComboBox<Object> getComboLlistes() { return comboLlistes; }
	public JButton getBtnGestioLlistes() { return btnGestioLlistes; }
	public JButton getBtnAfegitsRecentment() { return btnAfegitsRecentment; }
	public JButton getBtnLlegitsRecentment() { return btnLlegitsRecentment; }
	public JComboBox<String> getComboPresets() { return comboPresets; }
	public JButton getBtnCarregaPreset() { return btnCarregaPreset; }
	public JButton getBtnDesaPreset() { return btnDesaPreset; }
	public JButton getBtnEsborraPreset() { return btnEsborraPreset; }
}
