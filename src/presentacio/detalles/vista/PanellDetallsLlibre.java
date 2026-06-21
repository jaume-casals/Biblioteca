package presentacio.detalles.vista;



import presentacio.RegistreCampsFormulari;
import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;

public class PanellDetallsLlibre extends JDialog {

	private static final int ENTRY_MIN_W = 260;
	private static final int ENTRY_H     = 44;
	private static final int LBL_W       = 92;
	private static final int SIDE_W      = 215;
	private static final int IMG_SIZE    = 185;

	private final RegistreCampsFormulari registry = new RegistreCampsFormulari();
	private final List<JComponent> editableInputs;

	public PanellDetallsLlibre() {
		setModal(true);
		setResizable(true);
		setMinimumSize(new Dimension(480, 480));
		setPreferredSize(new Dimension(800, 680));
		setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(UITheme.palette().bgPanel());

		JPanel east = new JPanel();
		east.setBackground(UITheme.palette().bgPanel());
		east.setPreferredSize(new Dimension(SIDE_W, 0));
		east.setMinimumSize(new Dimension(SIDE_W, 0));
		east.setMaximumSize(new Dimension(SIDE_W, Integer.MAX_VALUE));
		east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
		east.setBorder(new EmptyBorder(8, 6, 8, 8));

		JLabel labelIcono = new JLabel("");
		labelIcono.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		labelIcono.setPreferredSize(new Dimension(IMG_SIZE, IMG_SIZE));
		labelIcono.setMaximumSize(new Dimension(SIDE_W - 14, IMG_SIZE));
		labelIcono.setAlignmentX(Component.LEFT_ALIGNMENT);
		registry.register("labelIcono", labelIcono);
		east.add(labelIcono);
		east.add(Box.createVerticalStrut(6));

		JButton btnSeleccionarImatge = new JButton(I18n.t("btn_sel_imatge_detail"));
		UIComponents.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setEnabled(false);
		registry.register("btnSeleccionarImatge", btnSeleccionarImatge);

		JButton btnEditar = new JButton(I18n.t("btn_edit_java"));
		UIComponents.styleAccentButton(btnEditar);
		registry.register("btnEditar", btnEditar);

		JButton btnEliminar = new JButton(I18n.t("btn_delete"));
		btnEliminar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnEliminar.setBackground(new Color(0xC0392B));
		btnEliminar.setForeground(Color.WHITE);
		btnEliminar.setFont(UITheme.fontBold());
		btnEliminar.setFocusPainted(false);
		btnEliminar.setBorderPainted(false);
		btnEliminar.setOpaque(true);
		btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		registry.register("btnEliminar", btnEliminar);

		JButton btnTancar = new JButton(I18n.t("btn_close"));
		UIComponents.styleSecondaryButton(btnTancar);
		btnTancar.addActionListener(e -> dispose());

		JButton btnGestioLlistes = new JButton(I18n.t("btn_llistes"));
		UIComponents.styleSecondaryButton(btnGestioLlistes);
		btnGestioLlistes.setToolTipText(I18n.t("tip_llistes"));
		registry.register("btnGestioLlistes", btnGestioLlistes);

		JButton btnGestioTags = new JButton(I18n.t("btn_etiquetes"));
		UIComponents.styleSecondaryButton(btnGestioTags);
		btnGestioTags.setToolTipText(I18n.t("tip_etiquetes"));
		registry.register("btnGestioTags", btnGestioTags);

		JButton btnHistorialPrestecs = new JButton(I18n.t("btn_historial_prestecs"));
		UIComponents.styleSecondaryButton(btnHistorialPrestecs);
		btnHistorialPrestecs.setToolTipText(I18n.t("tip_historial_prestecs"));
		registry.register("btnHistorialPrestecs", btnHistorialPrestecs);

		JButton btnImprimir = new JButton(I18n.t("btn_imprimir_detail"));
		UIComponents.styleSecondaryButton(btnImprimir);
		btnImprimir.setToolTipText(I18n.t("tip_imprimir_detail"));
		registry.register("btnImprimir", btnImprimir);

		int btnH = 36;
		for (JButton btn : new JButton[]{
				btnSeleccionarImatge, btnEditar, btnEliminar,
				btnTancar, btnGestioLlistes, btnGestioTags, btnHistorialPrestecs, btnImprimir}) {
			btn.setAlignmentX(Component.LEFT_ALIGNMENT);
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnH));
			east.add(btn);
			east.add(Box.createVerticalStrut(4));
		}

		add(east, BorderLayout.EAST);

		JTabbedPane tabbedPane = new JTabbedPane();
		// Les façanes DetallesGeneralTab/DetallesNotesTab/DetallesAvancatTab,
		// de 9 línies cadascuna, eren simples passarel·les; s'han inlineat
		// aquí segons el finding LOW de tot.txt (la delegació no aportava res).
		tabbedPane.addTab(I18n.t("stats_tab_general"), buildGeneralTab());
		tabbedPane.addTab(I18n.t("field_notes"), buildNotesTab());
		tabbedPane.addTab(I18n.t("tab_advanced"), buildAdvancedTab());
		add(tabbedPane, BorderLayout.CENTER);

		editableInputs = List.of(
			registry.textField("textAny"), registry.textField("textAutor"),
			registry.textField("textDescripcio"), registry.textField("textNom"),
			registry.textField("textPortada"), registry.textField("textPreu"),
			registry.textField("textValoracio"),
			registry.textField("textEditorial"), registry.textField("textSerie"),
			registry.textField("textVolum"), registry.textField("textDataCompra"),
			registry.textField("textDataLectura"), registry.textField("textIdioma"),
			registry.textField("textPaisOrigen"), registry.textField("textExemplars"),
			registry.textField("textLlenguaOriginal"),
			registry.textField("textPagines"), registry.textField("textPaginesLlegides"),
			registry.textField("textNomCa"), registry.textField("textNomEs"),
			registry.textField("textNomEn"),
			registry.comboBox("comboFormat"), registry.comboBox("comboEstat"),
			registry.comprovarBox("chckDesitjat"), registry.comprovarBox("chckLlegit"),
			(JTextArea) registry.get("textNotes"));

		setSize(800, 680);
	}

	JScrollPane buildGeneralTab() {
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.palette().bgPanel());
		grid.setBorder(new EmptyBorder(8, 8, 4, 4));

		afegirFieldEntry(grid, "textISBN",            I18n.t("field_isbn"));
		afegirFieldEntry(grid, "textNom",             I18n.t("field_title"));
		afegirFieldEntry(grid, "textAutor",           I18n.t("field_author"));
		afegirFieldEntry(grid, "textAny",             I18n.t("field_year"));
		afegirFieldEntry(grid, "textDescripcio",      I18n.t("field_description"));
		afegirFieldEntry(grid, "textValoracio",       I18n.t("field_rating"));
		afegirFieldEntry(grid, "textPreu",            I18n.t("field_price"));
		afegirFieldEntry(grid, "textEditorial",       I18n.t("field_publisher"));
		afegirFieldEntry(grid, "textSerie",           I18n.t("field_series"));
		afegirFieldEntry(grid, "textVolum",           I18n.t("field_volume"));
		afegirFieldEntry(grid, "textIdioma",          I18n.t("field_language"));
		afegirFieldEntry(grid, "textPaisOrigen",      I18n.t("field_country"));
		afegirComboEntry(grid, "comboFormat",         I18n.t("field_format"),
				herramienta.text.FormatOptions.withBlank());
		afegirComboEntry(grid, "comboEstat",          I18n.t("field_estat"),
				new String[]{"", I18n.t("estat_nou"), I18n.t("estat_bo"), I18n.t("estat_usat"), I18n.t("estat_deteriorat")});
		afegirCheckEntry(grid, "chckDesitjat",        I18n.t("field_wishlist"),
				I18n.t("tip_desitjat"));
		afegirCheckEntry(grid, "chckLlegit",          I18n.t("field_read"), null);
		afegirFieldEntry(grid, "textPortada",         I18n.t("col_cover"));

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UITheme.palette().bgPanel());
		wrapper.add(grid, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());

		scroll.getViewport().addComponentListener(new ComponentAdapter() {
			private int ultimCols = 2;
			@Override
			public void componentResized(ComponentEvent e) {
				int vpW = scroll.getViewport().getWidth();
				if (vpW <= 0) return;
				int cols = Math.max(1, vpW / ENTRY_MIN_W);
				if (cols != ultimCols) {
					ultimCols = cols;
					((GridLayout) grid.getLayout()).setColumns(cols);
					grid.revalidate();
					grid.repaint();
				}
			}
		});

		return scroll;
	}

	JPanel buildNotesTab() {
		JPanel panel = new JPanel(new BorderLayout(4, 0));
		panel.setBackground(UITheme.palette().bgPanel());
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel lblNotes = makeLabel(I18n.t("field_notes"));
		lblNotes.setVerticalAlignment(SwingConstants.TOP);
		lblNotes.setPreferredSize(new Dimension(LBL_W, 0));
		panel.add(lblNotes, BorderLayout.WEST);

		JTextArea textNotes = new JTextArea(20, 40);
		textNotes.setLineWrap(true);
		textNotes.setWrapStyleWord(true);
		textNotes.setEnabled(false);
		textNotes.setFont(UITheme.fontBase());
		textNotes.setBackground(UITheme.palette().bgMain());
		textNotes.setForeground(UITheme.palette().textDark());
		registry.register("textNotes", textNotes);

		JScrollPane notesScroll = new JScrollPane(textNotes);
		notesScroll.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		panel.add(notesScroll, BorderLayout.CENTER);

		return panel;
	}

	JScrollPane buildAdvancedTab() {
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.palette().bgPanel());
		grid.setBorder(new EmptyBorder(8, 8, 4, 4));

		afegirFieldEntry(grid, "textDataCompra",      I18n.t("field_purchased"));
		afegirFieldEntry(grid, "textDataLectura",     I18n.t("field_read_on"));
		afegirFieldEntry(grid, "textExemplars",       I18n.t("field_exemplars"));
		afegirFieldEntry(grid, "textLlenguaOriginal", I18n.t("field_llengua_original"));
		afegirFieldEntry(grid, "textPagines",         I18n.t("field_pages"));
		afegirFieldEntry(grid, "textPaginesLlegides", I18n.t("field_pages_read"));
		afegirFieldEntry(grid, "textNomCa",           I18n.t("field_title_ca"));
		afegirFieldEntry(grid, "textNomEs",           I18n.t("field_title_es"));
		afegirFieldEntry(grid, "textNomEn",           I18n.t("field_title_en"));

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UITheme.palette().bgPanel());
		wrapper.add(grid, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());

		scroll.getViewport().addComponentListener(new ComponentAdapter() {
			private int ultimCols = 2;
			@Override
			public void componentResized(ComponentEvent e) {
				int vpW = scroll.getViewport().getWidth();
				if (vpW <= 0) return;
				int cols = Math.max(1, vpW / ENTRY_MIN_W);
				if (cols != ultimCols) {
					ultimCols = cols;
					((GridLayout) grid.getLayout()).setColumns(cols);
					grid.revalidate();
					grid.repaint();
				}
			}
		});

		return scroll;
	}

	JTextField afegirFieldEntry(JPanel grid, String key, String label) {
		JPanel entry = entryPanel();
		JLabel lbl = makeLabel(label);
		entry.add(lbl, BorderLayout.WEST);
		JTextField field = new JTextField();
		field.setEnabled(false);
		field.setColumns(10);
		UIComponents.styleField(field);
		// Enllaça l'etiqueta al camp per a lectors de pantalla i
		// navegació amb teclat. La implementació anterior també
		// desava la parella a un RegistreCampsFormulari, però cap
		// consumidor no el consultava mai (veure finding MEDIUM
		// de tot.txt) — l'emmagatzematge era mort. La crida a
		// setLabelFor té comportament visible per si sola, de
		// manera que es conserva.
		lbl.setLabelFor(field);
		entry.add(field, BorderLayout.CENTER);
		grid.add(entry);
		registry.register(key, field);
		return field;
	}

	JComboBox<String> afegirComboEntry(JPanel grid, String key, String label, String[] items) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JComboBox<String> combo = new JComboBox<>(items);
		combo.setEnabled(false);
		combo.setBackground(UITheme.palette().bgMain());
		combo.setForeground(UITheme.palette().textDark());
		combo.setFont(UITheme.fontBase());
		entry.add(combo, BorderLayout.CENTER);
		grid.add(entry);
		registry.register(key, combo);
		return combo;
	}

	JCheckBox afegirCheckEntry(JPanel grid, String key, String label, String tooltip) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JCheckBox chk = new JCheckBox("");
		chk.setEnabled(false);
		chk.setBackground(UITheme.palette().bgPanel());
		chk.setHorizontalAlignment(SwingConstants.LEFT);
		if (tooltip != null) chk.setToolTipText(tooltip);
		entry.add(chk, BorderLayout.CENTER);
		grid.add(entry);
		registry.register(key, chk);
		return chk;
	}

	JPanel entryPanel() {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(UITheme.palette().bgPanel());
		p.setBorder(new EmptyBorder(2, 2, 2, 2));
		p.setPreferredSize(new Dimension(ENTRY_MIN_W, ENTRY_H));
		return p;
	}

	JLabel makeLabel(String text) {
		JLabel lbl = new JLabel(text);
		UIComponents.styleLabel(lbl);
		lbl.setPreferredSize(new Dimension(LBL_W, 0));
		return lbl;
	}

	public JLabel     obtenirLabelIcono()          { return (JLabel) registry.get("labelIcono"); }
	public JButton    obtenirBtnSeleccionarImatge() { return registry.button("btnSeleccionarImatge"); }
	public List<JComponent> obtenirEditableInputs() { return editableInputs; }
	public JButton    obtenirBtnEliminar()          { return registry.button("btnEliminar"); }
	public JButton    obtenirBtnEditar()            { return registry.button("btnEditar"); }
	public JTextField obtenirTextISBN()             { return registry.textField("textISBN"); }
	public JTextField obtenirTextNom()              { return registry.textField("textNom"); }
	public JTextField obtenirTextAutor()            { return registry.textField("textAutor"); }
	public JTextField obtenirTextAny()              { return registry.textField("textAny"); }
	public JTextField obtenirTextDescripcio()       { return registry.textField("textDescripcio"); }
	public JTextField obtenirTextValoracio()        { return registry.textField("textValoracio"); }
	public JTextField obtenirTextPreu()             { return registry.textField("textPreu"); }
	public JTextField obtenirTextPortada()          { return registry.textField("textPortada"); }
	public JTextField obtenirTextEditorial()        { return registry.textField("textEditorial"); }
	public JTextField obtenirTextSerie()            { return registry.textField("textSerie"); }
	public JTextField obtenirTextVolum()            { return registry.textField("textVolum"); }
	public JTextField obtenirTextDataCompra()       { return registry.textField("textDataCompra"); }
	public JTextField obtenirTextDataLectura()      { return registry.textField("textDataLectura"); }
	public JTextField obtenirTextIdioma()           { return registry.textField("textIdioma"); }
	public JTextField obtenirTextPaisOrigen()       { return registry.textField("textPaisOrigen"); }
	@SuppressWarnings("unchecked")
	public JComboBox<String> obtenirComboFormat()   { return (JComboBox<String>) registry.comboBox("comboFormat"); }
	@SuppressWarnings("unchecked")
	public JComboBox<String> obtenirComboEstat()    { return (JComboBox<String>) registry.comboBox("comboEstat"); }
	public JTextField obtenirTextExemplars()        { return registry.textField("textExemplars"); }
	public JTextField obtenirTextLlenguaOriginal()  { return registry.textField("textLlenguaOriginal"); }
	public JCheckBox  obtenirChckDesitjat()         { return registry.comprovarBox("chckDesitjat"); }
	public JTextField obtenirTextPagines()          { return registry.textField("textPagines"); }
	public JTextField obtenirTextPaginesLlegides()  { return registry.textField("textPaginesLlegides"); }
	public JTextField obtenirTextNomCa()            { return registry.textField("textNomCa"); }
	public JTextField obtenirTextNomEs()            { return registry.textField("textNomEs"); }
	public JTextField obtenirTextNomEn()            { return registry.textField("textNomEn"); }
	public JCheckBox  obtenirChckLlegit()           { return registry.comprovarBox("chckLlegit"); }
	public JTextArea  obtenirTextNotes()            { return (JTextArea) registry.get("textNotes"); }
	public JButton    obtenirBtnGestioLlistes()      { return registry.button("btnGestioLlistes"); }
	public JButton    obtenirBtnGestioTags()         { return registry.button("btnGestioTags"); }
	public JButton    obtenirBtnHistorialPrestecs()  { return registry.button("btnHistorialPrestecs"); }
	public JButton    obtenirBtnImprimir()           { return registry.button("btnImprimir"); }
}
