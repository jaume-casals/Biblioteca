package presentacio.panells;


import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import presentacio.formularis.RegistreCampsFormulari.Camp;
import presentacio.formularis.RegistreCampsFormulari;
import presentacio.util.UIComponents;



/**
 * Calaix de la barra lateral que conté les entrades de filtre (camps de
 * text, caselles, combos), la barra de presets, els botons aplicar/netejar
 * i els botons d'exportar/importar/còpia de seguretat.
 *
 * <p>A partir de R1, els 32 camps de component declarats a mà se substitueixen
 * per un sol {@link RegistreCampsFormulari} d'especificacions declaratives.
 * Els 32 getters públics es conserven com a shims d'una línia que deleguen
 * al registre, de manera que els 217 call sites externs (molt
 * prominentment {@link PanelMostrarBiblioteca}) continuen compilant sense
 * canvis.
 */
public class PanelCalaixFiltre extends JPanel {

	private JScrollPane panellDesplacamentFiltre;
	private JPanel panellFiltres;
	private final RegistreCampsFormulari registry = new RegistreCampsFormulari();

	public PanelCalaixFiltre() {
		buildFilterDrawer();
	}

	private void declareSpecs() {
		registry.add(Camp.combo("comboPresets",       null, "tip_combo_presets", 0));
		registry.add(Camp.button("btnCarregaPreset",   "btn_load_preset",       "tip_load_preset"));
		registry.add(Camp.button("btnDesaPreset",      "btn_save_preset_lbl",   "tip_save_preset"));
		registry.add(Camp.button("btnEsborraPreset",   "btn_delete_preset_lbl", "tip_delete_preset"));

		registry.add(Camp.check("chckbxLlegit",        "filter_llegit_chk",     "tip_filter_llegit"));
		registry.add(Camp.check("chckbxNoLlegit",      "filter_no_llegit_chk",  "tip_filter_no_llegit"));

		registry.add(Camp.text("textISBN",             "filter_isbn_lbl",       10));
		registry.add(Camp.text("textNom",              "filter_nom_lbl",        "tip_filter_substring", 14));
		registry.add(Camp.text("textAutor",            "filter_autor_lbl",      "tip_filter_substring", 14));

		registry.add(Camp.combo("comboTagFilter",      "filter_etiqueta_lbl",   "tip_filter_tag", 140));

		registry.add(Camp.text("filterEditorial",      "filter_editorial_lbl",  "tip_filter_substring", 14));
		registry.add(Camp.text("filterSerie",          "filter_serie_lbl",      "tip_filter_substring", 12));
		registry.add(Camp.text("filterIdioma",         "filter_idioma_lbl",     "tip_filter_substring", 10));
		registry.add(Camp.combo("filterFormat",        "filter_format_lbl",     null,           130));

		registry.add(Camp.text("anyMin",               "filter_any_lbl",        5));
		registry.add(Camp.text("anyMax",               "filter_any_lbl",        5));
		registry.add(Camp.text("valoracioMin",         "filter_valoracio_lbl",  5));
		registry.add(Camp.text("valoracioMax",         "filter_valoracio_lbl",  5));
		registry.add(Camp.text("preuMin",              "filter_preu_lbl",       5));
		registry.add(Camp.text("preuMax",              "filter_preu_lbl",       5));

		registry.add(Camp.button("bttnFiltrar",         "btn_filtrar",           "tip_filtrar"));
		registry.add(Camp.button("bttnQuitarFiltros",   "btn_quitar_filtres",    "tip_quitar_filtres"));

		registry.add(Camp.button("btnExportDropdown",   "btn_export_lbl",        null));
		registry.add(Camp.button("btnImportDropdown",   "btn_import_lbl",        null));
		registry.add(Camp.button("btnFetchCovers",      "btn_fetch_covers_lbl",  "tip_fetch_covers"));
		registry.add(Camp.button("btnEscanejarISBN",    "btn_scan_isbn_lbl",     "tip_scan_isbn"));
		registry.add(Camp.button("btnBackupBD",         "btn_backup_bd",         "tip_backup_bd"));
		registry.add(Camp.button("btnRestaurarBD",      "btn_restore_bd",        "tip_restore_bd"));

		registry.add(new Camp("btnExportCSV",         null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnExportJSON",        null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnExportHTML",        null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnExportPDF",         null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnImportarCSV",       null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnImportarJSON",      null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));
		registry.add(new Camp("btnImportarCalibre",   null, RegistreCampsFormulari.Tipus.BUTTON, null, 0, ""));

		registry.build();
	}

	private void buildFilterDrawer() {
		declareSpecs();

		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().bgMain());
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()));

