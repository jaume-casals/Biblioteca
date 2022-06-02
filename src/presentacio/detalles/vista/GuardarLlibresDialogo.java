package presentacio.detalles.vista;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GuardarLlibresDialogo extends JDialog {

	private JScrollPane pane;
	private JButton btnGuardar;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textPortada;
	private JCheckBox chckLlegit;

	public GuardarLlibresDialogo() {
		setResizable(false);
		setModal(true);
		setTitle("Nou Llibre");
		setBounds(50, 50, 506, 558);
		getContentPane().setLayout(null);

		int c = 192;
		int f = 83;
		int g = 41;

		pane = new JScrollPane();
		getContentPane().add(pane);

		btnGuardar = new JButton("Guardar"); //$NON-NLS-1$
		btnGuardar.setBounds(24, 37, 83, 34);
		getContentPane().add(btnGuardar);

		JLabel lblNewLabel = new JLabel("ISBN");
		lblNewLabel.setBounds(171, 34, f, g);
		getContentPane().add(lblNewLabel);

		textISBN = new JTextField();
		textISBN.setBounds(264, 34, c, g);
		getContentPane().add(textISBN);
		textISBN.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("NOM");
		lblNewLabel_1.setBounds(171, 86, f, g);
		getContentPane().add(lblNewLabel_1);

		textNom = new JTextField();
		textNom.setColumns(10);
		textNom.setBounds(264, 86, c, g);
		getContentPane().add(textNom);

		JLabel lblNewLabel_2 = new JLabel("AUTOR");
		lblNewLabel_2.setBounds(171, 138, f, g);
		getContentPane().add(lblNewLabel_2);

		textAutor = new JTextField();
		textAutor.setColumns(10);
		textAutor.setBounds(264, 138, c, g);
		getContentPane().add(textAutor);

		JLabel lblNewLabel_3 = new JLabel("ANY");
		lblNewLabel_3.setBounds(171, 190, f, g);
		getContentPane().add(lblNewLabel_3);

		textAny = new JTextField();
		textAny.setColumns(10);
		textAny.setBounds(264, 190, c, g);
		getContentPane().add(textAny);

		JLabel lblNewLabel_4 = new JLabel("DESCRIPCIO");
		lblNewLabel_4.setBounds(171, 242, f, g);
		getContentPane().add(lblNewLabel_4);

		textDescripcio = new JTextField();
		textDescripcio.setColumns(10);
		textDescripcio.setBounds(264, 242, c, g);
		getContentPane().add(textDescripcio);

		JLabel lblNewLabel_5 = new JLabel("VALORACIO");
		lblNewLabel_5.setBounds(171, 294, f, g);
		getContentPane().add(lblNewLabel_5);

		textValoracio = new JTextField();
		textValoracio.setColumns(10);
		textValoracio.setBounds(264, 294, c, g);
		getContentPane().add(textValoracio);

		JLabel lblNewLabel_6 = new JLabel("PREU");
		lblNewLabel_6.setBounds(171, 346, f, g);
		getContentPane().add(lblNewLabel_6);

		textPreu = new JTextField();
		textPreu.setColumns(10);
		textPreu.setBounds(264, 346, c, g);
		getContentPane().add(textPreu);

		JLabel lblNewLabel_7 = new JLabel("LLEGIT");
		lblNewLabel_7.setBounds(171, 398, f, g);
		getContentPane().add(lblNewLabel_7);

		JLabel lblNewLabel_8 = new JLabel("PORTADA");
		lblNewLabel_8.setBounds(171, 450, f, g);
		getContentPane().add(lblNewLabel_8);

		textPortada = new JTextField();
		textPortada.setColumns(10);
		textPortada.setBounds(264, 450, c, g);
		getContentPane().add(textPortada);

		chckLlegit = new JCheckBox("");
		chckLlegit.setHorizontalAlignment(SwingConstants.CENTER);
		chckLlegit.setBounds(264, 398, 192, 41);
		getContentPane().add(chckLlegit);
	}

	public JButton getBtnGuardar() {
		return btnGuardar;
	}

	public JTextField getTextPortada() {
		return textPortada;
	}

	public JTextField getTextISBN() {
		return textISBN;
	}

	public JTextField getTextDescripcio() {
		return textDescripcio;
	}

	public JTextField getTextNom() {
		return textNom;
	}

	public JTextField getTextPreu() {
		return textPreu;
	}

	public JTextField getTextAutor() {
		return textAutor;
	}

	public JTextField getTextValoracio() {
		return textValoracio;
	}

	public JTextField getTextAny() {
		return textAny;
	}

	public JCheckBox getChckLlegit() {
		return chckLlegit;
	}
}
