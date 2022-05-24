package presentacio.detalles.vista;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

public class DetallesLlibrePanel extends JPanel {

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

	public DetallesLlibrePanel() {
		setBorder(new LineBorder(Color.GRAY));
		setLayout(null);

		labelIcono = new JLabel("");

		labelIcono.setBounds(352, 64, 200, 200);
		add(labelIcono);

		int c = 192;
		int f = 83;
		int g = 41;

		JLabel lblNewLabel = new JLabel("ISBN");
		lblNewLabel.setBounds(31, 33, f, g);
		add(lblNewLabel);

		textISBN = new JTextField();
		textISBN.setEnabled(false);
		textISBN.setBounds(124, 33, c, g);
		add(textISBN);
		textISBN.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("NOM");
		lblNewLabel_1.setBounds(31, 85, f, g);
		add(lblNewLabel_1);

		textNom = new JTextField();
		textNom.setEnabled(false);
		textNom.setColumns(10);
		textNom.setBounds(124, 85, c, g);
		add(textNom);

		JLabel lblNewLabel_2 = new JLabel("AUTOR");
		lblNewLabel_2.setBounds(31, 137, f, g);
		add(lblNewLabel_2);

		textAutor = new JTextField();
		textAutor.setEnabled(false);
		textAutor.setColumns(10);
		textAutor.setBounds(124, 137, c, g);
		add(textAutor);

		JLabel lblNewLabel_3 = new JLabel("ANY");
		lblNewLabel_3.setBounds(31, 189, f, g);
		add(lblNewLabel_3);

		textAny = new JTextField();
		textAny.setEnabled(false);
		textAny.setColumns(10);
		textAny.setBounds(124, 189, c, g);
		add(textAny);

		JLabel lblNewLabel_4 = new JLabel("DESCRIPCIO");
		lblNewLabel_4.setBounds(31, 241, f, g);
		add(lblNewLabel_4);

		textDescripcio = new JTextField();
		textDescripcio.setEnabled(false);
		textDescripcio.setColumns(10);
		textDescripcio.setBounds(124, 241, c, g);
		add(textDescripcio);

		JLabel lblNewLabel_5 = new JLabel("VALORACIO");
		lblNewLabel_5.setBounds(31, 293, f, g);
		add(lblNewLabel_5);

		textValoracio = new JTextField();
		textValoracio.setEnabled(false);
		textValoracio.setColumns(10);
		textValoracio.setBounds(124, 293, c, g);
		add(textValoracio);

		JLabel lblNewLabel_6 = new JLabel("PREU");
		lblNewLabel_6.setBounds(31, 345, f, g);
		add(lblNewLabel_6);

		textPreu = new JTextField();
		textPreu.setEnabled(false);
		textPreu.setColumns(10);
		textPreu.setBounds(124, 345, c, g);
		add(textPreu);

		JLabel lblNewLabel_7 = new JLabel("LLEGIT");
		lblNewLabel_7.setBounds(31, 397, f, g);
		add(lblNewLabel_7);

		JLabel lblNewLabel_8 = new JLabel("PORTADA");
		lblNewLabel_8.setBounds(31, 449, f, g);
		add(lblNewLabel_8);

		textPortada = new JTextField();
		textPortada.setEnabled(false);
		textPortada.setColumns(10);
		textPortada.setBounds(124, 449, c, g);
		add(textPortada);

		chckLlegit = new JCheckBox("");
		chckLlegit.setEnabled(false);
		chckLlegit.setHorizontalAlignment(SwingConstants.CENTER);
		chckLlegit.setBounds(124, 397, 192, 41);
		add(chckLlegit);

		btnNewButton = new JButton("Editar");
		btnNewButton.setBounds(368, 449, 184, 41);
		add(btnNewButton);

	}

	public JLabel getLabelIcono() {
		return labelIcono;
	}

	public JButton getBtnEditar() {
		return btnNewButton;
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
