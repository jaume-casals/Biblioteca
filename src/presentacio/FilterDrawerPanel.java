package presentacio;


import presentacio.FormFieldRegistry.Field;
import presentacio.UIComponents;
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
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import herramienta.I18n;
import herramienta.UITheme;

/**
 * Sidebar drawer that holds the filter inputs (text fields, checkboxes,
 * combo boxes), the preset bar, the apply/clear buttons, and the
 * export/import/backup buttons.
 *
 * <p>As of R1, the 32 hand-declared component fields are replaced by a
 * single {@link FormFieldRegistry} of declarative specs. The 32 public
 * getters are kept as 1-line shims that delegate to the registry, so
 * the 217 external call sites (most prominently
 * {@link MostrarBibliotecaPanel}) keep compiling without changes.
 */
public class FilterDrawerPanel extends JPanel {

	private JScrollPane scrolpaneFiltro;
	private JPanel panelFiltros;
	private final FormFieldRegistry registry = new FormFieldRegistry();

	public FilterDrawerPanel() {
		buildFilterDrawer();
	}

	private void declareSpecs() {
		registry.add(Field.combo("comboPresets",       null, "tip_combo_presets", 0));
		registry.add(Field.button("btnCarregaPreset",   "btn_load_preset",       "tip_load_preset"));
		registry.add(Field.button("btnDesaPreset",      "btn_save_preset_lbl",   "tip_save_preset"));
		registry.add(Field.button("btnEsborraPreset",   "btn_delete_preset_lbl", "tip_delete_preset"));

		registry.add(Field.check("chckbxLlegit",        "filter_llegit_chk",     "tip_filter_llegit"));
		registry.add(Field.check("chckbxNoLlegit",      "filter_no_llegit_chk",  "tip_filter_no_llegit"));

		registry.add(Field.text("textISBN",             "filter_isbn_lbl",       10));
		registry.add(Field.text("textNom",              "filter_nom_lbl",        14));
		registry.add(Field.text("textAutor",            "filter_autor_lbl",      14));

		registry.add(Field.combo("comboTagFilter",      "filter_etiqueta_lbl",   "tip_filter_tag", 140));

		registry.add(Field.text("filterEditorial",      "filter_editorial_lbl",  14));
		registry.add(Field.text("filterSerie",          "filter_serie_lbl",      12));
		registry.add(Field.text("filterIdioma",         "filter_idioma_lbl",     10));
		registry.add(Field.combo("filterFormat",        "filter_format_lbl",     null,           130));

		registry.add(Field.text("anyMin",               "filter_any_lbl",        5));
		registry.add(Field.text("anyMax",               "filter_any_lbl",        5));
		registry.add(Field.text("valoracioMin",         "filter_valoracio_lbl",  5));
		registry.add(Field.text("valoracioMax",         "filter_valoracio_lbl",  5));
		registry.add(Field.text("preuMin",              "filter_preu_lbl",       5));
		registry.add(Field.text("preuMax",              "filter_preu_lbl",       5));

		registry.add(Field.button("bttnFiltrar",         "btn_filtrar",           "tip_filtrar"));
		registry.add(Field.button("bttnQuitarFiltros",   "btn_quitar_filtres",    "tip_quitar_filtres"));

		registry.add(Field.button("btnExportDropdown",   "btn_export_lbl",        null));
		registry.add(Field.button("btnImportDropdown",   "btn_import_lbl",        null));
		registry.add(Field.button("btnFetchCovers",      "btn_fetch_covers_lbl",  "tip_fetch_covers"));
		registry.add(Field.button("btnEscanejarISBN",    "btn_scan_isbn_lbl",     "tip_scan_isbn"));
		registry.add(Field.button("btnBackupBD",         "btn_backup_bd",         "tip_backup_bd"));
		registry.add(Field.button("btnRestaurarBD",      "btn_restore_bd",        "tip_restore_bd"));

		registry.add(new Field("btnExportCSV",         null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnExportJSON",        null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnExportHTML",        null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnExportPDF",         null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnImportarCSV",       null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnImportarJSON",      null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));
		registry.add(new Field("btnImportarCalibre",   null, FormFieldRegistry.Kind.BUTTON, null, 0, ""));

		registry.build();
	}

	private void buildFilterDrawer() {
		declareSpecs();

		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.palette().bgMain());
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.palette().borderClr()));

		JPanel presetBar = new JPanel(null);
		presetBar.setBackground(UITheme.palette().bgMain());
		presetBar.setPreferredSize(new Dimension(0, 38));

		JLabel lblPresets = new JLabel(I18n.t("lbl_preset_colon"));
		UIComponents.styleLabel(lblPresets);
		lblPresets.setBounds(8, 9, 50, 20);
		presetBar.add(lblPresets);

		registry.comboBox("comboPresets").setFont(UITheme.fontBase());
		registry.comboBox("comboPresets").setBounds(62, 6, 150, 26);
		presetBar.add(registry.comboBox("comboPresets"));

