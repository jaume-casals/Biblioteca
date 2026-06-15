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

		textISBN       = new JTextField(); textISBN.setColumns(10); UIComponents.styleField(textISBN);
		textNom        = new JTextField(); textNom.setColumns(10); UIComponents.styleField(textNom);
		textAutor      = new JTextField(); textAutor.setColumns(10); UIComponents.styleField(textAutor);
		textAny        = new JTextField(); textAny.setColumns(10); UIComponents.styleField(textAny);
		textDescripcio = new JTextField(); textDescripcio.setColumns(10); UIComponents.styleField(textDescripcio);
		textValoracio  = new JTextField(); textValoracio.setColumns(10); UIComponents.styleField(textValoracio);
		textPreu       = new JTextField(); textPreu.setColumns(10); UIComponents.styleField(textPreu);
		textEditorial  = new JTextField(); textEditorial.setColumns(10); UIComponents.styleField(textEditorial);
		textSerie      = new JTextField(); textSerie.setColumns(10); UIComponents.styleField(textSerie);
		textVolum      = new JTextField(); textVolum.setColumns(10); UIComponents.styleField(textVolum);
		textDataCompra  = new JTextField(); textDataCompra.setColumns(10); UIComponents.styleField(textDataCompra);
		textDataLectura = new JTextField(); textDataLectura.setColumns(10); UIComponents.styleField(textDataLectura);
		textDataCompra.setToolTipText("YYYY-MM-DD");
		textDataLectura.setToolTipText("YYYY-MM-DD");
		textIdioma     = new JTextField(); textIdioma.setColumns(10); UIComponents.styleField(textIdioma);
		comboFormat    = new JComboBox<>(herramienta.FormatOptions.withBlank());
		comboFormat.setBackground(UITheme.palette().bgMain());
		comboFormat.setForeground(UITheme.palette().textDark());
		comboFormat.setFont(UITheme.fontBase());
		chckDesitjat   = new JCheckBox("");
		chckDesitjat.setBackground(UITheme.palette().bgPanel());
		chckDesitjat.setHorizontalAlignment(SwingConstants.LEFT);
		chckDesitjat.setToolTipText(I18n.t("tip_desitjat"));
		chckLlegit     = new JCheckBox("");
		chckLlegit.setBackground(UITheme.palette().bgPanel());
		chckLlegit.setHorizontalAlignment(SwingConstants.LEFT);
		textNotes      = new JTextArea(4, 20); textNotes.setLineWrap(true); textNotes.setWrapStyleWord(true);
		textNotes.setBackground(UITheme.palette().bgMain());
		textNotes.setForeground(UITheme.palette().textDark());
		textNotes.setFont(UITheme.fontBase());
		textNotes.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		textPortada    = new JTextField(); textPortada.setColumns(10); UIComponents.styleField(textPortada);
		textPaisOrigen = new JTextField(); textPaisOrigen.setColumns(10); UIComponents.styleField(textPaisOrigen);
		comboEstat     = new JComboBox<>(new String[]{"", I18n.t("estat_nou"), I18n.t("estat_bo"), I18n.t("estat_usat"), I18n.t("estat_deteriorat")});
		comboEstat.setBackground(UITheme.palette().bgMain());
		comboEstat.setForeground(UITheme.palette().textDark());
		comboEstat.setFont(UITheme.fontBase());
		textExemplars  = new JTextField(); textExemplars.setColumns(10); UIComponents.styleField(textExemplars);
		textNomCa      = new JTextField(); textNomCa.setColumns(10); UIComponents.styleField(textNomCa);
		textNomEs      = new JTextField(); textNomEs.setColumns(10); UIComponents.styleField(textNomEs);
		textNomEn      = new JTextField(); textNomEn.setColumns(10); UIComponents.styleField(textNomEn);

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
