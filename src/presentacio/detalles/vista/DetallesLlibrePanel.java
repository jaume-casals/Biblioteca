package presentacio.detalles.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import herramienta.UITheme;

public class DetallesLlibrePanel extends JDialog {

	// Min width of one label+field cell; drives column-count calculation
	private static final int ENTRY_MIN_W = 260;
	private static final int ENTRY_H     = 44;
	private static final int LBL_W       = 92;
	private static final int SIDE_W      = 215;
	private static final int IMG_SIZE    = 185;

	private JLabel     labelIcono;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textPortada;
	private JTextField textEditorial;
	private JTextField textSerie;
	private JTextField textVolum;
	private JTextField textDataCompra;
	private JTextField textDataLectura;
	private JTextField textIdioma;
	private JTextField textPaisOrigen;
	private JComboBox<String> comboFormat;
	private JCheckBox  chckDesitjat;
	private JTextField textPagines;
	private JTextField textPaginesLlegides;
	private JCheckBox  chckLlegit;
	private JTextArea  textNotes;
	private JButton    btnEditar;
	private JButton    btnEliminar;
	private JButton    btnSeleccionarImatge;
	private JButton    btnGestioLlistes;
	private JButton    btnGestioTags;

	public DetallesLlibrePanel() {
		setModal(true);
		setResizable(true);
		setMinimumSize(new Dimension(480, 480));
		setPreferredSize(new Dimension(800, 680));
		setLayout(new BorderLayout(0, 0));
		getContentPane().setBackground(UITheme.BG_PANEL);

		// ── EAST: image + action buttons ──────────────────────────────────────
		JPanel east = new JPanel();
		east.setBackground(UITheme.BG_PANEL);
		east.setPreferredSize(new Dimension(SIDE_W, 0));
		east.setMinimumSize(new Dimension(SIDE_W, 0));
		east.setMaximumSize(new Dimension(SIDE_W, Integer.MAX_VALUE));
		east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
		east.setBorder(new EmptyBorder(8, 6, 8, 8));

		labelIcono = new JLabel("");
		labelIcono.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		labelIcono.setPreferredSize(new Dimension(IMG_SIZE, IMG_SIZE));
		labelIcono.setMaximumSize(new Dimension(SIDE_W - 14, IMG_SIZE));
		labelIcono.setAlignmentX(Component.LEFT_ALIGNMENT);
		east.add(labelIcono);
		east.add(Box.createVerticalStrut(6));

		btnSeleccionarImatge = new JButton("Seleccionar imatge");
		UITheme.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setEnabled(false);

		btnEditar = new JButton("Editar");
		UITheme.styleAccentButton(btnEditar);

		btnEliminar = new JButton("Eliminar");
		btnEliminar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnEliminar.setBackground(new Color(0xC0392B));
		btnEliminar.setForeground(Color.WHITE);
		btnEliminar.setFont(UITheme.FONT_BOLD);
		btnEliminar.setFocusPainted(false);
		btnEliminar.setBorderPainted(false);
		btnEliminar.setOpaque(true);
		btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JButton btnTancar = new JButton("Tancar");
		UITheme.styleSecondaryButton(btnTancar);
		btnTancar.addActionListener(e -> dispose());

		btnGestioLlistes = new JButton("Llistes...");
		UITheme.styleSecondaryButton(btnGestioLlistes);
		btnGestioLlistes.setToolTipText("Gestionar l'assignació d'aquest llibre a llistes");

		btnGestioTags = new JButton("Etiquetes...");
		UITheme.styleSecondaryButton(btnGestioTags);
		btnGestioTags.setToolTipText("Gestionar les etiquetes/gèneres d'aquest llibre");

		int btnH = 36;
		for (JButton btn : new JButton[]{
				btnSeleccionarImatge, btnEditar, btnEliminar,
				btnTancar, btnGestioLlistes, btnGestioTags}) {
			btn.setAlignmentX(Component.LEFT_ALIGNMENT);
			btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btnH));
			east.add(btn);
			east.add(Box.createVerticalStrut(4));
		}

		add(east, BorderLayout.EAST);

		// ── CENTER: responsive field grid + notes ─────────────────────────────
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.BG_PANEL);
		grid.setBorder(new EmptyBorder(8, 8, 4, 4));

		textISBN            = addFieldEntry(grid, "ISBN");
		textNom             = addFieldEntry(grid, "NOM");
		textAutor           = addFieldEntry(grid, "AUTOR");
		textAny             = addFieldEntry(grid, "ANY");
		textDescripcio      = addFieldEntry(grid, "DESCRIPCIO");
		textValoracio       = addFieldEntry(grid, "VALORACIO");
		textPreu            = addFieldEntry(grid, "PREU");
		textEditorial       = addFieldEntry(grid, "EDITORIAL");
		textSerie           = addFieldEntry(grid, "SÈRIE");
		textVolum           = addFieldEntry(grid, "VOLUM");
		textDataCompra      = addFieldEntry(grid, "DATA COMP.");
		textDataLectura     = addFieldEntry(grid, "DATA LECT.");
		textIdioma          = addFieldEntry(grid, "IDIOMA");
		textPaisOrigen      = addFieldEntry(grid, "PAÍS ORIGEN");
		comboFormat         = addComboEntry(grid, "FORMAT",
				new String[]{"", "Tapa dura", "Tapa blanda", "eBook", "Audiollibre"});
		chckDesitjat        = addCheckEntry(grid, "DESITJAT",
				"Vull comprar aquest llibre (no el tinc)");
		chckLlegit          = addCheckEntry(grid, "LLEGIT", null);
		textPortada         = addFieldEntry(grid, "PORTADA");
		textPagines         = addFieldEntry(grid, "PAGINES");
		textPaginesLlegides = addFieldEntry(grid, "PG. LLEGIDES");

		// Notes: full-width panel pinned to SOUTH
		JPanel notesRow = new JPanel(new BorderLayout(4, 0));
		notesRow.setBackground(UITheme.BG_PANEL);
		notesRow.setBorder(new EmptyBorder(0, 8, 8, 4));
		JLabel lblNotes = makeLabel("NOTES");
		lblNotes.setVerticalAlignment(SwingConstants.TOP);
		lblNotes.setPreferredSize(new Dimension(LBL_W, 0));
		notesRow.add(lblNotes, BorderLayout.WEST);
		textNotes = new JTextArea(4, 0);
		textNotes.setLineWrap(true);
		textNotes.setWrapStyleWord(true);
		textNotes.setEnabled(false);
		textNotes.setFont(UITheme.FONT_BASE);
		textNotes.setBackground(UITheme.BG_MAIN);
		textNotes.setForeground(UITheme.TEXT_DARK);
		JScrollPane notesScroll = new JScrollPane(textNotes);
		notesScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		notesRow.add(notesScroll, BorderLayout.CENTER);

		JPanel center = new JPanel(new BorderLayout(0, 4));
		center.setBackground(UITheme.BG_PANEL);
		center.add(grid, BorderLayout.CENTER);
		center.add(notesRow, BorderLayout.SOUTH);

		JScrollPane scroll = new JScrollPane(center,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.BG_PANEL);
		add(scroll, BorderLayout.CENTER);

		// Recompute column count based on viewport width (authoritative when shrinking)
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

		setSize(800, 680);
	}

	// ── entry builders ────────────────────────────────────────────────────────

	private JTextField addFieldEntry(JPanel grid, String label) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JTextField field = new JTextField();
		field.setEnabled(false);
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
		combo.setEnabled(false);
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
		chk.setEnabled(false);
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

	// ── getters (used by DetallesLlibrePanelControl) ─────────────────────────

	public JLabel     getLabelIcono()          { return labelIcono; }
	public JButton    getBtnSeleccionarImatge() { return btnSeleccionarImatge; }
	public JButton    getBtnEliminar()          { return btnEliminar; }
	public JButton    getBtnEditar()            { return btnEditar; }
	public JTextField getTextISBN()             { return textISBN; }
	public JTextField getTextNom()              { return textNom; }
	public JTextField getTextAutor()            { return textAutor; }
	public JTextField getTextAny()              { return textAny; }
	public JTextField getTextDescripcio()       { return textDescripcio; }
	public JTextField getTextValoracio()        { return textValoracio; }
	public JTextField getTextPreu()             { return textPreu; }
	public JTextField getTextPortada()          { return textPortada; }
	public JTextField getTextEditorial()        { return textEditorial; }
	public JTextField getTextSerie()            { return textSerie; }
	public JTextField getTextVolum()            { return textVolum; }
	public JTextField getTextDataCompra()       { return textDataCompra; }
	public JTextField getTextDataLectura()      { return textDataLectura; }
	public JTextField getTextIdioma()           { return textIdioma; }
	public JTextField getTextPaisOrigen()       { return textPaisOrigen; }
	public JComboBox<String> getComboFormat()   { return comboFormat; }
	public JCheckBox  getChckDesitjat()         { return chckDesitjat; }
	public JTextField getTextPagines()          { return textPagines; }
	public JTextField getTextPaginesLlegides()  { return textPaginesLlegides; }
	public JCheckBox  getChckLlegit()           { return chckLlegit; }
	public JTextArea  getTextNotes()            { return textNotes; }
	public JButton    getBtnGestioLlistes()     { return btnGestioLlistes; }
	public JButton    getBtnGestioTags()        { return btnGestioTags; }
}
