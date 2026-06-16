package presentacio.detalles.vista;



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

import herramienta.I18n;
import herramienta.UITheme;

public class DetallesLlibrePanel extends JDialog {

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
	private JComboBox<String> comboEstat;
	private JTextField textExemplars;
	private JTextField textLlenguaOriginal;
	private JCheckBox  chckDesitjat;
	private JTextField textPagines;
	private JTextField textPaginesLlegides;
	private JTextField textNomCa;
	private JTextField textNomEs;
	private JTextField textNomEn;
	private JCheckBox  chckLlegit;
	private JTextArea  textNotes;
	private JButton    btnEditar;
	private JButton    btnEliminar;
	private JButton    btnSeleccionarImatge;
	private JButton    btnGestioLlistes;
	private JButton    btnGestioTags;
	private JButton    btnHistorialPrestecs;
	private JButton    btnImprimir;

	private final List<JComponent> editableInputs;

	public DetallesLlibrePanel() {
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

		labelIcono = new JLabel("");
		labelIcono.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		labelIcono.setPreferredSize(new Dimension(IMG_SIZE, IMG_SIZE));
		labelIcono.setMaximumSize(new Dimension(SIDE_W - 14, IMG_SIZE));
		labelIcono.setAlignmentX(Component.LEFT_ALIGNMENT);
		east.add(labelIcono);
		east.add(Box.createVerticalStrut(6));

		btnSeleccionarImatge = new JButton(I18n.t("btn_sel_imatge_detail"));
		UIComponents.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setEnabled(false);

		btnEditar = new JButton(I18n.t("btn_edit_java"));
		UIComponents.styleAccentButton(btnEditar);

		btnEliminar = new JButton(I18n.t("btn_delete"));
		btnEliminar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnEliminar.setBackground(new Color(0xC0392B));
		btnEliminar.setForeground(Color.WHITE);
		btnEliminar.setFont(UITheme.fontBold());
		btnEliminar.setFocusPainted(false);
		btnEliminar.setBorderPainted(false);
		btnEliminar.setOpaque(true);
		btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JButton btnTancar = new JButton(I18n.t("btn_close"));
		UIComponents.styleSecondaryButton(btnTancar);
		btnTancar.addActionListener(e -> dispose());

		btnGestioLlistes = new JButton(I18n.t("btn_llistes"));
		UIComponents.styleSecondaryButton(btnGestioLlistes);
		btnGestioLlistes.setToolTipText(I18n.t("tip_llistes"));

		btnGestioTags = new JButton(I18n.t("btn_etiquetes"));
		UIComponents.styleSecondaryButton(btnGestioTags);
		btnGestioTags.setToolTipText(I18n.t("tip_etiquetes"));

		btnHistorialPrestecs = new JButton(I18n.t("btn_historial_prestecs"));
		UIComponents.styleSecondaryButton(btnHistorialPrestecs);
		btnHistorialPrestecs.setToolTipText(I18n.t("tip_historial_prestecs"));

		btnImprimir = new JButton(I18n.t("btn_imprimir_detail"));
		UIComponents.styleSecondaryButton(btnImprimir);
		btnImprimir.setToolTipText(I18n.t("tip_imprimir_detail"));

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
		// The DetallesGeneralTab/DetallesNotesTab/DetallesAvancatTab thin
		// facades were 9-line pass-throughs; inlined here per the tot.txt
		// LOW finding (the delegation added no value).
		tabbedPane.addTab(I18n.t("stats_tab_general"), buildGeneralTab());
		tabbedPane.addTab(I18n.t("field_notes"), buildNotesTab());
		tabbedPane.addTab(I18n.t("tab_advanced"), buildAdvancedTab());
		add(tabbedPane, BorderLayout.CENTER);

		editableInputs = List.of(
			textAny, textAutor, textDescripcio, textNom, textPortada, textPreu, textValoracio,
			textEditorial, textSerie, textVolum, textDataCompra, textDataLectura, textIdioma,
			textPaisOrigen, textExemplars, textLlenguaOriginal, textPagines, textPaginesLlegides,
			textNomCa, textNomEs, textNomEn,
			comboFormat, comboEstat, chckDesitjat, chckLlegit, textNotes);

		setSize(800, 680);
	}

	JScrollPane buildGeneralTab() {
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.palette().bgPanel());
		grid.setBorder(new EmptyBorder(8, 8, 4, 4));

