package presentacio.detalles.vista;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import herramienta.UITheme;

public class DetallesLlibrePanel extends JDialog {

	private JLabel labelIcono;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textPortada;
	private JCheckBox chckLlegit;
	private JButton btnNewButton;
	private JButton btnTancar;
	private JButton btnSeleccionarImatge;

	public DetallesLlibrePanel() {
		setBounds(new Rectangle(1000, 2000, 0, 0));
		setAlwaysOnTop(true);
		setLocation(new Point(1000, 2000));
		setName("Detalls Llibre"); //$NON-NLS-1$
		setModal(true);
		setBounds(0, 0, 600, 675);
		getContentPane().setLayout(null);
		setResizable(false);

		JPanel panel = new JPanel();
		panel.setBackground(UITheme.BG_PANEL);
		panel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		panel.setLayout(null);
		add(panel);
		panel.setBounds(new Rectangle(0, 0, 600, 675));

		labelIcono = new JLabel("");
		labelIcono.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		labelIcono.setBounds(352, 64, 200, 200);
		panel.add(labelIcono);

		int c = 192;
		int f = 83;
		int g = 41;

		JLabel lblNewLabel = new JLabel("ISBN");
		UITheme.styleLabel(lblNewLabel);
		lblNewLabel.setBounds(31, 33, f, g);
		panel.add(lblNewLabel);

		textISBN = new JTextField();
		textISBN.setEnabled(false);
		UITheme.styleField(textISBN);
		textISBN.setBounds(124, 33, c, g);
		panel.add(textISBN);
		textISBN.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("NOM");
		UITheme.styleLabel(lblNewLabel_1);
		lblNewLabel_1.setBounds(31, 85, f, g);
		panel.add(lblNewLabel_1);

		textNom = new JTextField();
		textNom.setEnabled(false);
		textNom.setColumns(10);
		UITheme.styleField(textNom);
		textNom.setBounds(124, 85, c, g);
		panel.add(textNom);

		JLabel lblNewLabel_2 = new JLabel("AUTOR");
		UITheme.styleLabel(lblNewLabel_2);
		lblNewLabel_2.setBounds(31, 137, f, g);
		panel.add(lblNewLabel_2);

		textAutor = new JTextField();
		textAutor.setEnabled(false);
		textAutor.setColumns(10);
		UITheme.styleField(textAutor);
		textAutor.setBounds(124, 137, c, g);
		panel.add(textAutor);

		JLabel lblNewLabel_3 = new JLabel("ANY");
		UITheme.styleLabel(lblNewLabel_3);
		lblNewLabel_3.setBounds(31, 189, f, g);
		panel.add(lblNewLabel_3);

		textAny = new JTextField();
		textAny.setEnabled(false);
		textAny.setColumns(10);
		UITheme.styleField(textAny);
		textAny.setBounds(124, 189, c, g);
		panel.add(textAny);

		JLabel lblNewLabel_4 = new JLabel("DESCRIPCIO");
		UITheme.styleLabel(lblNewLabel_4);
		lblNewLabel_4.setBounds(31, 241, f, g);
		panel.add(lblNewLabel_4);

		textDescripcio = new JTextField();
		textDescripcio.setEnabled(false);
		textDescripcio.setColumns(10);
		UITheme.styleField(textDescripcio);
		textDescripcio.setBounds(124, 241, c, g);
		panel.add(textDescripcio);

		JLabel lblNewLabel_5 = new JLabel("VALORACIO");
		UITheme.styleLabel(lblNewLabel_5);
		lblNewLabel_5.setBounds(31, 293, f, g);
		panel.add(lblNewLabel_5);

		textValoracio = new JTextField();
		textValoracio.setEnabled(false);
		textValoracio.setColumns(10);
		UITheme.styleField(textValoracio);
		textValoracio.setBounds(124, 293, c, g);
		panel.add(textValoracio);

		JLabel lblNewLabel_6 = new JLabel("PREU");
		UITheme.styleLabel(lblNewLabel_6);
		lblNewLabel_6.setBounds(31, 345, f, g);
		panel.add(lblNewLabel_6);

		textPreu = new JTextField();
		textPreu.setEnabled(false);
		textPreu.setColumns(10);
		UITheme.styleField(textPreu);
		textPreu.setBounds(124, 345, c, g);
		panel.add(textPreu);

		JLabel lblNewLabel_7 = new JLabel("LLEGIT");
		UITheme.styleLabel(lblNewLabel_7);
		lblNewLabel_7.setBounds(31, 397, f, g);
		panel.add(lblNewLabel_7);

		JLabel lblNewLabel_8 = new JLabel("PORTADA");
		UITheme.styleLabel(lblNewLabel_8);
		lblNewLabel_8.setBounds(31, 449, f, g);
		panel.add(lblNewLabel_8);

		textPortada = new JTextField();
		textPortada.setEnabled(false);
		textPortada.setColumns(10);
		UITheme.styleField(textPortada);
		textPortada.setBounds(124, 449, c, g);
		panel.add(textPortada);

		chckLlegit = new JCheckBox("");
		chckLlegit.setEnabled(false);
		chckLlegit.setBackground(UITheme.BG_PANEL);
		chckLlegit.setHorizontalAlignment(SwingConstants.CENTER);
		chckLlegit.setBounds(124, 397, 192, 41);
		panel.add(chckLlegit);

		btnSeleccionarImatge = new JButton("Seleccionar imatge");
		UITheme.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setBounds(352, 274, 200, 34);
		btnSeleccionarImatge.setEnabled(false);
		panel.add(btnSeleccionarImatge);

		btnNewButton = new JButton("Editar");
		UITheme.styleAccentButton(btnNewButton);
		btnNewButton.setBounds(368, 449, 184, 38);
		panel.add(btnNewButton);

		btnTancar = new JButton("Tancar");
		UITheme.styleSecondaryButton(btnTancar);
		btnTancar.setBounds(368, 495, 184, 38);
		btnTancar.addActionListener(e -> dispose());
		panel.add(btnTancar);

	}

	public JLabel getLabelIcono() {
		return labelIcono;
	}

	public JButton getBtnSeleccionarImatge() { return btnSeleccionarImatge; }

	public JButton getBtnEditar() {
		return btnNewButton;
	}

	public JButton getBtnTancar() {
		return btnTancar;
	}

	public JTextField getTextISBN() {
		return textISBN;
	}

	public JTextField getTextDescripcio() {
		return textDescripcio;
	}

	public JTextField getTextPreu() {
		return textPreu;
	}

	public JTextField getTextValoracio() {
		return textValoracio;
	}

	public JTextField getTextNom() {
		return textNom;
	}

	public JTextField getTextPortada() {
		return textPortada;
	}

	public JCheckBox getChckLlegit() {
		return chckLlegit;
	}

	public JTextField getTextAutor() {
		return textAutor;
	}

	public JTextField getTextAny() {
		return textAny;
	}

	public boolean getChckLlegitEnabled() {
		return chckLlegit.isEnabled();
	}

}