		UIComponents.styleAccentButton(registry.button("btnCarregaPreset"));
		registry.button("btnCarregaPreset").setBounds(216, 6, 75, 26);
		presetBar.add(registry.button("btnCarregaPreset"));

		UIComponents.styleSecondaryButton(registry.button("btnDesaPreset"));
		registry.button("btnDesaPreset").setBounds(295, 6, 65, 26);
		presetBar.add(registry.button("btnDesaPreset"));

		UIComponents.styleSecondaryButton(registry.button("btnEsborraPreset"));
		registry.button("btnEsborraPreset").setBounds(364, 6, 75, 26);
		presetBar.add(registry.button("btnEsborraPreset"));

		add(presetBar, BorderLayout.NORTH);

		panelFiltros = new JPanel();
		panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
		panelFiltros.setBackground(UITheme.palette().bgPanel());
		panelFiltros.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

		JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1.setBackground(UITheme.palette().bgPanel());

		styleCheckBox(registry.checkBox("chckbxLlegit"));
		row1.add(registry.checkBox("chckbxLlegit"));
		styleCheckBox(registry.checkBox("chckbxNoLlegit"));
		row1.add(registry.checkBox("chckbxNoLlegit"));

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

		panelFiltros.add(row1);

		JPanel row1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1b.setBackground(UITheme.palette().bgPanel());
		for (FilterRow r : List.of(
				new FilterRow(I18n.t("filter_editorial_lbl"), registry.textField("filterEditorial")),
				new FilterRow(I18n.t("filter_serie_lbl"),     registry.textField("filterSerie")),
				new FilterRow(I18n.t("filter_idioma_lbl"),    registry.textField("filterIdioma")))) {
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
		panelFiltros.add(row1b);

		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row2.setBackground(UITheme.palette().bgPanel());
		for (FilterRange r : List.of(
				new FilterRange(I18n.t("filter_any_lbl"),       registry.textField("anyMin"),       registry.textField("anyMax")),
				new FilterRange(I18n.t("filter_valoracio_lbl"), registry.textField("valoracioMin"), registry.textField("valoracioMax")),
				new FilterRange(I18n.t("filter_preu_lbl"),      registry.textField("preuMin"),      registry.textField("preuMax")))) {
			row2.add(makeRangeWrap(r.labelKey(), r.min(), r.max()));
		}

		row2.add(makeSep());

		UIComponents.styleAccentButton(registry.button("bttnFiltrar"));
		row2.add(registry.button("bttnFiltrar"));

		UIComponents.styleSecondaryButton(registry.button("bttnQuitarFiltros"));
		row2.add(registry.button("bttnQuitarFiltros"));

		panelFiltros.add(row2);

		JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row3.setBackground(UITheme.palette().bgPanel());

		UIComponents.styleSecondaryButton(registry.button("btnExportDropdown"));
		registry.button("btnExportDropdown").setText(I18n.t("btn_export_lbl") + " \u25BE");
		JPopupMenu exportMenu = new JPopupMenu();
		addExportMenuItem(exportMenu, "btn_export_csv",     "tip_export_csv",     "btnExportCSV");
		addExportMenuItem(exportMenu, "btn_export_json_lbl","tip_export_json",    "btnExportJSON");
		addExportMenuItem(exportMenu, "btn_export_html_lbl","tip_export_html",    "btnExportHTML");
		addExportMenuItem(exportMenu, "btn_export_pdf_lbl", "tip_export_pdf",     "btnExportPDF");
		registry.button("btnExportDropdown").addActionListener(e ->
			exportMenu.show(registry.button("btnExportDropdown"), 0, registry.button("btnExportDropdown").getHeight()));
		row3.add(registry.button("btnExportDropdown"));

		UIComponents.styleSecondaryButton(registry.button("btnImportDropdown"));
		registry.button("btnImportDropdown").setText(I18n.t("btn_import_lbl") + " \u25BE");
		JPopupMenu importMenu = new JPopupMenu();
		addImportMenuItem(importMenu, "btn_import_csv",     "tip_import_csv",     "btnImportarCSV");
		addImportMenuItem(importMenu, "btn_import_json_lbl","tip_import_json",    "btnImportarJSON");
		addImportMenuItem(importMenu, "btn_import_calibre", "tip_import_calibre", "btnImportarCalibre");
		registry.button("btnImportDropdown").addActionListener(e ->
			importMenu.show(registry.button("btnImportDropdown"), 0, registry.button("btnImportDropdown").getHeight()));
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

		panelFiltros.add(row3);

		scrolpaneFiltro = new JScrollPane(panelFiltros);
		scrolpaneFiltro.setBorder(null);
		scrolpaneFiltro.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrolpaneFiltro.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrolpaneFiltro.getViewport().setBackground(UITheme.palette().bgPanel());

		add(scrolpaneFiltro, BorderLayout.CENTER);
	}

