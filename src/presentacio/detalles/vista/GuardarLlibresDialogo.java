package presentacio.detalles.vista;

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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import herramienta.I18n;
import herramienta.UITheme;

public class GuardarLlibresDialogo extends JDialog {

	private static final int ENTRY_MIN_W = 240;
	private static final int ENTRY_H     = 42;
	private static final int LBL_W       = 90;
	private static final int SIDE_W      = 155;
	private static final int IMG_SIZE    = 135;

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
	private JProgressBar progressBar;

	public GuardarLlibresDialogo() {
		setModal(true);
		setResizable(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle(I18n.t("modal_new_book"));
		setMinimumSize(new Dimension(400, 500));
		setPreferredSize(new Dimension(600, 720));
		setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(UITheme.BG_PANEL);

		// ── WEST: preview + action buttons ───────────────────────────────────
		JPanel west = new JPanel();
		west.setBackground(UITheme.BG_PANEL);
		west.setPreferredSize(new Dimension(SIDE_W, 0));
		west.setMinimumSize(new Dimension(SIDE_W, 0));
		west.setMaximumSize(new Dimension(SIDE_W, Integer.MAX_VALUE));
		west.setLayout(new BoxLayout(west, BoxLayout.Y_AXIS));
		west.setBorder(new EmptyBorder(8, 8, 8, 6));

		labelPreview = new JLabel();
		labelPreview.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		labelPreview.setHorizontalAlignment(SwingConstants.CENTER);
		labelPreview.setPreferredSize(new Dimension(IMG_SIZE, IMG_SIZE));
		labelPreview.setMinimumSize(new Dimension(IMG_SIZE, IMG_SIZE));
		labelPreview.setMaximumSize(new Dimension(SIDE_W - 16, IMG_SIZE));
		labelPreview.setAlignmentX(Component.LEFT_ALIGNMENT);
		west.add(labelPreview);
		west.add(Box.createVerticalStrut(6));

		btnGuardar = new JButton(I18n.t("btn_save_java"));
		UITheme.styleAccentButton(btnGuardar);

		JButton btnCancel = new JButton(I18n.t("btn_cancel"));
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.addActionListener(e -> dispose());

		btnSeleccionarImatge = new JButton(I18n.t("btn_sel_imatge"));
		UITheme.styleSecondaryButton(btnSeleccionarImatge);

		for (JButton btn : new JButton[]{btnGuardar, btnCancel, btnSeleccionarImatge}) {
			btn.setAlignmentX(Component.LEFT_ALIGNMENT);
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
			west.add(btn);
			west.add(Box.createVerticalStrut(4));
		}

		add(west, BorderLayout.WEST);

		// ── CENTER: responsive field grid ────────────────────────────────────
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.BG_PANEL);
		grid.setBorder(new EmptyBorder(8, 4, 4, 8));

		textISBN       = addFieldEntry(grid, I18n.t("field_isbn"));
		textNom        = addFieldEntry(grid, I18n.t("field_title"));
		textAutor      = addFieldEntry(grid, I18n.t("field_author"));
		textAny        = addFieldEntry(grid, I18n.t("field_year"));
		textDescripcio = addFieldEntry(grid, I18n.t("field_description"));
		textValoracio  = addFieldEntry(grid, I18n.t("field_rating"));
		textPreu       = addFieldEntry(grid, I18n.t("field_price"));
		textEditorial  = addFieldEntry(grid, I18n.t("field_publisher"));
		textSerie      = addFieldEntry(grid, I18n.t("field_series"));
		textVolum      = addFieldEntry(grid, I18n.t("field_volume"));
		textDataCompra  = addFieldEntry(grid, I18n.t("field_purchased"));
		textDataLectura = addFieldEntry(grid, I18n.t("field_read_on"));
		textDataCompra.setToolTipText("YYYY-MM-DD");
		textDataLectura.setToolTipText("YYYY-MM-DD");
		textIdioma     = addFieldEntry(grid, I18n.t("field_language"));
		comboFormat    = addComboEntry(grid, I18n.t("field_format"),
				new String[]{"", I18n.t("fmt_hardcover"), I18n.t("fmt_softcover"), I18n.t("fmt_ebook"), I18n.t("fmt_audiobook")});
		chckDesitjat   = addCheckEntry(grid, I18n.t("field_wishlist"),
				I18n.t("tip_desitjat"));
		chckLlegit     = addCheckEntry(grid, I18n.t("field_read"), null);
		textPortada    = addFieldEntry(grid, I18n.t("col_cover"));

		JScrollPane scroll = new JScrollPane(grid,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.BG_PANEL);
		add(scroll, BorderLayout.CENTER);

		// Viewport-driven column reflow (same pattern as DetallesLlibrePanel)
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

		// ── SOUTH: internet search button + progress bar ──────────────────────
		JPanel south = new JPanel(new BorderLayout(0, 2));
		south.setBackground(UITheme.BG_PANEL);
		south.setBorder(new EmptyBorder(0, 8, 8, 8));

		btnCercaInternet = new JButton(I18n.t("btn_cerca_internet"));
		UITheme.styleAccentButton(btnCercaInternet);
		btnCercaInternet.setBackground(new java.awt.Color(0x117A65));
		btnCercaInternet.setForeground(java.awt.Color.WHITE);
		btnCercaInternet.setToolTipText(I18n.t("tip_cerca_internet"));
		south.add(btnCercaInternet, BorderLayout.CENTER);

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setPreferredSize(new Dimension(0, 8));
		progressBar.setVisible(false);
		south.add(progressBar, BorderLayout.SOUTH);

		add(south, BorderLayout.SOUTH);

		setSize(600, 720);
	}

	// ── entry builders ────────────────────────────────────────────────────────

	private JTextField addFieldEntry(JPanel grid, String label) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JTextField field = new JTextField();
		field.setColumns(10);
		UITheme.styleField(field);
		entry.add(field, BorderLayout.CENTER);
		grid.add(entry);
		return field;
	}

	private JComboBox<String> addComboEntry(JPanel grid, String label, String[] items) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JComboBox<String> combo = new JComboBox<>(items);
		combo.setBackground(UITheme.BG_MAIN);
		combo.setForeground(UITheme.TEXT_DARK);
		combo.setFont(UITheme.FONT_BASE);
		entry.add(combo, BorderLayout.CENTER);
		grid.add(entry);
		return combo;
	}

	private JCheckBox addCheckEntry(JPanel grid, String label, String tooltip) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JCheckBox chk = new JCheckBox("");
		chk.setBackground(UITheme.BG_PANEL);
		chk.setHorizontalAlignment(SwingConstants.LEFT);
		if (tooltip != null) chk.setToolTipText(tooltip);
		entry.add(chk, BorderLayout.CENTER);
		grid.add(entry);
		return chk;
	}

	private JPanel entryPanel() {
		JPanel p = new JPanel(new BorderLayout(4, 0));
		p.setBackground(UITheme.BG_PANEL);
		p.setBorder(new EmptyBorder(2, 2, 2, 2));
		p.setPreferredSize(new Dimension(ENTRY_MIN_W, ENTRY_H));
		return p;
	}

	private JLabel makeLabel(String text) {
		JLabel lbl = new JLabel(text);
		UITheme.styleLabel(lbl);
		lbl.setPreferredSize(new Dimension(LBL_W, 0));
		return lbl;
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
}