		JPanel presetBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		presetBar.setBackground(UITheme.palette().bgMain());

		JLabel lblPresets = new JLabel(I18n.t("lbl_preset_colon"));
		UIComponents.styleLabel(lblPresets);
		presetBar.add(lblPresets);

		registry.comboBox("comboPresets").setFont(UITheme.fontBase());
		registry.comboBox("comboPresets").setPreferredSize(new Dimension(150, 26));
		presetBar.add(registry.comboBox("comboPresets"));

		UIComponents.styleAccentButton(registry.button("btnCarregaPreset"));
		registry.button("btnCarregaPreset").setPreferredSize(new Dimension(75, 26));
		presetBar.add(registry.button("btnCarregaPreset"));

		UIComponents.styleSecondaryButton(registry.button("btnDesaPreset"));
		registry.button("btnDesaPreset").setPreferredSize(new Dimension(65, 26));
		presetBar.add(registry.button("btnDesaPreset"));

		UIComponents.styleSecondaryButton(registry.button("btnEsborraPreset"));
		registry.button("btnEsborraPreset").setPreferredSize(new Dimension(75, 26));
		presetBar.add(registry.button("btnEsborraPreset"));

		add(presetBar, BorderLayout.NORTH);

		panellFiltres = new JPanel();
		panellFiltres.setLayout(new BoxLayout(panellFiltres, BoxLayout.Y_AXIS));
		panellFiltres.setBackground(UITheme.palette().bgPanel());
		panellFiltres.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

		JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1.setBackground(UITheme.palette().bgPanel());

		styleCheckBox(registry.comprovarBox("chckbxLlegit"));
		row1.add(registry.comprovarBox("chckbxLlegit"));
		styleCheckBox(registry.comprovarBox("chckbxNoLlegit"));
		row1.add(registry.comprovarBox("chckbxNoLlegit"));

		row1.add(makeSep());

		row1.add(makeFieldWrap(I18n.t("filter_isbn_lbl"),  registry.textField("textISBN")));
		row1.add(makeFieldWrap(I18n.t("filter_nom_lbl"),   registry.textField("textNom")));
		row1.add(makeFieldWrap(I18n.t("filter_autor_lbl"), registry.textField("textAutor")));

		row1.add(makeSep());

		JLabel lblTag = new JLabel(I18n.t("filter_etiqueta_lbl"));
		UIComponents.styleLabel(lblTag);
		row1.add(lblTag);
		registry.comboBox("comboTagFilter").setFont(UITheme.fontBase());
		registry.comboBox("comboTagFilter").setToolTipText(I18n.t("tip_filter_tag"));
		registry.comboBox("comboTagFilter").setPreferredSize(new Dimension(140, 28));
		row1.add(registry.comboBox("comboTagFilter"));

		panellFiltres.add(row1);

