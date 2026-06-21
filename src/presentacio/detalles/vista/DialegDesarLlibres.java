package presentacio.detalles.vista;



import presentacio.util.UIComponents;
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

import herramienta.i18n.I18n;
import herramienta.ui.UITheme;
import presentacio.formularis.RegistreCampsFormulari;
import presentacio.formularis.RegistreCampsFormulari.Camp;
import presentacio.formularis.ConstructorGraellaFormulari;

public class DialegDesarLlibres extends JDialog {

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

	public DialegDesarLlibres() {
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

		RegistreCampsFormulari reg = new RegistreCampsFormulari()
			.add(Camp.text("textISBN",       "field_isbn",        10))
			.add(Camp.text("textNom",        "field_title",       10))
			.add(Camp.text("textAutor",      "field_author",      10))
			.add(Camp.text("textAny",        "field_year",        10))
			.add(Camp.text("textDescripcio", "field_description", 10))
			.add(Camp.text("textValoracio",  "field_rating",      10))
			.add(Camp.text("textPreu",       "field_price",       10))
			.add(Camp.text("textEditorial",  "field_publisher",   10))
			.add(Camp.text("textSerie",      "field_series",      10))
			.add(Camp.text("textVolum",      "field_volume",      10))
			.add(Camp.text("textDataCompra", "field_purchased",   10))
			.add(Camp.text("textDataLectura","field_read_on",     10))
			.add(Camp.text("textIdioma",     "field_language",    10))
			.add(Camp.combo("comboFormat",   "field_format",      null, 0))
			.add(Camp.check("chckDesitjat",  null,                "tip_desitjat"))
			.add(Camp.check("chckLlegit",    null,                null))
			.add(Camp.text("textPortada",    "col_cover",         10))
			.add(Camp.text("textPaisOrigen", "field_country",     10))
			.add(Camp.combo("comboEstat",    "field_estat",       null, 0))
			.add(Camp.text("textExemplars",  "field_exemplars",   10))
			.add(Camp.text("textNomCa",      "field_title_ca",    10))
			.add(Camp.text("textNomEs",      "field_title_es",    10))
			.add(Camp.text("textNomEn",      "field_title_en",    10))
			.build();

		for (Camp f : reg.specsOfKind(RegistreCampsFormulari.Tipus.TEXT)) {
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
		comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(herramienta.text.FormatOptions.withBlank()));
		comboFormat.setBackground(UITheme.palette().bgMain());
		comboFormat.setForeground(UITheme.palette().textDark());
		comboFormat.setFont(UITheme.fontBase());
		comboEstat     = reg.comboBox("comboEstat");
		comboEstat.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"", I18n.t("estat_nou"), I18n.t("estat_bo"), I18n.t("estat_usat"), I18n.t("estat_deteriorat")}));
		comboEstat.setBackground(UITheme.palette().bgMain());
		comboEstat.setForeground(UITheme.palette().textDark());
		comboEstat.setFont(UITheme.fontBase());
		chckDesitjat   = reg.comprovarBox("chckDesitjat");
		chckDesitjat.setBackground(UITheme.palette().bgPanel());
		chckDesitjat.setHorizontalAlignment(SwingConstants.LEFT);
		chckLlegit     = reg.comprovarBox("chckLlegit");
		chckLlegit.setBackground(UITheme.palette().bgPanel());
		chckLlegit.setHorizontalAlignment(SwingConstants.LEFT);

		textNotes      = new JTextArea(4, 20); textNotes.setLineWrap(true); textNotes.setWrapStyleWord(true);
		textNotes.setBackground(UITheme.palette().bgMain());
		textNotes.setForeground(UITheme.palette().textDark());
		textNotes.setFont(UITheme.fontBase());
		textNotes.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.palette().borderClr()));

		ConstructorGraellaFormulari builder = ConstructorGraellaFormulari.columnsOf(grid);
		builder.afegirField(I18n.t("field_isbn"), textISBN);
		builder.afegirField(I18n.t("field_title"), textNom);
		builder.afegirField(I18n.t("field_author"), textAutor);
		builder.afegirField(I18n.t("field_year"), textAny);
		builder.afegirField(I18n.t("field_description"), textDescripcio);
		builder.afegirField(I18n.t("field_rating"), textValoracio);
		builder.afegirField(I18n.t("field_price"), textPreu);
		builder.afegirField(I18n.t("field_publisher"), textEditorial);
		builder.afegirField(I18n.t("field_series"), textSerie);
		builder.afegirField(I18n.t("field_volume"), textVolum);
		builder.afegirField(I18n.t("field_purchased"), textDataCompra);
		builder.afegirField(I18n.t("field_read_on"), textDataLectura);
		builder.afegirField(I18n.t("field_language"), textIdioma);
		builder.afegirCombo(I18n.t("field_format"), comboFormat);
		builder.afegirCheck(I18n.t("field_wishlist"), chckDesitjat);
		builder.afegirCheck(I18n.t("field_read"), chckLlegit);
		builder.afegirRaw(I18n.t("field_notes"), new javax.swing.JScrollPane(textNotes));
		builder.afegirField(I18n.t("col_cover"), textPortada);
		builder.afegirField(I18n.t("field_country"), textPaisOrigen);
		builder.afegirCombo(I18n.t("field_estat"), comboEstat);
		builder.afegirField(I18n.t("field_exemplars"), textExemplars);
		builder.afegirField(I18n.t("field_title_ca"), textNomCa);
		builder.afegirField(I18n.t("field_title_es"), textNomEs);
		builder.afegirField(I18n.t("field_title_en"), textNomEn);

		JScrollPane scroll = new JScrollPane(grid,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());
		add(scroll, BorderLayout.CENTER);

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

		JPanel south = new JPanel(new BorderLayout(0, 2));
		south.setBackground(UITheme.palette().bgPanel());
		south.setBorder(new EmptyBorder(0, 8, 8, 8));

		btnCercaInternet = new JButton(I18n.t("btn_cerca_internet"));
		UIComponents.styleAccentButton(btnCercaInternet);
		btnCercaInternet.setBackground(herramienta.ui.UITheme.palette().green());
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

	public JLabel        obtenirLabelPreview()       { return labelPreview; }
	public JProgressBar  obtenirProgressBar()        { return progressBar; }
	public JButton       obtenirBtnGuardar()         { return btnGuardar; }
	public JButton       obtenirBtnSeleccionarImatge(){ return btnSeleccionarImatge; }
	public JButton       obtenirBtnCercaInternet()   { return btnCercaInternet; }
	public JTextField    obtenirTextISBN()           { return textISBN; }
	public JTextField    obtenirTextNom()            { return textNom; }
	public JTextField    obtenirTextAutor()          { return textAutor; }
	public JTextField    obtenirTextAny()            { return textAny; }
	public JTextField    obtenirTextDescripcio()     { return textDescripcio; }
	public JTextField    obtenirTextValoracio()      { return textValoracio; }
	public JTextField    obtenirTextPreu()           { return textPreu; }
	public JTextField    obtenirTextEditorial()      { return textEditorial; }
	public JTextField    obtenirTextSerie()          { return textSerie; }
	public JTextField    obtenirTextVolum()          { return textVolum; }
	public JTextField    obtenirTextDataCompra()     { return textDataCompra; }
	public JTextField    obtenirTextDataLectura()    { return textDataLectura; }
	public JTextField    obtenirTextIdioma()         { return textIdioma; }
	public JComboBox<String> obtenirComboFormat()    { return comboFormat; }
	public JCheckBox     obtenirChckDesitjat()       { return chckDesitjat; }
	public JTextField    obtenirTextPortada()        { return textPortada; }
	public JCheckBox     obtenirChckLlegit()         { return chckLlegit; }
	public JTextArea     obtenirTextNotes()          { return textNotes; }
	public JTextField    obtenirTextPaisOrigen()     { return textPaisOrigen; }
	public JComboBox<String> obtenirComboEstat()     { return comboEstat; }
	public JTextField    obtenirTextExemplars()      { return textExemplars; }
	public JTextField    obtenirTextNomCa()          { return textNomCa; }
	public JTextField    obtenirTextNomEs()          { return textNomEs; }
	public JTextField    obtenirTextNomEn()          { return textNomEn; }
}
