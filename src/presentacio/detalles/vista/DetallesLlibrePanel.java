package presentacio.detalles.vista;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import herramienta.UITheme;

public class DetallesLlibrePanel extends JDialog {

	private static final int DIALOG_W  = 600;
	private static final int DIALOG_H  = 720;
	private static final int LBL_X     = 31;
	private static final int LBL_W     = 83;
	private static final int FIELD_X   = 124;
	private static final int FIELD_W   = 192;
	private static final int ROW_H     = 41;
	private static final int ROW_STEP  = 52;
	private static final int IMG_X     = 352;
	private static final int IMG_SIZE  = 200;

	private JLabel     labelIcono;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textPortada;
	private JTextField textPagines;
	private JTextField textPaginesLlegides;
	private JCheckBox  chckLlegit;
	private JTextArea  textNotes;
	private JButton    btnEditar;
	private JButton    btnEliminar;
	private JButton    btnSeleccionarImatge;
	private JButton    btnGestioLlistes;

	public DetallesLlibrePanel() {
		setModal(true);
		setResizable(false);
		setBounds(0, 0, DIALOG_W, DIALOG_H);

		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		panel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		panel.setLayout(null);
		panel.setBounds(0, 0, DIALOG_W, DIALOG_H);
		add(panel);

		// Cover image
		labelIcono = new JLabel("");
		labelIcono.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		labelIcono.setBounds(IMG_X, 10, IMG_SIZE, IMG_SIZE);
		panel.add(labelIcono);

		// Fields
		int row = 0;
		textISBN      = addRow(panel, "ISBN",      row++);
		textNom       = addRow(panel, "NOM",        row++);
		textAutor     = addRow(panel, "AUTOR",      row++);
		textAny       = addRow(panel, "ANY",        row++);
		textDescripcio = addRow(panel, "DESCRIPCIO", row++);
		textValoracio = addRow(panel, "VALORACIO",  row++);
		textPreu      = addRow(panel, "PREU",       row++);

		// Llegit checkbox (replaces a normal text field)
		JLabel lblLlegit = new JLabel("LLEGIT");
		UITheme.styleLabel(lblLlegit);
		lblLlegit.setBounds(LBL_X, y(row), LBL_W, ROW_H);
		panel.add(lblLlegit);
		chckLlegit = new JCheckBox("");
		chckLlegit.setEnabled(false);
		chckLlegit.setBackground(UITheme.BG_PANEL);
		chckLlegit.setHorizontalAlignment(SwingConstants.CENTER);
		chckLlegit.setBounds(FIELD_X, y(row), FIELD_W, ROW_H);
		panel.add(chckLlegit);
		row++;

		textPortada = addRow(panel, "PORTADA", row++);
		textPagines = addRow(panel, "PAGINES", row++);
		textPaginesLlegides = addRow(panel, "PG. LLEGIDES", row++);

		// Notes (multi-line)
		JLabel lblNotes = new JLabel("NOTES");
		UITheme.styleLabel(lblNotes);
		int notesY = y(row);
		lblNotes.setBounds(LBL_X, notesY, LBL_W, ROW_H);
		panel.add(lblNotes);
		textNotes = new JTextArea();
		textNotes.setLineWrap(true);
		textNotes.setWrapStyleWord(true);
		textNotes.setEnabled(false);
		textNotes.setFont(UITheme.FONT_BASE);
		textNotes.setBackground(UITheme.BG_MAIN);
		textNotes.setForeground(UITheme.TEXT_DARK);
		textNotes.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		JScrollPane notesScroll = new JScrollPane(textNotes);
		notesScroll.setBounds(FIELD_X, notesY, FIELD_W + 60, 90);
		notesScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		panel.add(notesScroll);

		// Buttons on the right column
		int btnW = 184;
		int btnX = IMG_X + IMG_SIZE - btnW;
		btnSeleccionarImatge = new JButton("Seleccionar imatge");
		UITheme.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setBounds(btnX, 220, btnW, 34);
		btnSeleccionarImatge.setEnabled(false);
		panel.add(btnSeleccionarImatge);

		btnEditar = new JButton("Editar");
		UITheme.styleAccentButton(btnEditar);
		btnEditar.setBounds(btnX, 264, btnW, 38);
		panel.add(btnEditar);

		btnEliminar = new JButton("Eliminar");
		btnEliminar.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		btnEliminar.setBackground(new Color(0xC0392B));
		btnEliminar.setForeground(Color.WHITE);
		btnEliminar.setFont(UITheme.FONT_BOLD);
		btnEliminar.setFocusPainted(false);
		btnEliminar.setBorderPainted(false);
		btnEliminar.setOpaque(true);
		btnEliminar.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		btnEliminar.setBounds(btnX, 312, btnW, 38);
		panel.add(btnEliminar);

		JButton btnTancar = new JButton("Tancar");
		UITheme.styleSecondaryButton(btnTancar);
		btnTancar.setBounds(btnX, 360, btnW, 38);
		btnTancar.addActionListener(e -> dispose());
		panel.add(btnTancar);

		btnGestioLlistes = new JButton("Llistes...");
		UITheme.styleSecondaryButton(btnGestioLlistes);
		btnGestioLlistes.setBounds(btnX, 408, btnW, 38);
		btnGestioLlistes.setToolTipText("Gestionar l'assignació d'aquest llibre a llistes");
		panel.add(btnGestioLlistes);
	}

	private JTextField addRow(JPanel panel, String label, int rowIndex) {
		int y = y(rowIndex);
		JLabel lbl = new JLabel(label);
		UITheme.styleLabel(lbl);
		lbl.setBounds(LBL_X, y, LBL_W, ROW_H);
		panel.add(lbl);

		JTextField field = new JTextField();
		field.setEnabled(false);
		field.setColumns(10);
		UITheme.styleField(field);
		field.setBounds(FIELD_X, y, FIELD_W, ROW_H);
		panel.add(field);
		return field;
	}

	private int y(int rowIndex) {
		return 10 + rowIndex * ROW_STEP;
	}

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
	public JTextField getTextPagines()          { return textPagines; }
	public JTextField getTextPaginesLlegides()  { return textPaginesLlegides; }
	public JCheckBox  getChckLlegit()           { return chckLlegit; }
	public JTextArea  getTextNotes()            { return textNotes; }
	public JButton    getBtnGestioLlistes()     { return btnGestioLlistes; }
}