		JPanel row1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1b.setBackground(UITheme.palette().bgPanel());
		for (FilaFiltre r : List.of(
				new FilaFiltre(I18n.t("filter_editorial_lbl"), registry.textField("filterEditorial")),
				new FilaFiltre(I18n.t("filter_serie_lbl"),     registry.textField("filterSerie")),
				new FilaFiltre(I18n.t("filter_idioma_lbl"),    registry.textField("filterIdioma")))) {
			row1b.add(makeFieldWrap(r.labelKey(), r.field()));
		}
		JLabel lblFormat = new JLabel(I18n.t("filter_format_lbl") + ":");
		UIComponents.styleLabel(lblFormat);
		row1b.add(lblFormat);
		registry.comboBox("filterFormat").setModel(
			new javax.swing.DefaultComboBoxModel<>(new String[]{
				"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"),
				I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")}));
		registry.comboBox("filterFormat").setFont(UITheme.fontBase());
		registry.comboBox("filterFormat").setPreferredSize(new Dimension(130, 28));
		row1b.add(registry.comboBox("filterFormat"));
		panellFiltres.add(row1b);

		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row2.setBackground(UITheme.palette().bgPanel());
		for (RangFiltre r : List.of(
				new RangFiltre(I18n.t("filter_any_lbl"),       registry.textField("anyMin"),       registry.textField("anyMax")),
				new RangFiltre(I18n.t("filter_valoracio_lbl"), registry.textField("valoracioMin"), registry.textField("valoracioMax")),
				new RangFiltre(I18n.t("filter_preu_lbl"),      registry.textField("preuMin"),      registry.textField("preuMax")))) {
			row2.add(makeRangeWrap(r.labelKey(), r.min(), r.max()));
		}

		row2.add(makeSep());

		UIComponents.styleAccentButton(registry.button("bttnFiltrar"));
		row2.add(registry.button("bttnFiltrar"));

		UIComponents.styleSecondaryButton(registry.button("bttnQuitarFiltros"));
		row2.add(registry.button("bttnQuitarFiltros"));

		panellFiltres.add(row2);

		JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row3.setBackground(UITheme.palette().bgPanel());

		UIComponents.styleSecondaryButton(registry.button("btnExportDropdown"));
		registry.button("btnExportDropdown").setText(I18n.t("btn_export_lbl") + " \u25BE");
		JPopupMenu exportarMenu = new JPopupMenu();
		afegirExportMenuItem(exportarMenu, "btn_export_csv",     "tip_export_csv",     "btnExportCSV");
		afegirExportMenuItem(exportarMenu, "btn_export_json_lbl","tip_export_json",    "btnExportJSON");
		afegirExportMenuItem(exportarMenu, "btn_export_html_lbl","tip_export_html",    "btnExportHTML");
		afegirExportMenuItem(exportarMenu, "btn_export_pdf_lbl", "tip_export_pdf",     "btnExportPDF");
		registry.button("btnExportDropdown").addActionListener(e ->
			exportarMenu.show(registry.button("btnExportDropdown"), 0, registry.button("btnExportDropdown").getHeight()));
		row3.add(registry.button("btnExportDropdown"));

		UIComponents.styleSecondaryButton(registry.button("btnImportDropdown"));
		registry.button("btnImportDropdown").setText(I18n.t("btn_import_lbl") + " \u25BE");
		JPopupMenu importarMenu = new JPopupMenu();
		afegirExportMenuItem(importarMenu, "btn_import_csv",     "tip_import_csv",     "btnImportarCSV");
		afegirExportMenuItem(importarMenu, "btn_import_json_lbl","tip_import_json",    "btnImportarJSON");
		afegirExportMenuItem(importarMenu, "btn_import_calibre", "tip_import_calibre", "btnImportarCalibre");
		registry.button("btnImportDropdown").addActionListener(e ->
			importarMenu.show(registry.button("btnImportDropdown"), 0, registry.button("btnImportDropdown").getHeight()));
		row3.add(registry.button("btnImportDropdown"));

		UIComponents.styleSecondaryButton(registry.button("btnFetchCovers"));
		row3.add(registry.button("btnFetchCovers"));

		UIComponents.styleSecondaryButton(registry.button("btnEscanejarISBN"));
		row3.add(registry.button("btnEscanejarISBN"));

		row3.add(makeSep());

		UIComponents.styleSecondaryButton(registry.button("btnBackupBD"));
		row3.add(registry.button("btnBackupBD"));

		UIComponents.styleSecondaryButton(registry.button("btnRestaurarBD"));
		row3.add(registry.button("btnRestaurarBD"));

		panellFiltres.add(row3);

		panellDesplacamentFiltre = new JScrollPane(panellFiltres);
		panellDesplacamentFiltre.setBorder(null);
		panellDesplacamentFiltre.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		panellDesplacamentFiltre.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		panellDesplacamentFiltre.getViewport().setBackground(UITheme.palette().bgPanel());

		add(panellDesplacamentFiltre, BorderLayout.CENTER);
	}

	private void afegirExportMenuItem(JPopupMenu menu, String labelKey, String tipKey, String targetKey) {
		JMenuItem mi = new JMenuItem(I18n.t(labelKey));
		mi.setToolTipText(I18n.t(tipKey));
		JButton target = registry.button(targetKey);
		mi.addActionListener(e -> target.doClick());
		menu.add(mi);
	}

	private void styleCheckBox(JCheckBox cb) {
		cb.setFont(UITheme.fontBase());
		cb.setBackground(UITheme.palette().bgPanel());
		cb.setForeground(UITheme.palette().textDark());
	}

	public void aplicarTheme() {
		styleAllButtons();
		registry.comboBox("comboPresets").setFont(UITheme.fontBase());
		setBackground(UITheme.palette().bgMain());
	}

	public void aplicarThemePostLaf() {
		styleAllButtons();
	}

	/**
	 * Única font de veritat per als estils dels 11 botons compartits per
	 * {@link #applyTheme()} i {@link #applyThemePostLaf()}. La troballa
	 * MEDIUM de tot.txt va assenyalar el bloc duplicat; l'helper manté
	 * els dos punts d'entrada sincronitzats quan s'afegeix un nou botó
	 * al registre.
	 */
	private void styleAllButtons() {
		styleFilterComponents();
		UIComponents.styleAccentButton(registry.button("bttnFiltrar"));
		UIComponents.styleSecondaryButton(registry.button("bttnQuitarFiltros"));
		UIComponents.styleSecondaryButton(registry.button("btnExportDropdown"));
		UIComponents.styleSecondaryButton(registry.button("btnImportDropdown"));
		UIComponents.styleSecondaryButton(registry.button("btnFetchCovers"));
		UIComponents.styleSecondaryButton(registry.button("btnEscanejarISBN"));
		UIComponents.styleSecondaryButton(registry.button("btnBackupBD"));
		UIComponents.styleSecondaryButton(registry.button("btnRestaurarBD"));
		UIComponents.styleAccentButton(registry.button("btnCarregaPreset"));
		UIComponents.styleSecondaryButton(registry.button("btnDesaPreset"));
		UIComponents.styleSecondaryButton(registry.button("btnEsborraPreset"));
	}

	private JPanel makeFieldWrap(String label, JTextField field) {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(UITheme.palette().bgPanel());
		JLabel lbl = new JLabel(label + ":");
		UIComponents.styleLabel(lbl);
		lbl.setPreferredSize(new Dimension(lbl.getPreferredSize().width, 28));
		UIComponents.styleField(field);
		field.setPreferredSize(new Dimension(field.getPreferredSize().width, 28));
		p.add(lbl, BorderLayout.WEST);
		p.add(field, BorderLayout.CENTER);
		return p;
	}

	private JPanel makeRangeWrap(String label, JTextField min, JTextField max) {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
		p.setBackground(UITheme.palette().bgPanel());
		JLabel lbl = new JLabel(label + ":");
		UIComponents.styleLabel(lbl);
		UIComponents.styleField(min);
		UIComponents.styleField(max);
		min.setPreferredSize(new Dimension(min.getPreferredSize().width, 28));
		max.setPreferredSize(new Dimension(max.getPreferredSize().width, 28));
		JLabel dash = new JLabel(I18n.t("filter_dash"));
		dash.setForeground(UITheme.palette().textMid());
		p.add(lbl);
		p.add(min);
		p.add(dash);
		p.add(max);
		return p;
	}

	private javax.swing.JSeparator makeSep() {
		javax.swing.JSeparator sep = new javax.swing.JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(new Dimension(1, 28));
		sep.setForeground(UITheme.palette().borderClr());
		return sep;
	}

	private void styleFilterComponents() {
		panellFiltres.setBackground(UITheme.palette().bgPanel());
		panellDesplacamentFiltre.getViewport().setBackground(UITheme.palette().bgPanel());
		for (Component r : panellFiltres.getComponents()) {
			if (r instanceof JPanel) {
				((JPanel) r).setBackground(UITheme.palette().bgPanel());
				for (Component c : ((JPanel) r).getComponents()) {
					if (c instanceof JCheckBox) {
						((JCheckBox) c).setBackground(UITheme.palette().bgPanel());
						((JCheckBox) c).setForeground(UITheme.palette().textDark());
						((JCheckBox) c).setFont(UITheme.fontBase());
					} else if (c instanceof JLabel) {
						UIComponents.styleLabel((JLabel) c);
					} else if (c instanceof JTextField) {
						UIComponents.styleField((JTextField) c);
					} else if (c instanceof JPanel) {
						((JPanel) c).setBackground(UITheme.palette().bgPanel());
						for (Component cc : ((JPanel) c).getComponents()) {
							if (cc instanceof JLabel) UIComponents.styleLabel((JLabel) cc);
							if (cc instanceof JTextField) UIComponents.styleField((JTextField) cc);
						}
					}
				}
			}
		}
	}

	public JPanel obtenirPanelFiltros()                { return panellFiltres; }
	public JScrollPane obtenirScrolpaneFiltro()        { return panellDesplacamentFiltre; }
	public RegistreCampsFormulari obtenirRegistry()         { return registry; }

	public JTextField obtenirTextISBN()                 { return registry.textField("textISBN"); }
	public JTextField obtenirTextNom()                  { return registry.textField("textNom"); }
	public JTextField obtenirTextAutor()                { return registry.textField("textAutor"); }
	public JTextField obtenirAnyMin()                   { return registry.textField("anyMin"); }
	public JTextField obtenirAnyMax()                   { return registry.textField("anyMax"); }
	public JTextField obtenirValoracioMin()             { return registry.textField("valoracioMin"); }
	public JTextField obtenirValoracioMax()             { return registry.textField("valoracioMax"); }
	public JTextField obtenirPreuMin()                  { return registry.textField("preuMin"); }
	public JTextField obtenirPreuMax()                  { return registry.textField("preuMax"); }
	public JCheckBox obtenirCasellaLlegit()              { return registry.comprovarBox("chckbxLlegit"); }
	public JCheckBox obtenirCasellaNoLlegit()            { return registry.comprovarBox("chckbxNoLlegit"); }
	public JButton obtenirBtnFiltrar()                  { return registry.button("bttnFiltrar"); }
	public JButton obtenirBtnQuitarFiltros()           { return registry.button("bttnQuitarFiltros"); }
	public JComboBox<Object> obtenirComboTagFilter()    { return registry.comboBox("comboTagFilter"); }
	public JTextField obtenirFilterEditorial()          { return registry.textField("filterEditorial"); }
	public JTextField obtenirFilterSerie()              { return registry.textField("filterSerie"); }
	public JTextField obtenirFilterIdioma()             { return registry.textField("filterIdioma"); }
	public JComboBox<String> obtenirFilterFormat()      { return registry.comboBox("filterFormat"); }
	public JComboBox<String> obtenirComboPresets()      { return registry.comboBox("comboPresets"); }
	public JButton obtenirBtnCarregaPreset()            { return registry.button("btnCarregaPreset"); }
	public JButton obtenirBtnDesaPreset()               { return registry.button("btnDesaPreset"); }
	public JButton obtenirBtnEsborraPreset()            { return registry.button("btnEsborraPreset"); }
	public JButton obtenirBtnExportCSV()                { return registry.button("btnExportCSV"); }
	public JButton obtenirBtnImportarCSV()              { return registry.button("btnImportarCSV"); }
	public JButton obtenirBtnImportarCalibre()          { return registry.button("btnImportarCalibre"); }
	public JButton obtenirBtnExportJSON()               { return registry.button("btnExportJSON"); }
	public JButton obtenirBtnImportarJSON()             { return registry.button("btnImportarJSON"); }
	public JButton obtenirBtnExportHTML()               { return registry.button("btnExportHTML"); }
	public JButton obtenirBtnExportPDF()                { return registry.button("btnExportPDF"); }
	public JButton obtenirBtnFetchCovers()              { return registry.button("btnFetchCovers"); }
	public JButton obtenirBtnEscanejarISBN()            { return registry.button("btnEscanejarISBN"); }
	public JButton obtenirBtnBackupBD()                 { return registry.button("btnBackupBD"); }
	public JButton obtenirBtnRestaurarBD()              { return registry.button("btnRestaurarBD"); }

	/**
	 * Triple declaratiu per a una fila de filtre de camp de text amb
	 * etiqueta. Usat per {@link #buildFilterDrawer()} per declarar els
	 * tres camps de text de la "fila1b" (editorial / serie / idioma)
	 * sense repetir tres cops el boilerplate label-key + new-JTextField.
	 */
	record FilaFiltre(String labelKey, JTextField field) {}

	/**
	 * Triple declaratiu per a una fila de filtre de rang numèric.
	 * Usat per {@link #buildFilterDrawer()} per declarar els tres camps
	 * de rang de la "fila2" (any / valoracio / preu) sense repetir tres
	 * cops el boilerplate label-key + dos new-JTextFields.
	 */
	record RangFiltre(String labelKey, JTextField min, JTextField max) {}
}