		textISBN            = addFieldEntry(grid, I18n.t("field_isbn"));
		textNom             = addFieldEntry(grid, I18n.t("field_title"));
		textAutor           = addFieldEntry(grid, I18n.t("field_author"));
		textAny             = addFieldEntry(grid, I18n.t("field_year"));
		textDescripcio      = addFieldEntry(grid, I18n.t("field_description"));
		textValoracio       = addFieldEntry(grid, I18n.t("field_rating"));
		textPreu            = addFieldEntry(grid, I18n.t("field_price"));
		textEditorial       = addFieldEntry(grid, I18n.t("field_publisher"));
		textSerie           = addFieldEntry(grid, I18n.t("field_series"));
		textVolum           = addFieldEntry(grid, I18n.t("field_volume"));
		textIdioma          = addFieldEntry(grid, I18n.t("field_language"));
		textPaisOrigen      = addFieldEntry(grid, I18n.t("field_country"));
		comboFormat         = addComboEntry(grid, I18n.t("field_format"),
				herramienta.FormatOptions.withBlank());
		comboEstat          = addComboEntry(grid, I18n.t("field_estat"),
				new String[]{"", I18n.t("estat_nou"), I18n.t("estat_bo"), I18n.t("estat_usat"), I18n.t("estat_deteriorat")});
		chckDesitjat        = addCheckEntry(grid, I18n.t("field_wishlist"),
				I18n.t("tip_desitjat"));
		chckLlegit          = addCheckEntry(grid, I18n.t("field_read"), null);
		textPortada         = addFieldEntry(grid, I18n.t("col_cover"));

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UITheme.palette().bgPanel());
		wrapper.add(grid, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());

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

		textNotes = new JTextArea(20, 40);
		textNotes.setLineWrap(true);
		textNotes.setWrapStyleWord(true);
		textNotes.setEnabled(false);
		textNotes.setFont(UITheme.fontBase());
		textNotes.setBackground(UITheme.palette().bgMain());
		textNotes.setForeground(UITheme.palette().textDark());

		JScrollPane notesScroll = new JScrollPane(textNotes);
		notesScroll.setBorder(BorderFactory.createLineBorder(UITheme.palette().borderClr()));
		panel.add(notesScroll, BorderLayout.CENTER);

		return panel;
	}

	JScrollPane buildAdvancedTab() {
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setBackground(UITheme.palette().bgPanel());
		grid.setBorder(new EmptyBorder(8, 8, 4, 4));

		textDataCompra      = addFieldEntry(grid, I18n.t("field_purchased"));
		textDataLectura     = addFieldEntry(grid, I18n.t("field_read_on"));
		textExemplars       = addFieldEntry(grid, I18n.t("field_exemplars"));
		textLlenguaOriginal = addFieldEntry(grid, I18n.t("field_llengua_original"));
		textPagines         = addFieldEntry(grid, I18n.t("field_pages"));
		textPaginesLlegides = addFieldEntry(grid, I18n.t("field_pages_read"));
		textNomCa           = addFieldEntry(grid, I18n.t("field_title_ca"));
		textNomEs           = addFieldEntry(grid, I18n.t("field_title_es"));
		textNomEn           = addFieldEntry(grid, I18n.t("field_title_en"));

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UITheme.palette().bgPanel());
		wrapper.add(grid, BorderLayout.NORTH);

		JScrollPane scroll = new JScrollPane(wrapper,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(UITheme.palette().bgPanel());

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

		return scroll;
	}

	JTextField addFieldEntry(JPanel grid, String label) {
		JPanel entry = entryPanel();
		JLabel lbl = makeLabel(label);
		entry.add(lbl, BorderLayout.WEST);
		JTextField field = new JTextField();
		field.setEnabled(false);
		field.setColumns(10);
		UIComponents.styleField(field);
		// Link the label to the field for screen readers and keyboard
		// navigation. The previous implementation also stored the
		// pair in a FormFieldRegistry, but no caller ever queried
		// the registry (see tot.txt MEDIUM finding) — the storage
		// was dead. The linkLabel call has visible behavior on its
		// own, so keep it.
		lbl.setLabelFor(field);
		entry.add(field, BorderLayout.CENTER);
		grid.add(entry);
		return field;
	}

	JComboBox<String> addComboEntry(JPanel grid, String label, String[] items) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JComboBox<String> combo = new JComboBox<>(items);
		combo.setEnabled(false);
		combo.setBackground(UITheme.palette().bgMain());
		combo.setForeground(UITheme.palette().textDark());
		combo.setFont(UITheme.fontBase());
		entry.add(combo, BorderLayout.CENTER);
		grid.add(entry);
		return combo;
	}

	JCheckBox addCheckEntry(JPanel grid, String label, String tooltip) {
		JPanel entry = entryPanel();
		entry.add(makeLabel(label), BorderLayout.WEST);
		JCheckBox chk = new JCheckBox("");
		chk.setEnabled(false);
		chk.setBackground(UITheme.palette().bgPanel());
		chk.setHorizontalAlignment(SwingConstants.LEFT);
		if (tooltip != null) chk.setToolTipText(tooltip);
		entry.add(chk, BorderLayout.CENTER);
		grid.add(entry);
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

	public JLabel     getLabelIcono()          { return labelIcono; }
	public JButton    getBtnSeleccionarImatge() { return btnSeleccionarImatge; }
	public List<JComponent> getEditableInputs() { return editableInputs; }
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
	public JComboBox<String> getComboEstat()    { return comboEstat; }
	public JTextField getTextExemplars()        { return textExemplars; }
	public JTextField getTextLlenguaOriginal()  { return textLlenguaOriginal; }
	public JCheckBox  getChckDesitjat()         { return chckDesitjat; }
	public JTextField getTextPagines()          { return textPagines; }
	public JTextField getTextPaginesLlegides()  { return textPaginesLlegides; }
	public JTextField getTextNomCa()            { return textNomCa; }
	public JTextField getTextNomEs()            { return textNomEs; }
	public JTextField getTextNomEn()            { return textNomEn; }
	public JCheckBox  getChckLlegit()           { return chckLlegit; }
	public JTextArea  getTextNotes()            { return textNotes; }
	public JButton    getBtnGestioLlistes()      { return btnGestioLlistes; }
	public JButton    getBtnGestioTags()         { return btnGestioTags; }
	public JButton    getBtnHistorialPrestecs()  { return btnHistorialPrestecs; }
	public JButton    getBtnImprimir()           { return btnImprimir; }
}