	private void addExportMenuItem(JPopupMenu menu, String labelKey, String tipKey, String targetKey) {
		JMenuItem mi = new JMenuItem(I18n.t(labelKey));
		mi.setToolTipText(I18n.t(tipKey));
		JButton target = registry.button(targetKey);
		mi.addActionListener(e -> target.doClick());
		menu.add(mi);
	}

	private void addImportMenuItem(JPopupMenu menu, String labelKey, String tipKey, String targetKey) {
		addExportMenuItem(menu, labelKey, tipKey, targetKey);
	}

	private void styleCheckBox(JCheckBox cb) {
		cb.setFont(UITheme.fontBase());
		cb.setBackground(UITheme.palette().bgPanel());
		cb.setForeground(UITheme.palette().textDark());
	}

	public void applyTheme() {
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
		registry.comboBox("comboPresets").setFont(UITheme.fontBase());
		setBackground(UITheme.palette().bgMain());
	}

	public void applyThemePostLaf() {
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
		JLabel dash = new JLabel("\u2013");
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
		panelFiltros.setBackground(UITheme.palette().bgPanel());
		scrolpaneFiltro.getViewport().setBackground(UITheme.palette().bgPanel());
		for (Component r : panelFiltros.getComponents()) {
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

	public JPanel getPanelFiltros()                { return panelFiltros; }
	public JScrollPane getScrolpaneFiltro()        { return scrolpaneFiltro; }
	public FormFieldRegistry getRegistry()         { return registry; }

	public JTextField getTextISBN()                 { return registry.textField("textISBN"); }
	public JTextField getTextNom()                  { return registry.textField("textNom"); }
	public JTextField getTextAutor()                { return registry.textField("textAutor"); }
	public JTextField getAnyMin()                   { return registry.textField("anyMin"); }
	public JTextField getAnyMax()                   { return registry.textField("anyMax"); }
	public JTextField getValoracioMin()             { return registry.textField("valoracioMin"); }
	public JTextField getValoracioMax()             { return registry.textField("valoracioMax"); }
	public JTextField getPreuMin()                  { return registry.textField("preuMin"); }
	public JTextField getPreuMax()                  { return registry.textField("preuMax"); }
	public JCheckBox getchckbxLlegit()              { return registry.checkBox("chckbxLlegit"); }
	public JCheckBox getchckbxNoLlegit()            { return registry.checkBox("chckbxNoLlegit"); }
	public JButton getbtnFiltrar()                  { return registry.button("bttnFiltrar"); }
	public JButton getBtnQuitarFiltros()           { return registry.button("bttnQuitarFiltros"); }
	public JComboBox<Object> getComboTagFilter()    { return registry.comboBox("comboTagFilter"); }
	public JTextField getFilterEditorial()          { return registry.textField("filterEditorial"); }
	public JTextField getFilterSerie()              { return registry.textField("filterSerie"); }
	public JTextField getFilterIdioma()             { return registry.textField("filterIdioma"); }
	public JComboBox<String> getFilterFormat()      { return registry.comboBox("filterFormat"); }
	public JComboBox<String> getComboPresets()      { return registry.comboBox("comboPresets"); }
	public JButton getBtnCarregaPreset()            { return registry.button("btnCarregaPreset"); }
	public JButton getBtnDesaPreset()               { return registry.button("btnDesaPreset"); }
	public JButton getBtnEsborraPreset()            { return registry.button("btnEsborraPreset"); }
	public JButton getBtnExportCSV()                { return registry.button("btnExportCSV"); }
	public JButton getBtnImportarCSV()              { return registry.button("btnImportarCSV"); }
	public JButton getBtnImportarCalibre()          { return registry.button("btnImportarCalibre"); }
	public JButton getBtnExportJSON()               { return registry.button("btnExportJSON"); }
	public JButton getBtnImportarJSON()             { return registry.button("btnImportarJSON"); }
	public JButton getBtnExportHTML()               { return registry.button("btnExportHTML"); }
	public JButton getBtnExportPDF()                { return registry.button("btnExportPDF"); }
	public JButton getBtnFetchCovers()              { return registry.button("btnFetchCovers"); }
	public JButton getBtnEscanejarISBN()            { return registry.button("btnEscanejarISBN"); }
	public JButton getBtnBackupBD()                 { return registry.button("btnBackupBD"); }
	public JButton getBtnRestaurarBD()              { return registry.button("btnRestaurarBD"); }

	/**
	 * Declarative triple for a single labelled text-field filter row.
	 * Used by {@link #buildFilterDrawer()} to declare the three "row1b"
	 * text fields (editorial / serie / idioma) without repeating the
	 * label-key + new-JTextField boilerplate three times.
	 */
	record FilterRow(String labelKey, JTextField field) {}

	/**
	 * Declarative triple for a single numeric-range filter row.
	 * Used by {@link #buildFilterDrawer()} to declare the three "row2"
	 * range fields (any / valoracio / preu) without repeating the
	 * label-key + two new-JTextFields boilerplate three times.
	 */
	record FilterRange(String labelKey, JTextField min, JTextField max) {}
}
