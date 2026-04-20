package presentacio.detalles.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import herramienta.UITheme;

public class GuardarLlibresDialogo extends JDialog {

	private JScrollPane pane;
	private JLabel labelPreview;
	private JButton btnGuardar;
	private JButton btnSeleccionarImatge;
	private JButton btnCercaInternet;
	private JTextField textISBN;
	private JTextField textNom;
	private JTextField textAutor;
	private JTextField textAny;
	private JTextField textDescripcio;
	private JTextField textValoracio;
	private JTextField textPreu;
	private JTextField textPortada;
	private JCheckBox chckLlegit;
	private JProgressBar progressBar;

	public GuardarLlibresDialogo() {
		setResizable(false);
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle("Nou Llibre");
		setBounds(50, 50, 506, 650);
		getContentPane().setLayout(null);
		getContentPane().setBackground(UITheme.BG_PANEL);

		int c = 192;
		int f = 83;
		int g = 41;

		pane = new JScrollPane();
		getContentPane().add(pane);

		btnGuardar = new JButton("Guardar");
		UITheme.styleAccentButton(btnGuardar);
		btnGuardar.setBounds(24, 37, 120, 34);
		getContentPane().add(btnGuardar);

		JButton btnCancel = new JButton("Cancel·lar");
		UITheme.styleSecondaryButton(btnCancel);
		btnCancel.setBounds(24, 79, 120, 34);
		btnCancel.addActionListener(e -> dispose());
		getContentPane().add(btnCancel);

		labelPreview = new JLabel();
		labelPreview.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_CLR));
		labelPreview.setHorizontalAlignment(SwingConstants.CENTER);
		labelPreview.setBounds(24, 122, 120, 120);
		getContentPane().add(labelPreview);

		JLabel lblNewLabel = new JLabel("ISBN");
		UITheme.styleLabel(lblNewLabel);
		lblNewLabel.setBounds(171, 34, f, g);
		getContentPane().add(lblNewLabel);

		textISBN = new JTextField();
		UITheme.styleField(textISBN);
		textISBN.setBounds(264, 34, c, g);
		getContentPane().add(textISBN);
		textISBN.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("NOM");
		UITheme.styleLabel(lblNewLabel_1);
		lblNewLabel_1.setBounds(171, 86, f, g);
		getContentPane().add(lblNewLabel_1);

		textNom = new JTextField();
		textNom.setColumns(10);
		UITheme.styleField(textNom);
		textNom.setBounds(264, 86, c, g);
		getContentPane().add(textNom);

		JLabel lblNewLabel_2 = new JLabel("AUTOR");
		UITheme.styleLabel(lblNewLabel_2);
		lblNewLabel_2.setBounds(171, 138, f, g);
		getContentPane().add(lblNewLabel_2);

		textAutor = new JTextField();
		textAutor.setColumns(10);
		UITheme.styleField(textAutor);
		textAutor.setBounds(264, 138, c, g);
		getContentPane().add(textAutor);

		JLabel lblNewLabel_3 = new JLabel("ANY");
		UITheme.styleLabel(lblNewLabel_3);
		lblNewLabel_3.setBounds(171, 190, f, g);
		getContentPane().add(lblNewLabel_3);

		textAny = new JTextField();
		textAny.setColumns(10);
		UITheme.styleField(textAny);
		textAny.setBounds(264, 190, c, g);
		getContentPane().add(textAny);

		JLabel lblNewLabel_4 = new JLabel("DESCRIPCIO");
		UITheme.styleLabel(lblNewLabel_4);
		lblNewLabel_4.setBounds(171, 242, f, g);
		getContentPane().add(lblNewLabel_4);

		textDescripcio = new JTextField();
		textDescripcio.setColumns(10);
		UITheme.styleField(textDescripcio);
		textDescripcio.setBounds(264, 242, c, g);
		getContentPane().add(textDescripcio);

		JLabel lblNewLabel_5 = new JLabel("VALORACIO");
		UITheme.styleLabel(lblNewLabel_5);
		lblNewLabel_5.setBounds(171, 294, f, g);
		getContentPane().add(lblNewLabel_5);

		textValoracio = new JTextField();
		textValoracio.setColumns(10);
		UITheme.styleField(textValoracio);
		textValoracio.setBounds(264, 294, c, g);
		getContentPane().add(textValoracio);

		JLabel lblNewLabel_6 = new JLabel("PREU");
		UITheme.styleLabel(lblNewLabel_6);
		lblNewLabel_6.setBounds(171, 346, f, g);
		getContentPane().add(lblNewLabel_6);

		textPreu = new JTextField();
		textPreu.setColumns(10);
		UITheme.styleField(textPreu);
		textPreu.setBounds(264, 346, c, g);
		getContentPane().add(textPreu);

		JLabel lblNewLabel_7 = new JLabel("LLEGIT");
		UITheme.styleLabel(lblNewLabel_7);
		lblNewLabel_7.setBounds(171, 398, f, g);
		getContentPane().add(lblNewLabel_7);

		JLabel lblNewLabel_8 = new JLabel("PORTADA");
		UITheme.styleLabel(lblNewLabel_8);
		lblNewLabel_8.setBounds(171, 450, f, g);
		getContentPane().add(lblNewLabel_8);

		textPortada = new JTextField();
		textPortada.setColumns(10);
		UITheme.styleField(textPortada);
		textPortada.setBounds(264, 450, c, g);
		getContentPane().add(textPortada);

		chckLlegit = new JCheckBox("");
		chckLlegit.setBackground(UITheme.BG_PANEL);
		chckLlegit.setHorizontalAlignment(SwingConstants.CENTER);
		chckLlegit.setBounds(264, 398, 192, 41);
		getContentPane().add(chckLlegit);

		btnSeleccionarImatge = new JButton("Explorar...");
		UITheme.styleSecondaryButton(btnSeleccionarImatge);
		btnSeleccionarImatge.setBounds(264, 497, 192, 34);
		getContentPane().add(btnSeleccionarImatge);

		btnCercaInternet = new JButton("⬇  Cerca a Internet (ISBN / Títol / Autor)");
		UITheme.styleAccentButton(btnCercaInternet);
		btnCercaInternet.setBackground(new java.awt.Color(0x117A65));
		btnCercaInternet.setBounds(24, 548, 458, 40);
		btnCercaInternet.setToolTipText("Omple els camps automàticament cercant a OpenLibrary.org");
		getContentPane().add(btnCercaInternet);

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		progressBar.setBounds(24, 596, 458, 8);
		progressBar.setVisible(false);
		getContentPane().add(progressBar);
	}

	public JLabel getLabelPreview() { return labelPreview; }
	public JProgressBar getProgressBar() { return progressBar; }
	public JButton getBtnGuardar() { return btnGuardar; }
	public JButton getBtnSeleccionarImatge() { return btnSeleccionarImatge; }
	public JButton getBtnCercaInternet() { return btnCercaInternet; }


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
