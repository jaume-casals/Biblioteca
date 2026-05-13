package presentacio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import herramienta.I18n;
import herramienta.UITheme;

public class MostrarBibliotecaPanel extends JPanel {

	// ── Table ─────────────────────────────────────────────────────────────────
	private JTable jTableBilio;
	private JScrollPane scrollPaneJTable;
	private JPanel paginationPanel;
	private JButton btnPaginaAnterior;
	private JButton btnPaginaSeguent;
	private JLabel lblPagina;

	// ── Filter panel (horizontal rows, in collapsible drawer) ────────────────
	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;
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
	private JButton bttnFiltrar;
	private JButton bttnQuitarFiltros;

	// ── Search bar (topbar) ───────────────────────────────────────────────────
	private JTextField searchBar;

	// ── Shelf combo (hidden, functional) ─────────────────────────────────────
	private JComboBox<Object> comboLlistes;
	private JButton btnGestioLlistes;

	// ── Tag filter combo ───────────────────────────────────────────────────────
	private JComboBox<Object> comboTagFilter;

	// ── Extra filter fields ───────────────────────────────────────────────────
	private JTextField filterEditorial;
	private JTextField filterSerie;
	private JTextField filterIdioma;
	private JComboBox<String> filterFormat;

	// ── Presets ───────────────────────────────────────────────────────────────
	private JComboBox<String> comboPresets;
	private JButton btnCarregaPreset;
	private JButton btnDesaPreset;
	private JButton btnEsborraPreset;

	// ── Action buttons ────────────────────────────────────────────────────────
	private JButton btnNouLlibre;
	private JButton btnAfegitsRecentment;
	private JButton btnLlegitsRecentment;
	private JButton btnDesitjats;
	private JButton btnEnCurs;
	private JButton btnExportCSV;
	private JButton btnImportarCSV;
	private JButton btnImportarCalibre;
	private JButton btnExportJSON;
	private JButton btnImportarJSON;
	private JButton btnExportHTML;
	private JButton btnExportPDF;
	private JButton btnExportDropdown;
	private JButton btnImportDropdown;
	private JButton btnFetchCovers;
	private JButton btnEscanejarISBN;
	private JButton btnEstadistiques;
	private JButton btnLlibreAleatori;
	private JButton btnBackupBD;
	private JButton btnRestaurarBD;
	private JButton btnConfiguracio;
	private JButton btnSobre;
	private JButton btnSortir;
	private JButton btnThemeToggle;

	// ── New layout components ─────────────────────────────────────────────────
	private JButton btnToggleFiltres;
	private JButton btnToggleVista;
	private JButton btnGroupSeries;
	private JPanel filterDrawer;
	private JPanel contentCards;
	private java.awt.CardLayout cardLayout;
	private GaleriaCobertesPanel galeria;
	private JPanel sidebarShelvesPanel;
	private final Map<Integer, JButton> sidebarShelfBtnMap = new HashMap<>();
	private JButton btnTotsElsLlibres;
	private final List<JButton> sidebarBtns = new ArrayList<>();
	private boolean galeriaMode = false;

	// Reference to sidebar for applyTheme
	private JPanel sidebar;

	private java.util.function.BiConsumer<Integer, java.util.List<Long>> onDragToShelf;

