package presentacio.detalles.vista;

import presentacio.util.UIComponents;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import herramienta.ui.Tokens;
import presentacio.util.Icones;

import javax.swing.BorderFactory;
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
import herramienta.text.EstatOptions;
import herramienta.text.FormatOptions;
import herramienta.ui.UITheme;
import domini.Llibre;
import presentacio.formularis.RegistreCampsFormulari;
import presentacio.formularis.RegistreCampsFormulari.Camp;
import presentacio.formularis.ConstructorGraellaFormulari;

public class DialegDesarLlibres extends JDialog {

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
		setMinimumSize(new Dimension(560, 520));
		setPreferredSize(new Dimension(680, 740));
		setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(UITheme.palette().bgPanel());

		creaCamps();

		add(buildCapcalera(), BorderLayout.NORTH);
		add(buildCos(), BorderLayout.CENTER);
		add(buildPeu(), BorderLayout.SOUTH);

		// pack(): el diàlegPren la mida del contingut real. Amb
		// setPreferredSize solament, Swing pot deixar-lo a la mida mínima.
		pack();
		setMinimumSize(new Dimension(560, 520));
		setSize(Math.max(getWidth(), 680), Math.max(getHeight(), 700));
		setLocationRelativeTo(getOwner());
	}

	/**
	 * Crea i registra tots els camps del formulari.
	 *
	 * <p>Els camps es comparteixen entre el diàleg de llibre nou i el
	 * d'edició: el registre {@link RegistreCampsFormulari} en manté les
	 * referències per validar-les i emplenar-les.
	 */
	private void creaCamps() {
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
		comboFormat.setModel(new javax.swing.DefaultComboBoxModel<>(FormatOptions.withBlank()));
		UIComponents.styleCombo(comboFormat);
		comboEstat     = reg.comboBox("comboEstat");
		comboEstat.setModel(new javax.swing.DefaultComboBoxModel<>(EstatOptions.withBlank()));
		UIComponents.styleCombo(comboEstat);
		chckDesitjat   = reg.comprovarBox("chckDesitjat");
		chckDesitjat.setBackground(UITheme.palette().bgPanel());
		chckDesitjat.setHorizontalAlignment(SwingConstants.LEFT);
		chckLlegit     = reg.comprovarBox("chckLlegit");
		chckLlegit.setBackground(UITheme.palette().bgPanel());
		chckLlegit.setHorizontalAlignment(SwingConstants.LEFT);

		textNotes      = new JTextArea(6, 20);
		textNotes.setLineWrap(true);
		textNotes.setWrapStyleWord(true);
		textNotes.setBackground(UITheme.palette().fieldBg());
		textNotes.setForeground(UITheme.palette().textDark());
		textNotes.setFont(UITheme.fontBase());
		textNotes.setCaretColor(UITheme.palette().accent());
		textNotes.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
	}

	/**
	 * Capçalera del diàleg.
	 *
	 * <p>Una sola línia amb el títol i, a la dreta, la cerca de metadades.
	 * La cerca és una acció de primer nivell: la majoria de llibres nous
	 * s'identifiquen per ISBN.
	 */
	private JPanel buildCapcalera() {
		JPanel cap = new JPanel(new BorderLayout(Tokens.SPACE_4, 0));
		cap.setBackground(UITheme.palette().bgPanel());
		cap.setBorder(new EmptyBorder(Tokens.SPACE_4, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_5));

		JLabel titol = new JLabel(I18n.t("modal_new_book"));
		titol.setFont(UITheme.fontTitle().deriveFont((float) (Tokens.FONT_SIZE_TITLE - 4)));
		titol.setForeground(UITheme.palette().textDark());
		cap.add(titol, BorderLayout.WEST);

		btnCercaInternet = new JButton(I18n.t("btn_cerca_internet"));
		btnCercaInternet.setIcon(Icones.icon("cerca", Tokens.ICON_SIZE, UITheme.palette().textOnAccent()));
		btnCercaInternet.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		UIComponents.styleSecondaryButton(btnCercaInternet);
		UIComponents.setAlçadaControl(btnCercaInternet, Tokens.CONTROL_HEIGHT);
		btnCercaInternet.setToolTipText(I18n.t("tip_cerca_internet"));
		JPanel dret = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		dret.setBackground(UITheme.palette().bgPanel());
		dret.add(btnCercaInternet);
		cap.add(dret, BorderLayout.EAST);
		return cap;
	}

	/**
	 * Cos del diàleg: camps principals i pestanyes de metadades.
	 *
	 * <p>El contingut llarg està dins d'un desplaçable, però les accions
	 * principals no: el peu queda fora del desplaçament.
	 */
	private JPanel buildCos() {
		JPanel panell = new JPanel(new BorderLayout(0, 0));
		panell.setBackground(UITheme.palette().bgPanel());

		JPanel form = new JPanel();
		form.setBackground(UITheme.palette().bgPanel());
		form.setBorder(new EmptyBorder(Tokens.SPACE_2, Tokens.SPACE_5, 0, Tokens.SPACE_5));

		ConstructorGraellaFormulari builder = ConstructorGraellaFormulari.columnsOf(form);
		builder.separador(I18n.t("form_section_identity"));
		builder.afegirField(I18n.t("field_isbn"), textISBN);
		builder.afegirField(I18n.t("field_title"), textNom);
		builder.afegirField(I18n.t("field_author"), textAutor);
		builder.separador(I18n.t("form_section_cover"));
		builder.componentPle(buildPortada());
		builder.componentPle(buildPestanyes());

		JScrollPane scroll = new JScrollPane(form,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());
		panell.add(scroll, BorderLayout.CENTER);
		return panell;
	}

	/** Previsualització de la portada i accions per triar-ne una. */
	private JPanel buildPortada() {
		JPanel fila = new JPanel(new BorderLayout(Tokens.SPACE_3, 0));
		fila.setBackground(UITheme.palette().bgPanel());
		fila.setOpaque(false);

		labelPreview = new JLabel();
		labelPreview.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		labelPreview.setHorizontalAlignment(SwingConstants.CENTER);
		labelPreview.setVerticalAlignment(SwingConstants.CENTER);
		labelPreview.setPreferredSize(new Dimension(110, 150));
		labelPreview.setMinimumSize(new Dimension(110, 150));
		labelPreview.setMaximumSize(new Dimension(110, 150));
		labelPreview.setOpaque(true);
		labelPreview.setBackground(UITheme.palette().bgSunken());
		fila.add(labelPreview, BorderLayout.WEST);

		JPanel dreta = new JPanel(new GridBagLayout());
		dreta.setBackground(UITheme.palette().bgPanel());
		dreta.setOpaque(false);

		btnSeleccionarImatge = new JButton(I18n.t("btn_sel_imatge"));
		btnSeleccionarImatge.setIcon(Icones.icon("importar", Tokens.ICON_SIZE, UITheme.palette().textDark()));
		btnSeleccionarImatge.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		UIComponents.styleSecondaryButton(btnSeleccionarImatge);

		JPanel filaBotons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		filaBotons.setBackground(UITheme.palette().bgPanel());
		filaBotons.setOpaque(false);
		filaBotons.add(btnSeleccionarImatge);

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0;
		gc.weightx = 1; gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.NORTHWEST;
		gc.insets = new Insets(0, 0, Tokens.SPACE_2, 0);
		dreta.add(filaBotons, gc);

		// El camp del camí és d'una línia: no ha d'absorbir l'alçada
		// sobrant de la columna, només s'hi estén.
		gc.gridy = 1;
		gc.weighty = 0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(0, 0, 0, 0);
		dreta.add(textPortada, gc);

		fila.add(dreta, BorderLayout.CENTER);
		return fila;
	}

	/** Pestanyes amb les metadades secundàries. */
	private JComponent buildPestanyes() {
		JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(UITheme.fontBase());

		JPanel detalls = new JPanel();
		detalls.setBackground(UITheme.palette().bgPanel());
		ConstructorGraellaFormulari b1 = ConstructorGraellaFormulari.columnsOf(detalls);
		b1.afegirField(I18n.t("field_year"), textAny);
		b1.afegirField(I18n.t("field_rating"), textValoracio);
		b1.afegirField(I18n.t("field_price"), textPreu);
		b1.afegirCombo(I18n.t("field_format"), comboFormat);
		b1.afegirCombo(I18n.t("field_estat"), comboEstat);
		b1.afegirField(I18n.t("field_exemplars"), textExemplars);
		b1.afegirField(I18n.t("field_country"), textPaisOrigen);
		b1.afegirCheck(I18n.t("field_wishlist"), chckDesitjat);
		b1.afegirCheck(I18n.t("field_read"), chckLlegit);
		tabs.addTab(I18n.t("form_tab_details"), detalls);

		JPanel publicacio = new JPanel();
		publicacio.setBackground(UITheme.palette().bgPanel());
		ConstructorGraellaFormulari b2 = ConstructorGraellaFormulari.columnsOf(publicacio);
		b2.afegirField(I18n.t("field_publisher"), textEditorial);
		b2.afegirField(I18n.t("field_series"), textSerie);
		b2.afegirField(I18n.t("field_volume"), textVolum);
		b2.afegirField(I18n.t("field_language"), textIdioma);
		b2.afegirField(I18n.t("field_purchased"), textDataCompra);
		b2.afegirField(I18n.t("field_read_on"), textDataLectura);
		tabs.addTab(I18n.t("form_tab_publication"), publicacio);

		JPanel notes = new JPanel(new BorderLayout(0, 0));
		notes.setBackground(UITheme.palette().bgPanel());
		JPanel notesForm = new JPanel();
		notesForm.setBackground(UITheme.palette().bgPanel());
		ConstructorGraellaFormulari b3 = ConstructorGraellaFormulari.columnsOf(notesForm);
		b3.afegirField(I18n.t("field_description"), textDescripcio);
		notes.add(notesForm, BorderLayout.NORTH);
		notes.add(new JScrollPane(textNotes), BorderLayout.CENTER);
		tabs.addTab(I18n.t("form_tab_notes"), notes);

		JPanel titols = new JPanel();
		titols.setBackground(UITheme.palette().bgPanel());
		ConstructorGraellaFormulari b4 = ConstructorGraellaFormulari.columnsOf(titols);
		b4.afegirField(I18n.t("field_title_ca"), textNomCa);
		b4.afegirField(I18n.t("field_title_es"), textNomEs);
		b4.afegirField(I18n.t("field_title_en"), textNomEn);
		tabs.addTab(I18n.t("form_tab_titles"), titols);

		return tabs;
	}

	/**
	 * Peu fix del diàleg.
	 *
	 * <p>Les accions principals queden fora de la zona desplaçable: sempre
	 * són visibles, encara que el contingut sigui llarg. El formulari
	 * permet entrada manual: la cerca a Internet és una opció, no un pas
	 * obligatori.
	 */
	private JPanel buildPeu() {
		JPanel peu = new JPanel(new BorderLayout(0, Tokens.SPACE_2));
		peu.setBackground(UITheme.palette().bgPanel());
		peu.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.palette().borderClr()),
				new EmptyBorder(Tokens.SPACE_3, Tokens.SPACE_5, Tokens.SPACE_3, Tokens.SPACE_5)));

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setPreferredSize(new Dimension(0, 6));
		progressBar.setVisible(false);
		peu.add(progressBar, BorderLayout.NORTH);

		JButton btnCancel = new JButton(I18n.t("btn_cancel"));
		UIComponents.styleSecondaryButton(btnCancel);
		btnCancel.addActionListener(e -> dispose());

		btnGuardar = new JButton(I18n.t("btn_save_java"));
		UIComponents.styleAccentButton(btnGuardar);

		JPanel accions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Tokens.SPACE_2, 0));
		accions.setBackground(UITheme.palette().bgPanel());
		UIComponents.setAlçadaControl(btnCancel, Tokens.CONTROL_HEIGHT);
		UIComponents.setAlçadaControl(btnGuardar, Tokens.CONTROL_HEIGHT);
		accions.add(btnCancel);
		accions.add(btnGuardar);
		peu.add(accions, BorderLayout.CENTER);
		return peu;
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

	public void omplir(Llibre l) {
		textNom.setText(l.obtenirNom() != null ? l.obtenirNom() : "");
		textAutor.setText(l.obtenirAutor() != null ? l.obtenirAutor() : "");
		textAny.setText(l.obtenirAny() != null && l.obtenirAny() != 0 ? String.valueOf(l.obtenirAny()) : "");
		textDescripcio.setText(l.obtenirDescripcio() != null ? l.obtenirDescripcio() : "");
		textValoracio.setText(l.obtenirValoracio() != null && l.obtenirValoracio() != 0.0 ? String.valueOf(l.obtenirValoracio()) : "");
		textPreu.setText(l.obtenirPreu() != null && l.obtenirPreu() != 0.0 ? String.valueOf(l.obtenirPreu()) : "");
		textEditorial.setText(l.obtenirEditorial());
		textSerie.setText(l.obtenirSerie());
		textVolum.setText(l.obtenirVolum() > 0 ? String.valueOf(l.obtenirVolum()) : "");
		textIdioma.setText(l.obtenirIdioma() != null ? l.obtenirIdioma() : "");
		chckLlegit.setSelected(Boolean.TRUE.equals(l.obtenirLlegit()));
		chckDesitjat.setSelected(l.esDesitjat());
		textISBN.setText("");
	}
}
