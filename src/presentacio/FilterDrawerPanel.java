package presentacio;



import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

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

public class FilterDrawerPanel extends JPanel {

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
	private JComboBox<Object> comboTagFilter;
	private JTextField filterEditorial;
	private JTextField filterSerie;
	private JTextField filterIdioma;
	private JComboBox<String> filterFormat;
	private JComboBox<String> comboPresets;
	private JButton btnCarregaPreset;
	private JButton btnDesaPreset;
	private JButton btnEsborraPreset;
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
	private JButton btnBackupBD;
	private JButton btnRestaurarBD;

	public FilterDrawerPanel() {
		buildFilterDrawer();
	}

	private void buildFilterDrawer() {
		setLayout(new BorderLayout(0, 0));
		setBackground(UITheme.BG_MAIN);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_CLR));

		JPanel presetBar = new JPanel(null);
		presetBar.setBackground(UITheme.BG_MAIN);
		presetBar.setPreferredSize(new Dimension(0, 38));

		JLabel lblPresets = new JLabel(I18n.t("lbl_preset_colon"));
		UIComponents.styleLabel(lblPresets);
		lblPresets.setBounds(8, 9, 50, 20);
		presetBar.add(lblPresets);

		comboPresets = new JComboBox<>();
		comboPresets.setFont(UITheme.fontBase());
		comboPresets.setToolTipText(I18n.t("tip_combo_presets"));
		comboPresets.setBounds(62, 6, 150, 26);
		presetBar.add(comboPresets);

		btnCarregaPreset = new JButton(I18n.t("btn_load_preset"));
		UIComponents.styleAccentButton(btnCarregaPreset);
		btnCarregaPreset.setToolTipText(I18n.t("tip_load_preset"));
		btnCarregaPreset.setBounds(216, 6, 75, 26);
		presetBar.add(btnCarregaPreset);

		btnDesaPreset = new JButton(I18n.t("btn_save_preset_lbl"));
		UIComponents.styleSecondaryButton(btnDesaPreset);
		btnDesaPreset.setToolTipText(I18n.t("tip_save_preset"));
		btnDesaPreset.setBounds(295, 6, 65, 26);
		presetBar.add(btnDesaPreset);

		btnEsborraPreset = new JButton(I18n.t("btn_delete_preset_lbl"));
		UIComponents.styleSecondaryButton(btnEsborraPreset);
		btnEsborraPreset.setToolTipText(I18n.t("tip_delete_preset"));
		btnEsborraPreset.setBounds(364, 6, 75, 26);
		presetBar.add(btnEsborraPreset);

		add(presetBar, BorderLayout.NORTH);

		panelFiltros = new JPanel();
		panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
		panelFiltros.setBackground(UITheme.BG_PANEL);
		panelFiltros.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

		JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1.setBackground(UITheme.BG_PANEL);

		chckbxLlegit = new JCheckBox(I18n.t("filter_llegit_chk"));
		chckbxLlegit.setFont(UITheme.fontBase());
		chckbxLlegit.setBackground(UITheme.BG_PANEL);
		chckbxLlegit.setForeground(UITheme.TEXT_DARK);
		chckbxLlegit.setToolTipText(I18n.t("tip_filter_llegit"));
		row1.add(chckbxLlegit);

		chckbxNoLlegit = new JCheckBox(I18n.t("filter_no_llegit_chk"));
		chckbxNoLlegit.setFont(UITheme.fontBase());
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
		UIComponents.styleLabel(lblTag);
		row1.add(lblTag);
		comboTagFilter = new JComboBox<>();
		comboTagFilter.setFont(UITheme.fontBase());
		comboTagFilter.setToolTipText(I18n.t("tip_filter_tag"));
		comboTagFilter.setPreferredSize(new Dimension(140, 28));
		row1.add(comboTagFilter);

		panelFiltros.add(row1);

		JPanel row1b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row1b.setBackground(UITheme.BG_PANEL);
		// Use the FilterRow helper for the three text-field rows: one
		// (label, field, columns) triple per row keeps the call site
		// declarative and the helper centralises the label/field wrap.
		for (FilterRow r : List.of(
				new FilterRow(I18n.t("filter_editorial_lbl"),
					filterEditorial = new JTextField(14)),
				new FilterRow(I18n.t("filter_serie_lbl"),
					filterSerie     = new JTextField(12)),
				new FilterRow(I18n.t("filter_idioma_lbl"),
					filterIdioma    = new JTextField(10)))) {
			row1b.add(makeFieldWrap(r.labelKey(), r.field()));
		}
		JLabel lblFormat = new JLabel(I18n.t("filter_format_lbl") + ":");
		UIComponents.styleLabel(lblFormat);
		row1b.add(lblFormat);
		filterFormat = new JComboBox<>(new String[]{"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"), I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")});
		filterFormat.setFont(UITheme.fontBase());
		filterFormat.setPreferredSize(new Dimension(130, 28));
		row1b.add(filterFormat);
		panelFiltros.add(row1b);

		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row2.setBackground(UITheme.BG_PANEL);

		// Three numeric-range rows: each declared as a (label, min, max)
		// triple so the range-wrap helper applies consistently.
		for (FilterRange r : List.of(
				new FilterRange(I18n.t("filter_any_lbl"),
					anyMin = new JTextField(5), anyMax = new JTextField(5)),
				new FilterRange(I18n.t("filter_valoracio_lbl"),
					valoracioMin = new JTextField(5), valoracioMax = new JTextField(5)),
				new FilterRange(I18n.t("filter_preu_lbl"),
					preuMin = new JTextField(5), preuMax = new JTextField(5)))) {
			row2.add(makeRangeWrap(r.labelKey(), r.min(), r.max()));
		}

		row2.add(makeSep());

		bttnFiltrar = new JButton(I18n.t("btn_filtrar"));
		UIComponents.styleAccentButton(bttnFiltrar);
		bttnFiltrar.setToolTipText(I18n.t("tip_filtrar"));
		row2.add(bttnFiltrar);

		bttnQuitarFiltros = new JButton(I18n.t("btn_quitar_filtres"));
		UIComponents.styleSecondaryButton(bttnQuitarFiltros);
		bttnQuitarFiltros.setToolTipText(I18n.t("tip_quitar_filtres"));
		row2.add(bttnQuitarFiltros);

		panelFiltros.add(row2);

		JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		row3.setBackground(UITheme.BG_PANEL);

		btnExportCSV = new JButton();
		btnExportJSON = new JButton();
		btnExportHTML = new JButton();
		btnExportPDF = new JButton();

		btnExportDropdown = new JButton(I18n.t("btn_export_lbl") + " ▾");
		UIComponents.styleSecondaryButton(btnExportDropdown);
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
		UIComponents.styleSecondaryButton(btnImportDropdown);
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
		UIComponents.styleSecondaryButton(btnFetchCovers);
		btnFetchCovers.setToolTipText(I18n.t("tip_fetch_covers"));
		row3.add(btnFetchCovers);

		btnEscanejarISBN = new JButton(I18n.t("btn_scan_isbn_lbl"));
		UIComponents.styleSecondaryButton(btnEscanejarISBN);
		btnEscanejarISBN.setToolTipText(I18n.t("tip_scan_isbn"));
		row3.add(btnEscanejarISBN);

		row3.add(makeSep());

		btnBackupBD = new JButton(I18n.t("btn_backup_bd"));
		UIComponents.styleSecondaryButton(btnBackupBD);
		btnBackupBD.setToolTipText(I18n.t("tip_backup_bd"));
		row3.add(btnBackupBD);

		btnRestaurarBD = new JButton(I18n.t("btn_restore_bd"));
		UIComponents.styleSecondaryButton(btnRestaurarBD);
		btnRestaurarBD.setToolTipText(I18n.t("tip_restore_bd"));
		row3.add(btnRestaurarBD);

		panelFiltros.add(row3);

		scrolpaneFiltro = new JScrollPane(panelFiltros);
		scrolpaneFiltro.setBorder(null);
		scrolpaneFiltro.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrolpaneFiltro.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrolpaneFiltro.getViewport().setBackground(UITheme.BG_PANEL);

		add(scrolpaneFiltro, BorderLayout.CENTER);
	}

	public void applyTheme() {
		styleFilterComponents();
		UIComponents.styleAccentButton(bttnFiltrar);
		UIComponents.styleSecondaryButton(bttnQuitarFiltros);
		UIComponents.styleSecondaryButton(btnExportDropdown);
		UIComponents.styleSecondaryButton(btnImportDropdown);
		UIComponents.styleSecondaryButton(btnFetchCovers);
		UIComponents.styleSecondaryButton(btnEscanejarISBN);
		UIComponents.styleSecondaryButton(btnBackupBD);
		UIComponents.styleSecondaryButton(btnRestaurarBD);
		UIComponents.styleAccentButton(btnCarregaPreset);
		UIComponents.styleSecondaryButton(btnDesaPreset);
		UIComponents.styleSecondaryButton(btnEsborraPreset);
		comboPresets.setFont(UITheme.fontBase());
		setBackground(UITheme.BG_MAIN);
	}

	public void applyThemePostLaf() {
		styleFilterComponents();
		UIComponents.styleAccentButton(bttnFiltrar);
		UIComponents.styleSecondaryButton(bttnQuitarFiltros);
		UIComponents.styleSecondaryButton(btnExportDropdown);
		UIComponents.styleSecondaryButton(btnImportDropdown);
		UIComponents.styleSecondaryButton(btnFetchCovers);
		UIComponents.styleSecondaryButton(btnEscanejarISBN);
		UIComponents.styleSecondaryButton(btnBackupBD);
		UIComponents.styleSecondaryButton(btnRestaurarBD);
		UIComponents.styleAccentButton(btnCarregaPreset);
		UIComponents.styleSecondaryButton(btnDesaPreset);
		UIComponents.styleSecondaryButton(btnEsborraPreset);
	}

	private JPanel makeFieldWrap(String label, JTextField field) {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(UITheme.BG_PANEL);
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
		p.setBackground(UITheme.BG_PANEL);
		JLabel lbl = new JLabel(label + ":");
		UIComponents.styleLabel(lbl);
		UIComponents.styleField(min);
		UIComponents.styleField(max);
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

	private void styleFilterComponents() {
		panelFiltros.setBackground(UITheme.BG_PANEL);
		scrolpaneFiltro.getViewport().setBackground(UITheme.BG_PANEL);
		for (Component r : panelFiltros.getComponents()) {
			if (r instanceof JPanel) {
				((JPanel) r).setBackground(UITheme.BG_PANEL);
				for (Component c : ((JPanel) r).getComponents()) {
					if (c instanceof JCheckBox) {
						((JCheckBox) c).setBackground(UITheme.BG_PANEL);
						((JCheckBox) c).setForeground(UITheme.TEXT_DARK);
						((JCheckBox) c).setFont(UITheme.fontBase());
					} else if (c instanceof JLabel) {
						UIComponents.styleLabel((JLabel) c);
					} else if (c instanceof JTextField) {
						UIComponents.styleField((JTextField) c);
					} else if (c instanceof JPanel) {
						((JPanel) c).setBackground(UITheme.BG_PANEL);
						for (Component cc : ((JPanel) c).getComponents()) {
							if (cc instanceof JLabel) UIComponents.styleLabel((JLabel) cc);
							if (cc instanceof JTextField) UIComponents.styleField((JTextField) cc);
						}
					}
				}
			}
		}
	}

	public JPanel getPanelFiltros()          { return panelFiltros; }
	public JScrollPane getScrolpaneFiltro()   { return scrolpaneFiltro; }
	public JTextField getTextISBN()           { return textISBN; }
	public JTextField getTextNom()            { return textNom; }
	public JTextField getTextAutor()          { return textAutor; }
	public JTextField getAnyMin()             { return anyMin; }
	public JTextField getAnyMax()             { return anyMax; }
	public JTextField getValoracioMin()       { return valoracioMin; }
	public JTextField getValoracioMax()       { return valoracioMax; }
	public JTextField getPreuMin()            { return preuMin; }
	public JTextField getPreuMax()            { return preuMax; }
	public JCheckBox getchckbxLlegit()        { return chckbxLlegit; }
	public JCheckBox getchckbxNoLlegit()      { return chckbxNoLlegit; }
	public JButton getbtnFiltrar()            { return bttnFiltrar; }
	public JButton getbttnQuitarFiltros()     { return bttnQuitarFiltros; }
	public JComboBox<Object> getComboTagFilter() { return comboTagFilter; }
	public JTextField getFilterEditorial()       { return filterEditorial; }
	public JTextField getFilterSerie()           { return filterSerie; }
	public JTextField getFilterIdioma()          { return filterIdioma; }
	public JComboBox<String> getFilterFormat()   { return filterFormat; }
	public JComboBox<String> getComboPresets(){ return comboPresets; }
	public JButton getBtnCarregaPreset()      { return btnCarregaPreset; }
	public JButton getBtnDesaPreset()         { return btnDesaPreset; }
	public JButton getBtnEsborraPreset()      { return btnEsborraPreset; }
	public JButton getBtnExportCSV()          { return btnExportCSV; }
	public JButton getBtnImportarCSV()        { return btnImportarCSV; }
	public JButton getBtnImportarCalibre()    { return btnImportarCalibre; }
	public JButton getBtnExportJSON()         { return btnExportJSON; }
	public JButton getBtnImportarJSON()       { return btnImportarJSON; }
	public JButton getBtnExportHTML()         { return btnExportHTML; }
	public JButton getBtnExportPDF()          { return btnExportPDF; }
	public JButton getBtnFetchCovers()        { return btnFetchCovers; }
	public JButton getBtnEscanejarISBN()      { return btnEscanejarISBN; }
	public JButton getBtnBackupBD()           { return btnBackupBD; }
	public JButton getBtnRestaurarBD()        { return btnRestaurarBD; }

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