	public MostrarBibliotecaPanel() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.BG_MAIN);

		// Hidden but functional shelf combo
		comboLlistes = new JComboBox<>();

		sidebar = buildSidebar();
		add(sidebar, BorderLayout.WEST);

		JPanel rightArea = new JPanel(new BorderLayout(0, 0));
		rightArea.setBackground(UITheme.BG_MAIN);

		rightArea.add(buildTopBar(), BorderLayout.NORTH);

		JPanel contentArea = new JPanel(new BorderLayout(0, 0));
		contentArea.setBackground(UITheme.BG_MAIN);

		filterDrawer = buildFilterDrawer();
		filterDrawer.setVisible(false);
		contentArea.add(filterDrawer, BorderLayout.NORTH);

		JPanel centerContent = new JPanel(new BorderLayout(0, 0));
		centerContent.setBackground(UITheme.BG_MAIN);

		galeria = new GaleriaCobertesPanel();
		buildTable();

		cardLayout = new java.awt.CardLayout();
		contentCards = new JPanel(cardLayout);
		contentCards.setBackground(UITheme.BG_MAIN);
		contentCards.add(scrollPaneJTable, "TAULA");
		contentCards.add(galeria, "GALERIA");

		buildPagination();

		centerContent.add(contentCards, BorderLayout.CENTER);
		centerContent.add(paginationPanel, BorderLayout.SOUTH);

		contentArea.add(centerContent, BorderLayout.CENTER);
		rightArea.add(contentArea, BorderLayout.CENTER);

		add(rightArea, BorderLayout.CENTER);
	}

	// ── Sidebar ───────────────────────────────────────────────────────────────

	private JPanel buildSidebar() {
		JPanel sb = new JPanel(new BorderLayout(0, 0));
		sb.setBackground(UITheme.SIDEBAR_BG);
		sb.setPreferredSize(new Dimension(220, 0));
		sb.setMinimumSize(new Dimension(220, 0));

		// Top section
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(UITheme.SIDEBAR_BG);

		// Logo
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

		sb.add(top, BorderLayout.NORTH);

		// Shelves scroll area
		sidebarShelvesPanel = new JPanel();
		sidebarShelvesPanel.setLayout(new BoxLayout(sidebarShelvesPanel, BoxLayout.Y_AXIS));
		sidebarShelvesPanel.setBackground(UITheme.SIDEBAR_BG);

		JScrollPane shelvesScroll = new JScrollPane(sidebarShelvesPanel);
		shelvesScroll.setBorder(null);
		shelvesScroll.getVerticalScrollBar().setUnitIncrement(12);
		shelvesScroll.setBackground(UITheme.SIDEBAR_BG);
		shelvesScroll.getViewport().setBackground(UITheme.SIDEBAR_BG);
		shelvesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sb.add(shelvesScroll, BorderLayout.CENTER);

		// Bottom section
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

		btnThemeToggle = makeSidebarBtn(I18n.t("btn_mode_fosc"));
		btnThemeToggle.setToolTipText(I18n.t("tip_mode_fosc"));
		btnThemeToggle.addActionListener(e -> {
			UITheme.setDark(!UITheme.isDark);
			applyTheme();
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

		sb.add(bottom, BorderLayout.SOUTH);

		return sb;
	}

	private JPanel buildTopBar() {
		JPanel topBar = new JPanel(new BorderLayout(8, 0));
		topBar.setBackground(UITheme.BG_PANEL);
		topBar.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CLR),
			BorderFactory.createEmptyBorder(10, 16, 10, 16)
		));

		JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
		searchWrap.setBackground(UITheme.BG_PANEL);
		searchBar = new JTextField();
		searchBar.setToolTipText(I18n.t("tip_search_bar"));
		UITheme.styleField(searchBar);
		searchWrap.add(searchBar, BorderLayout.CENTER);
		topBar.add(searchWrap, BorderLayout.CENTER);

		JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		rightBtns.setBackground(UITheme.BG_PANEL);

		btnToggleFiltres = new JButton(I18n.t("btn_toggle_filtres_lbl"));
		UITheme.styleSecondaryButton(btnToggleFiltres);
		btnToggleFiltres.setToolTipText(I18n.t("tip_toggle_filtres"));
		rightBtns.add(btnToggleFiltres);

		btnToggleVista = new JButton(I18n.t("btn_toggle_vista_lbl"));
		UITheme.styleSecondaryButton(btnToggleVista);
		btnToggleVista.setToolTipText(I18n.t("tip_toggle_vista"));
		rightBtns.add(btnToggleVista);

		btnGroupSeries = new JButton(I18n.t("btn_group_series_lbl"));
		UITheme.styleSecondaryButton(btnGroupSeries);
		btnGroupSeries.setToolTipText(I18n.t("tip_group_series"));
		rightBtns.add(btnGroupSeries);

		btnNouLlibre = new JButton(I18n.t("btn_nou_llibre_short"));
		UITheme.styleAccentButton(btnNouLlibre);
		btnNouLlibre.setToolTipText(I18n.t("tip_nou_llibre_short"));
		rightBtns.add(btnNouLlibre);

		topBar.add(rightBtns, BorderLayout.EAST);

		return topBar;
	}

	private JPanel buildFilterDrawer() {
		JPanel drawer = new JPanel(new BorderLayout(0, 0));
		drawer.setBackground(UITheme.BG_MAIN);
		drawer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CLR));

		// Preset bar
		JPanel presetBar = new JPanel(null);
		presetBar.setBackground(UITheme.BG_MAIN);
		presetBar.setPreferredSize(new Dimension(0, 38));

		JLabel lblPresets = new JLabel(I18n.t("lbl_preset_colon"));
		UITheme.styleLabel(lblPresets);
		lblPresets.setBounds(8, 9, 50, 20);
		presetBar.add(lblPresets);

		comboPresets = new JComboBox<>();
		comboPresets.setFont(UITheme.FONT_BASE);
		comboPresets.setToolTipText(I18n.t("tip_combo_presets"));
		comboPresets.setBounds(62, 6, 150, 26);
		presetBar.add(comboPresets);

		btnCarregaPreset = new JButton(I18n.t("btn_load_preset"));
		UITheme.styleAccentButton(btnCarregaPreset);
		btnCarregaPreset.setToolTipText(I18n.t("tip_load_preset"));
		btnCarregaPreset.setBounds(216, 6, 75, 26);
		presetBar.add(btnCarregaPreset);

		btnDesaPreset = new JButton(I18n.t("btn_save_preset_lbl"));
		UITheme.styleSecondaryButton(btnDesaPreset);
		btnDesaPreset.setToolTipText(I18n.t("tip_save_preset"));
		btnDesaPreset.setBounds(295, 6, 65, 26);
		presetBar.add(btnDesaPreset);

		btnEsborraPreset = new JButton(I18n.t("btn_delete_preset_lbl"));
		UITheme.styleSecondaryButton(btnEsborraPreset);
		btnEsborraPreset.setToolTipText(I18n.t("tip_delete_preset"));
		btnEsborraPreset.setBounds(364, 6, 75, 26);
		presetBar.add(btnEsborraPreset);

		drawer.add(presetBar, BorderLayout.NORTH);

		// Filter fields panel (horizontal rows)
		panelFiltros = new JPanel();
		panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
		panelFiltros.setBackground(UITheme.BG_PANEL);
		panelFiltros.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

		// Row 1: checkboxes + text fields
		JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1.setBackground(UITheme.BG_PANEL);

		chckbxLlegit = new JCheckBox(I18n.t("filter_llegit_chk"));
		chckbxLlegit.setFont(UITheme.FONT_BASE);
		chckbxLlegit.setBackground(UITheme.BG_PANEL);
		chckbxLlegit.setForeground(UITheme.TEXT_DARK);
		chckbxLlegit.setToolTipText(I18n.t("tip_filter_llegit"));
		row1.add(chckbxLlegit);

		chckbxNoLlegit = new JCheckBox(I18n.t("filter_no_llegit_chk"));
		chckbxNoLlegit.setFont(UITheme.FONT_BASE);
		chckbxNoLlegit.setBackground(UITheme.BG_PANEL);
		chckbxNoLlegit.setForeground(UITheme.TEXT_DARK);
		chckbxNoLlegit.setToolTipText(I18n.t("tip_filter_no_llegit"));
		row1.add(chckbxNoLlegit);

		row1.add(makeSep());

		row1.add(makeFieldWrap(I18n.t("filter_isbn_lbl"), textISBN = new JTextField(10)));
		row1.add(makeFieldWrap(I18n.t("filter_nom_lbl"), textNom = new JTextField(14)));
		row1.add(makeFieldWrap(I18n.t("filter_autor_lbl"), textAutor = new JTextField(14)));

		row1.add(makeSep());

		JLabel lblTag = new JLabel(I18n.t("filter_etiqueta_lbl"));
		UITheme.styleLabel(lblTag);
		row1.add(lblTag);
		comboTagFilter = new JComboBox<>();
		comboTagFilter.setFont(UITheme.FONT_BASE);
		comboTagFilter.setToolTipText(I18n.t("tip_filter_tag"));
		comboTagFilter.setPreferredSize(new Dimension(140, 28));
		row1.add(comboTagFilter);

		panelFiltros.add(row1);

		// Row 1b: editorial, serie, format, idioma
		JPanel row1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1b.setBackground(UITheme.BG_PANEL);
		row1b.add(makeFieldWrap(I18n.t("filter_editorial_lbl"), filterEditorial = new JTextField(14)));
		row1b.add(makeFieldWrap(I18n.t("filter_serie_lbl"),     filterSerie     = new JTextField(12)));
		row1b.add(makeFieldWrap(I18n.t("filter_idioma_lbl"),    filterIdioma    = new JTextField(10)));
		JLabel lblFormat = new JLabel(I18n.t("filter_format_lbl") + ":");
		UITheme.styleLabel(lblFormat);
		row1b.add(lblFormat);
		filterFormat = new JComboBox<>(new String[]{"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"), I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")});
		filterFormat.setFont(UITheme.FONT_BASE);
		filterFormat.setPreferredSize(new Dimension(130, 28));
		row1b.add(filterFormat);
		panelFiltros.add(row1b);

		// Row 2: range fields + action buttons
		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row2.setBackground(UITheme.BG_PANEL);

		row2.add(makeRangeWrap(I18n.t("filter_any_lbl"), anyMin = new JTextField(5), anyMax = new JTextField(5)));
		row2.add(makeRangeWrap(I18n.t("filter_valoracio_lbl"), valoracioMin = new JTextField(5), valoracioMax = new JTextField(5)));
		row2.add(makeRangeWrap(I18n.t("filter_preu_lbl"), preuMin = new JTextField(5), preuMax = new JTextField(5)));

		row2.add(makeSep());

		bttnFiltrar = new JButton(I18n.t("btn_filtrar"));
		UITheme.styleAccentButton(bttnFiltrar);
		bttnFiltrar.setToolTipText(I18n.t("tip_filtrar"));
		row2.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton(I18n.t("btn_quitar_filtres"));
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		bttnQuitarFiltros.setToolTipText(I18n.t("tip_quitar_filtres"));
		row2.add(bttnQuitarFiltros);

		panelFiltros.add(row2);

		// Row 3: utility buttons
		JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row3.setBackground(UITheme.BG_PANEL);

		btnExportCSV = new JButton();
		btnExportJSON = new JButton();
		btnExportHTML = new JButton();
		btnExportPDF = new JButton();

		btnExportDropdown = new JButton(I18n.t("btn_export_lbl") + " ▾");
		UITheme.styleSecondaryButton(btnExportDropdown);
		JPopupMenu exportMenu = new JPopupMenu();
		JMenuItem miExportCSV = new JMenuItem(I18n.t("btn_export_csv"));
		miExportCSV.setToolTipText(I18n.t("tip_export_csv"));
		miExportCSV.addActionListener(e -> btnExportCSV.doClick());
		JMenuItem miExportJSON = new JMenuItem(I18n.t("btn_export_json_lbl"));
		miExportJSON.setToolTipText(I18n.t("tip_export_json"));
		miExportJSON.addActionListener(e -> btnExportJSON.doClick());
		JMenuItem miExportHTML = new JMenuItem(I18n.t("btn_export_html_lbl"));
		miExportHTML.setToolTipText(I18n.t("tip_export_html"));
		miExportHTML.addActionListener(e -> btnExportHTML.doClick());
		JMenuItem miExportPDF = new JMenuItem(I18n.t("btn_export_pdf_lbl"));
		miExportPDF.setToolTipText(I18n.t("tip_export_pdf"));
		miExportPDF.addActionListener(e -> btnExportPDF.doClick());
		exportMenu.add(miExportCSV);
		exportMenu.add(miExportJSON);
		exportMenu.add(miExportHTML);
		exportMenu.add(miExportPDF);
		btnExportDropdown.addActionListener(e -> exportMenu.show(btnExportDropdown, 0, btnExportDropdown.getHeight()));
		row3.add(btnExportDropdown);

		btnImportarCSV = new JButton();
		btnImportarCalibre = new JButton();
		btnImportarJSON = new JButton();

		btnImportDropdown = new JButton(I18n.t("btn_import_lbl") + " ▾");
		UITheme.styleSecondaryButton(btnImportDropdown);
		JPopupMenu importMenu = new JPopupMenu();
		JMenuItem miImportCSV = new JMenuItem(I18n.t("btn_import_csv"));
		miImportCSV.setToolTipText(I18n.t("tip_import_csv"));
		miImportCSV.addActionListener(e -> btnImportarCSV.doClick());
		JMenuItem miImportJSON = new JMenuItem(I18n.t("btn_import_json_lbl"));
		miImportJSON.setToolTipText(I18n.t("tip_import_json"));
		miImportJSON.addActionListener(e -> btnImportarJSON.doClick());
		JMenuItem miImportCalibre = new JMenuItem(I18n.t("btn_import_calibre"));
		miImportCalibre.setToolTipText(I18n.t("tip_import_calibre"));
		miImportCalibre.addActionListener(e -> btnImportarCalibre.doClick());
		importMenu.add(miImportCSV);
		importMenu.add(miImportJSON);
		importMenu.add(miImportCalibre);
		btnImportDropdown.addActionListener(e -> importMenu.show(btnImportDropdown, 0, btnImportDropdown.getHeight()));
		row3.add(btnImportDropdown);

		btnFetchCovers = new JButton(I18n.t("btn_fetch_covers_lbl"));
		UITheme.styleSecondaryButton(btnFetchCovers);
		btnFetchCovers.setToolTipText(I18n.t("tip_fetch_covers"));
		row3.add(btnFetchCovers);

		btnEscanejarISBN = new JButton(I18n.t("btn_scan_isbn_lbl"));
		UITheme.styleSecondaryButton(btnEscanejarISBN);
		btnEscanejarISBN.setToolTipText(I18n.t("tip_scan_isbn"));
		row3.add(btnEscanejarISBN);

		row3.add(makeSep());

		btnBackupBD = new JButton(I18n.t("btn_backup_bd"));
		UITheme.styleSecondaryButton(btnBackupBD);
		btnBackupBD.setToolTipText(I18n.t("tip_backup_bd"));
		row3.add(btnBackupBD);

		btnRestaurarBD = new JButton(I18n.t("btn_restore_bd"));
		UITheme.styleSecondaryButton(btnRestaurarBD);
		btnRestaurarBD.setToolTipText(I18n.t("tip_restore_bd"));
		row3.add(btnRestaurarBD);

		panelFiltros.add(row3);

		scrolpaneFiltro = new JScrollPane(panelFiltros);
		scrolpaneFiltro.setBorder(null);
		scrolpaneFiltro.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrolpaneFiltro.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrolpaneFiltro.getViewport().setBackground(UITheme.BG_PANEL);

		drawer.add(scrolpaneFiltro, BorderLayout.CENTER);

		return drawer;
	}

	private void buildTable() {
		scrollPaneJTable = new JScrollPane();
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);
		scrollPaneJTable.getVerticalScrollBar().setUnitIncrement(16);

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

		String[] headerTips = {"Portada del llibre", "Numero ISBN del llibre", "Titol del llibre", "Nom de l'autor",
			"Any de publicacio", "Valoracio de 0 a 10", "Preu (" + herramienta.Config.getCurrencySymbol() + ")",
			"Estat de lectura (clic per canviar)", "Progres de lectura", "Obrir detalls"};

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

		jTableBilio.setDragEnabled(true);
		jTableBilio.setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(javax.swing.JComponent c) { return COPY; }
			@Override
			protected Transferable createTransferable(javax.swing.JComponent c) {
				JTable t = (JTable) c;
				StringBuilder sb = new StringBuilder();
				for (int row : t.getSelectedRows()) {
					Object v = t.getValueAt(row, 1); // COLUMNA_ISBN = 1
					if (v != null) { if (sb.length() > 0) sb.append(","); sb.append(v); }
				}
				return new StringSelection(sb.toString());
			}
		});
	}

	private void buildPagination() {
		paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
		paginationPanel.setBackground(UITheme.BG_MAIN);
		btnPaginaAnterior = new JButton(I18n.t("btn_page_prev"));
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		lblPagina = new JLabel(I18n.t("page_info_java", 1, 1));
		UITheme.styleLabel(lblPagina);
		btnPaginaSeguent = new JButton(I18n.t("btn_page_next"));
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		paginationPanel.add(btnPaginaAnterior);
		paginationPanel.add(lblPagina);
		paginationPanel.add(btnPaginaSeguent);
		paginationPanel.setVisible(false);
	}

	// ── Sidebar helpers ───────────────────────────────────────────────────────

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

	// ── Filter panel helpers ─────────────────────────────────────────────────

	private JPanel makeFieldWrap(String label, JTextField field) {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(UITheme.BG_PANEL);
		JLabel lbl = new JLabel(label + ":");
		UITheme.styleLabel(lbl);
		lbl.setPreferredSize(new Dimension(lbl.getPreferredSize().width, 28));
		UITheme.styleField(field);
		field.setPreferredSize(new Dimension(field.getPreferredSize().width, 28));
		p.add(lbl, BorderLayout.WEST);
		p.add(field, BorderLayout.CENTER);
		return p;
	}

	private JPanel makeRangeWrap(String label, JTextField min, JTextField max) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		p.setBackground(UITheme.BG_PANEL);
		JLabel lbl = new JLabel(label + ":");
		UITheme.styleLabel(lbl);
		UITheme.styleField(min);
		UITheme.styleField(max);
		min.setPreferredSize(new Dimension(min.getPreferredSize().width, 28));
		max.setPreferredSize(new Dimension(max.getPreferredSize().width, 28));
		JLabel dash = new JLabel("\u2013");
		dash.setForeground(UITheme.TEXT_MID);
		p.add(lbl);
		p.add(min);
		p.add(dash);
		p.add(max);
		return p;
	}

	private javax.swing.JSeparator makeSep() {
		javax.swing.JSeparator sep = new javax.swing.JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(new Dimension(1, 28));
		sep.setForeground(UITheme.BORDER_CLR);
		return sep;
	}

	// ── Public methods for sidebar shelf management ───────────────────────────

	public void rebuildSidebarShelves(List<domini.Llista> llistes, Map<Integer, Integer> counts) {
		sidebarShelvesPanel.removeAll();
		// Remove old shelf buttons from sidebarBtns before clearing map
		for (JButton btn : sidebarShelfBtnMap.values()) sidebarBtns.remove(btn);
		sidebarShelfBtnMap.clear();

		for (domini.Llista l : llistes) {
			String label = l.getNom() + " (" + counts.getOrDefault(l.getId(), 0) + ")";
			JButton btn = makeSidebarBtn("  " + label);
			if (l.getColor() != null) {
				try {
					Color c = Color.decode(l.getColor());
					btn.setIcon(new javax.swing.Icon() {
						public int getIconWidth()  { return 10; }
						public int getIconHeight() { return 10; }
						public void paintIcon(Component cp, java.awt.Graphics g, int x, int y) {
							g.setColor(c);
							g.fillRoundRect(x, y + 2, 8, 8, 3, 3);
						}
					});
					btn.setHorizontalTextPosition(JButton.RIGHT);
				} catch (Exception ignored) {}
			}
			final int id = l.getId();
			btn.addActionListener(e -> {
				for (int i = 0; i < comboLlistes.getItemCount(); i++) {
					Object item = comboLlistes.getItemAt(i);
					if (item instanceof domini.Llista && ((domini.Llista) item).getId() == id) {
						comboLlistes.setSelectedIndex(i);
						break;
					}
				}
			});
			sidebarShelfBtnMap.put(l.getId(), btn);
			sidebarShelvesPanel.add(btn);
			final int shelfId = l.getId();
			new DropTarget(btn, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
				@Override public void drop(DropTargetDropEvent ev) {
					try {
						ev.acceptDrop(DnDConstants.ACTION_COPY);
						String data = (String) ev.getTransferable().getTransferData(DataFlavor.stringFlavor);
						if (data == null || data.isBlank()) return;
						java.util.List<Long> isbns = new java.util.ArrayList<>();
						for (String part : data.split(",")) {
							try { isbns.add(Long.parseLong(part.trim())); } catch (NumberFormatException ignored) {}
						}
						if (!isbns.isEmpty() && onDragToShelf != null) onDragToShelf.accept(shelfId, isbns);
						ev.dropComplete(true);
					} catch (Exception ex) { ev.dropComplete(false); }
				}
			});
		}
		sidebarShelvesPanel.revalidate();
		sidebarShelvesPanel.repaint();
	}

	public boolean isFilterDrawerVisible() { return filterDrawer.isVisible(); }

	public void setFilterDrawerVisible(boolean v) {
		filterDrawer.setVisible(v);
		revalidate();
		repaint();
	}

	public boolean isGaleriaMode() { return galeriaMode; }

	public void showGaleria() {
		galeriaMode = true;
		cardLayout.show(contentCards, "GALERIA");
	}

	public void showTaula() {
		galeriaMode = false;
		cardLayout.show(contentCards, "TAULA");
	}

	// ── applyTheme ────────────────────────────────────────────────────────────

	public void applyTheme() {
		UITheme.rebuildFonts(herramienta.Config.getFontSize());
		herramienta.Config.setDarkMode(UITheme.isDark);

		setBackground(UITheme.BG_MAIN);

		// Sidebar
		applyBgToNonButtons(sidebar, UITheme.SIDEBAR_BG);
		for (JButton btn : sidebarBtns) {
			UITheme.styleSidebarButton(btn);
			btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
		}
		btnSortir.setForeground(new Color(0xFF8080));
		btnThemeToggle.setText(UITheme.isDark ? "Mode clar" : "Mode fosc");

		// Table
		scrollPaneJTable.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		scrollPaneJTable.getViewport().setBackground(UITheme.BG_PANEL);
		jTableBilio.setBackground(UITheme.BG_PANEL);
		jTableBilio.setForeground(UITheme.TEXT_DARK);
		jTableBilio.setSelectionBackground(UITheme.ACCENT);
		jTableBilio.setSelectionForeground(java.awt.Color.WHITE);
		jTableBilio.setGridColor(UITheme.TABLE_GRID);
		UIManager.put("Table.alternateRowColor", UITheme.TABLE_ALT);

		// Search bar
		UITheme.styleField(searchBar);

		// Filter panel
		panelFiltros.setBackground(UITheme.BG_PANEL);
		scrolpaneFiltro.getViewport().setBackground(UITheme.BG_PANEL);
		for (Component r : panelFiltros.getComponents()) {
			if (r instanceof JPanel) {
				((JPanel) r).setBackground(UITheme.BG_PANEL);
				for (Component c : ((JPanel) r).getComponents()) {
					if (c instanceof JCheckBox) {
						((JCheckBox) c).setBackground(UITheme.BG_PANEL);
						((JCheckBox) c).setForeground(UITheme.TEXT_DARK);
						((JCheckBox) c).setFont(UITheme.FONT_BASE);
					} else if (c instanceof JLabel) {
						UITheme.styleLabel((JLabel) c);
					} else if (c instanceof JTextField) {
						UITheme.styleField((JTextField) c);
					} else if (c instanceof JPanel) {
						((JPanel) c).setBackground(UITheme.BG_PANEL);
						for (Component cc : ((JPanel) c).getComponents()) {
							if (cc instanceof JLabel) UITheme.styleLabel((JLabel) cc);
							if (cc instanceof JTextField) UITheme.styleField((JTextField) cc);
						}
					}
				}
			}
		}
		UITheme.styleAccentButton(bttnFiltrar);
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		UITheme.styleSecondaryButton(btnExportDropdown);
		UITheme.styleSecondaryButton(btnImportDropdown);
		UITheme.styleSecondaryButton(btnFetchCovers);
		UITheme.styleSecondaryButton(btnEscanejarISBN);
		UITheme.styleSecondaryButton(btnBackupBD);
		UITheme.styleSecondaryButton(btnRestaurarBD);
		UITheme.styleAccentButton(btnCarregaPreset);
		UITheme.styleSecondaryButton(btnDesaPreset);
		UITheme.styleSecondaryButton(btnEsborraPreset);
		comboPresets.setFont(UITheme.FONT_BASE);
		filterDrawer.setBackground(UITheme.BG_MAIN);

		// Top bar buttons
		UITheme.styleAccentButton(btnNouLlibre);
		UITheme.styleSecondaryButton(btnToggleFiltres);
		UITheme.styleSecondaryButton(btnToggleVista);
		UITheme.styleSecondaryButton(btnGroupSeries);

		// Pagination
		paginationPanel.setBackground(UITheme.BG_MAIN);
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		UITheme.styleSecondaryButton(btnPaginaSeguent);
		UITheme.styleLabel(lblPagina);

		// Gallery
		galeria.applyTheme();

		// Window background
		Window w = SwingUtilities.getWindowAncestor(this);
		if (w instanceof JFrame) {
			((JFrame) w).getContentPane().setBackground(UITheme.BG_MAIN);
		}

		// Reinstall Nimbus so it re-derives colors from updated UIManager keys.
		try {
			UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
			UITheme.applyUIManager();
		} catch (Exception ignored) {}
		if (w != null) SwingUtilities.updateComponentTreeUI(w);

		// Re-apply custom styles after L&F reinstall
		applyBgToNonButtons(sidebar, UITheme.SIDEBAR_BG);
		for (JButton btn : sidebarBtns) {
			UITheme.styleSidebarButton(btn);
			btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height + 4));
		}
		btnSortir.setForeground(new Color(0xFF8080));
		UITheme.styleAccentButton(bttnFiltrar);
		UITheme.styleSecondaryButton(bttnQuitarFiltros);
		UITheme.styleAccentButton(btnNouLlibre);
		UITheme.styleSecondaryButton(btnToggleFiltres);
		UITheme.styleSecondaryButton(btnToggleVista);
		UITheme.styleSecondaryButton(btnGroupSeries);
		UITheme.styleSecondaryButton(btnExportDropdown);
		UITheme.styleSecondaryButton(btnImportDropdown);
		UITheme.styleSecondaryButton(btnFetchCovers);
		UITheme.styleSecondaryButton(btnEscanejarISBN);
		UITheme.styleSecondaryButton(btnBackupBD);
		UITheme.styleSecondaryButton(btnRestaurarBD);
		UITheme.styleAccentButton(btnCarregaPreset);
		UITheme.styleSecondaryButton(btnDesaPreset);
		UITheme.styleSecondaryButton(btnEsborraPreset);
		UITheme.styleSecondaryButton(btnPaginaAnterior);
		UITheme.styleSecondaryButton(btnPaginaSeguent);

		repaint();
	}

	private void applyBgToNonButtons(Component comp, Color bg) {
		if (comp instanceof JButton) return;
		comp.setBackground(bg);
		if (comp instanceof JScrollPane) {
			((JScrollPane) comp).getViewport().setBackground(bg);
		}
		if (comp instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) comp).getComponents()) {
				if (!(child instanceof JTable)) applyBgToNonButtons(child, bg);
			}
		}
	}

	// ── Getters ───────────────────────────────────────────────────────────────

	public JButton getBtnPaginaAnterior()     { return btnPaginaAnterior; }
	public JButton getBtnPaginaSeguent()      { return btnPaginaSeguent; }
	public JLabel  getLblPagina()             { return lblPagina; }
	public JPanel  getPaginationPanel()       { return paginationPanel; }
	public JButton getBtnNouLlibre()          { return btnNouLlibre; }
	public JButton getBtnExportCSV()          { return btnExportCSV; }
	public JButton getBtnImportarCSV()        { return btnImportarCSV; }
	public JButton getBtnImportarCalibre()    { return btnImportarCalibre; }
	public JButton getBtnExportJSON()         { return btnExportJSON; }
	public JButton getBtnImportarJSON()       { return btnImportarJSON; }
	public JButton getBtnExportHTML()         { return btnExportHTML; }
	public JButton getBtnExportPDF()          { return btnExportPDF; }
	public JButton getBtnFetchCovers()        { return btnFetchCovers; }
	public JButton getBtnEscanejarISBN()      { return btnEscanejarISBN; }
	public JButton getBtnEstadistiques()      { return btnEstadistiques; }
	public JButton getBtnLlibreAleatori()     { return btnLlibreAleatori; }
	public JButton getBtnBackupBD()           { return btnBackupBD; }
	public JButton getBtnRestaurarBD()        { return btnRestaurarBD; }
	public JButton getBtnConfiguracio()       { return btnConfiguracio; }
	public JButton getBtnSobre()              { return btnSobre; }
	public JTextField getSearchBar()          { return searchBar; }
	public JTextField getTextISBN()           { return textISBN; }
	public JTextField getTextNom()            { return textNom; }
	public JTextField getTextAutor()          { return textAutor; }
	public JTextField getAnyMin()             { return anyMin; }
	public JTextField getAnyMax()             { return anyMax; }
	public JTextField getValoracioMin()       { return valoracioMin; }
	public JTextField getValoracioMax()       { return valoracioMax; }
	public JTextField getPreuMin()            { return preuMin; }
	public JTextField getPreuMax()            { return preuMax; }
	public JTable  getjTableBilio()           { return jTableBilio; }
	public JPanel  getPanelFiltros()          { return panelFiltros; }
	public JScrollPane getScrollPaneJTable()  { return scrollPaneJTable; }
	public JScrollPane getScrolpaneFiltro()   { return scrolpaneFiltro; }
	public JCheckBox getchckbxLlegit()        { return chckbxLlegit; }
	public JCheckBox getchckbxNoLlegit()      { return chckbxNoLlegit; }
	public JButton getbtnFiltrar()            { return bttnFiltrar; }
	public JButton getbttnQuitarFiltros()     { return bttnQuitarFiltros; }
	public JComboBox<Object> getComboLlistes(){ return comboLlistes; }
	public JButton getBtnGestioLlistes()      { return btnGestioLlistes; }
	public JComboBox<Object> getComboTagFilter() { return comboTagFilter; }
	public JTextField getFilterEditorial()       { return filterEditorial; }
	public JTextField getFilterSerie()           { return filterSerie; }
	public JTextField getFilterIdioma()          { return filterIdioma; }
	public JComboBox<String> getFilterFormat()   { return filterFormat; }
	public JButton getBtnAfegitsRecentment()  { return btnAfegitsRecentment; }
	public JButton getBtnLlegitsRecentment()  { return btnLlegitsRecentment; }
	public JButton getBtnDesitjats()          { return btnDesitjats; }
	public JButton getBtnEnCurs()             { return btnEnCurs; }
	public void setOnDragToShelf(java.util.function.BiConsumer<Integer, java.util.List<Long>> cb) { this.onDragToShelf = cb; }
	public JComboBox<String> getComboPresets(){ return comboPresets; }
	public JButton getBtnCarregaPreset()      { return btnCarregaPreset; }
	public JButton getBtnDesaPreset()         { return btnDesaPreset; }
	public JButton getBtnEsborraPreset()      { return btnEsborraPreset; }
	public JButton getBtnToggleFiltres()      { return btnToggleFiltres; }
	public JButton getBtnToggleVista()        { return btnToggleVista; }
	public JButton getBtnGroupSeries()        { return btnGroupSeries; }
	public GaleriaCobertesPanel getGaleria()  { return galeria; }
}
