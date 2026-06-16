package presentacio.detalles.vista;



import presentacio.UIComponents;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import herramienta.I18n;
import herramienta.UITheme;
import presentacio.FormFieldRegistry;
import presentacio.FormFieldRegistry.Field;
import presentacio.FormGridBuilder;

public class GuardarLlibresDialogo extends JDialog {

	private static final int ENTRY_MIN_W = 240;

	private JLabel     labelPreview;
	private JButton    btnGuardar;
	private JButton    btnSeleccionarImatge;
	private JButton    btnCercaInternet;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textEditorial;
	private JTextField textSerie;
	private JTextField textVolum;
	private JTextField textDataCompra;
	private JTextField textDataLectura;
	private JTextField textIdioma;
	private JComboBox<String> comboFormat;
	private JCheckBox  chckDesitjat;
	private JTextField textPortada;
	private JCheckBox  chckLlegit;
	private JTextArea  textNotes;
	private JTextField textPaisOrigen;
	private JComboBox<String> comboEstat;
	private JTextField textExemplars;
	private JTextField textNomCa;
	private JTextField textNomEs;
	private JTextField textNomEn;
	private JProgressBar progressBar;

	public GuardarLlibresDialogo() {
		setModal(true);
		setResizable(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle(I18n.t("modal_new_book"));
		setMinimumSize(new Dimension(400, 500));
		setPreferredSize(new Dimension(600, 720));
		setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(UITheme.palette().bgPanel());

		JPanel west = new JPanel();
		west.setBackground(UITheme.palette().bgPanel());
		west.setPreferredSize(new Dimension(155, 0));
		west.setMinimumSize(new Dimension(155, 0));
		west.setMaximumSize(new Dimension(155, Integer.MAX_VALUE));
		west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
		west.setBorder(new EmptyBorder(8, 8, 8, 6));

		labelPreview = new JLabel();
		labelPreview.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		labelPreview.setHorizontalAlignment(SwingConstants.CENTER);
		labelPreview.setPreferredSize(new Dimension(135, 135));
		labelPreview.setMinimumSize(new Dimension(135, 135));
		labelPreview.setMaximumSize(new Dimension(139, 135));
		labelPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
		west.add(labelPreview);
		west.add(Box.createVerticalStrut(6));

		btnGuardar = new JButton(I18n.t("btn_save_java"));
		UIComponents.styleAccentButton(btnGuardar);

		JButton btnCancel = new JButton(I18n.t("btn_cancel"));
		UIComponents.styleSecondaryButton(btnCancel);
		btnCancel.addActionListener(e -> dispose());

		btnSeleccionarImatge = new JButton(I18n.t("btn_sel_imatge"));
		UIComponents.styleSecondaryButton(btnSeleccionarImatge);

		for (JButton btn : new JButton[]{btnGuardar, btnCancel, btnSeleccionarImatge}) {
			btn.setAlignmentX(Component.LEFT_ALIGNMENT);
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
			west.add(btn);
			west.add(Box.createVerticalStrut(4));
		}

		add(west, BorderLayout.WEST);

		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.palette().bgPanel());
		grid.setBorder(new EmptyBorder(8, 4, 4, 8));

		FormFieldRegistry reg = new FormFieldRegistry()
			.add(Field.text("textISBN",       "field_isbn",        10))
			.add(Field.text("textNom",        "field_title",       10))
			.add(Field.text("textAutor",      "field_author",      10))
			.add(Field.text("textAny",        "field_year",        10))
			.add(Field.text("textDescripcio", "field_description", 10))
			.add(Field.text("textValoracio",  "field_rating",      10))
			.add(Field.text("textPreu",       "field_price",       10))
			.add(Field.text("textEditorial",  "field_publisher",   10))
			.add(Field.text("textSerie",      "field_series",      10))
			.add(Field.text("textVolum",      "field_volume",      10))
			.add(Field.text("textDataCompra", "field_purchased",   10))
			.add(Field.text("textDataLectura","field_read_on",     10))
			.add(Field.text("textIdioma",     "field_language",    10))
			.add(Field.combo("comboFormat",   "field_format",      null, 0))
			.add(Field.check("chckDesitjat",  null,                "tip_desitjat"))
			.add(Field.check("chckLlegit",    null,                null))
			.add(Field.text("textPortada",    "col_cover",         10))
			.add(Field.text("textPaisOrigen", "field_country",     10))
			.add(Field.combo("comboEstat",    "field_estat",       null, 0))
			.add(Field.text("textExemplars",  "field_exemplars",   10))
			.add(Field.text("textNomCa",      "field_title_ca",    10))
			.add(Field.text("textNomEs",      "field_title_es",    10))
			.add(Field.text("textNomEn",      "field_title_en",    10))
			.build();

		for (Field f : reg.specsOfKind(FormFieldRegistry.Kind.TEXT)) {
			UIComponents.styleField(reg.textField(f.key()));
		}

		textISBN       = reg.textField("textISBN");
		textNom        = reg.textField("textNom");
		textAutor      = reg.textField("textAutor");
		textAny        = reg.textField("textAny");
		textDescripcio = reg.textField("textDescripcio");
		textValoracio  = reg.textField("textValoracio");
		textPreu       = reg.textField("textPreu");
		textEditorial  = reg.textField("textEditorial");
		textSerie      = reg.textField("textSerie");
		textVolum      = reg.textField("textVolum");
		textDataCompra  = reg.textField("textDataCompra");
		textDataLectura = reg.textField("textDataLectura");
		textDataCompra.setToolTipText("YYYY-MM-DD");
		textDataLectura.setToolTipText("YYYY-MM-DD");
		textIdioma     = reg.textField("textIdioma");
		textPortada    = reg.textField("textPortada");
		textPaisOrigen = reg.textField("textPaisOrigen");
		textExemplars  = reg.textField("textExemplars");
		textNomCa      = reg.textField("textNomCa");
		textNomEs      = reg.textField("textNomEs");
		textNomEn      = reg.textField("textNomEn");

		comboFormat    = reg.comboBox("comboFormat");
		comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(herramienta.FormatOptions.withBlank()));
		comboFormat.setBackground(UITheme.palette().bgMain());
		comboFormat.setForeground(UITheme.palette().textDark());
		comboFormat.setFont(UITheme.fontBase());
		comboEstat     = reg.comboBox("comboEstat");
		comboEstat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"", I18n.t("estat_nou"), I18n.t("estat_bo"), I18n.t("estat_usat"), I18n.t("estat_deteriorat")}));
		comboEstat.setBackground(UITheme.palette().bgMain());
		comboEstat.setForeground(UITheme.palette().textDark());
		comboEstat.setFont(UITheme.fontBase());
		chckDesitjat   = reg.checkBox("chckDesitjat");
		chckDesitjat.setBackground(UITheme.palette().bgPanel());
		chckDesitjat.setHorizontalAlignment(SwingConstants.LEFT);
		chckLlegit     = reg.checkBox("chckLlegit");
		chckLlegit.setBackground(UITheme.palette().bgPanel());
		chckLlegit.setHorizontalAlignment(SwingConstants.LEFT);

		textNotes      = new JTextArea(4, 20); textNotes.setLineWrap(true); textNotes.setWrapStyleWord(true);
		textNotes.setBackground(UITheme.palette().bgMain());
		textNotes.setForeground(UITheme.palette().textDark());
		textNotes.setFont(UITheme.fontBase());
		textNotes.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.palette().borderClr()));

		FormGridBuilder builder = FormGridBuilder.columnsOf(grid);
		builder.addField(I18n.t("field_isbn"), textISBN);
		builder.addField(I18n.t("field_title"), textNom);
		builder.addField(I18n.t("field_author"), textAutor);
		builder.addField(I18n.t("field_year"), textAny);
		builder.addField(I18n.t("field_description"), textDescripcio);
		builder.addField(I18n.t("field_rating"), textValoracio);
		builder.addField(I18n.t("field_price"), textPreu);
		builder.addField(I18n.t("field_publisher"), textEditorial);
		builder.addField(I18n.t("field_series"), textSerie);
		builder.addField(I18n.t("field_volume"), textVolum);
		builder.addField(I18n.t("field_purchased"), textDataCompra);
		builder.addField(I18n.t("field_read_on"), textDataLectura);
		builder.addField(I18n.t("field_language"), textIdioma);
		builder.addCombo(I18n.t("field_format"), comboFormat);
		builder.addCheck(I18n.t("field_wishlist"), chckDesitjat);
		builder.addCheck(I18n.t("field_read"), chckLlegit);
		builder.addRaw(I18n.t("field_notes"), new javax.swing.JScrollPane(textNotes));
		builder.addField(I18n.t("col_cover"), textPortada);
		builder.addField(I18n.t("field_country"), textPaisOrigen);
		builder.addCombo(I18n.t("field_estat"), comboEstat);
		builder.addField(I18n.t("field_exemplars"), textExemplars);
		builder.addField(I18n.t("field_title_ca"), textNomCa);
		builder.addField(I18n.t("field_title_es"), textNomEs);
		builder.addField(I18n.t("field_title_en"), textNomEn);

		JScrollPane scroll = new JScrollPane(grid,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());
		add(scroll, BorderLayout.CENTER);

		scroll.getViewport().addComponentListener(new ComponentAdapter() {
			private int lastCols = 2;
			@Override
			public void componentResized(ComponentEvent e) {
				int vpW = scroll.getViewport().getWidth();
				if (vpW <= 0) return;
				int cols = Math.max(1, vpW / ENTRY_MIN_W);
				if (cols != lastCols) {
					lastCols = cols;
					((GridLayout) grid.getLayout()).setColumns(cols);
					grid.revalidate();
					grid.repaint();
				}
			}
		});

		JPanel south = new JPanel(new BorderLayout(0, 2));
		south.setBackground(UITheme.palette().bgPanel());
		south.setBorder(new EmptyBorder(0, 8, 8, 8));

		btnCercaInternet = new JButton(I18n.t("btn_cerca_internet"));
		UIComponents.styleAccentButton(btnCercaInternet);
		btnCercaInternet.setBackground(herramienta.UITheme.palette().green());
		btnCercaInternet.setForeground(java.awt.Color.WHITE);
		btnCercaInternet.setToolTipText(I18n.t("tip_cerca_internet"));
		south.add(btnCercaInternet, BorderLayout.CENTER);

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setPreferredSize(new Dimension(0, 8));
		progressBar.setVisible(false);
		south.add(progressBar, BorderLayout.SOUTH);

		add(south, BorderLayout.SOUTH);
	}

	// ── getters ───────────────────────────────────────────────────────────────

	public JLabel        getLabelPreview()       { return labelPreview; }
	public JProgressBar  getProgressBar()        { return progressBar; }
	public JButton       getBtnGuardar()         { return btnGuardar; }
	public JButton       getBtnSeleccionarImatge(){ return btnSeleccionarImatge; }
	public JButton       getBtnCercaInternet()   { return btnCercaInternet; }
	public JTextField    getTextISBN()           { return textISBN; }
	public JTextField    getTextNom()            { return textNom; }
	public JTextField    getTextAutor()          { return textAutor; }
	public JTextField    getTextAny()            { return textAny; }
	public JTextField    getTextDescripcio()     { return textDescripcio; }
	public JTextField    getTextValoracio()      { return textValoracio; }
	public JTextField    getTextPreu()           { return textPreu; }
	public JTextField    getTextEditorial()      { return textEditorial; }
	public JTextField    getTextSerie()          { return textSerie; }
	public JTextField    getTextVolum()          { return textVolum; }
	public JTextField    getTextDataCompra()     { return textDataCompra; }
	public JTextField    getTextDataLectura()    { return textDataLectura; }
	public JTextField    getTextIdioma()         { return textIdioma; }
	public JComboBox<String> getComboFormat()    { return comboFormat; }
	public JCheckBox     getChckDesitjat()       { return chckDesitjat; }
	public JTextField    getTextPortada()        { return textPortada; }
	public JCheckBox     getChckLlegit()         { return chckLlegit; }
	public JTextArea     getTextNotes()          { return textNotes; }
	public JTextField    getTextPaisOrigen()     { return textPaisOrigen; }
	public JComboBox<String> getComboEstat()     { return comboEstat; }
	public JTextField    getTextExemplars()      { return textExemplars; }
	public JTextField    getTextNomCa()          { return textNomCa; }
	public JTextField    getTextNomEs()          { return textNomEs; }
	public JTextField    getTextNomEn()          { return textNomEn; }
}
