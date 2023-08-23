package presentacio.acercade;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

public class AcercaDeDialogo extends JDialog {

	private JButton btnCredit;
	private JButton btnExit;
	private JButton btnLicencia;
	private CardLayout casJorcl;
	private final JPanel contentPanel = new JPanel();
	private JPanel midpanel;

	public AcercaDeDialogo() {
		setTitle("Sobre Nosaltres");
		setModal(true);
		setResizable(false);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		setBounds(100, 100, 450, 300);
		setContentPane(contentPanel);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		JPanel toppanel = new JPanel();
		contentPanel.add(toppanel);
		toppanel.setLayout(new BoxLayout(toppanel, BoxLayout.Y_AXIS));

		JLabel lblNewLabel_2 = new JLabel("");
		lblNewLabel_2.setAlignmentX(Component.CENTER_ALIGNMENT);
		toppanel.add(lblNewLabel_2);

		JLabel lblNewLabel_1 = new JLabel("Biblioteca 0.1.2-beta");
		lblNewLabel_1.setAlignmentX(Component.CENTER_ALIGNMENT);
		toppanel.add(lblNewLabel_1);

		midpanel = new JPanel();
		contentPanel.add(midpanel);
		midpanel.setName("Jaume i Jordi Casals Vilaplana");

		contentPanel.add(midpanel);
		casJorcl = new CardLayout(0, 0);
		midpanel.setLayout(casJorcl);

		JPanel first = new JPanel();
		midpanel.add(first, "first"); //$NON-NLS-1$
		first.setLayout(new BoxLayout(first, BoxLayout.Y_AXIS));

		JLabel lblNewLabel = new JLabel(
				"<html><p style=\"text-align: center;margin: 30px\">Software de gestio de llibres</html></p>");
		lblNewLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		first.add(lblNewLabel);

		Component verticalGlue = Box.createVerticalGlue();
		first.add(verticalGlue);

		JLabel lblNewLabel_3 = new JLabel("2022 Biblioteca");
		lblNewLabel_3.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblNewLabel_3.setBounds(new Rectangle(10, 10, 10, 10));
		first.add(lblNewLabel_3);

		Component rigidArea = Box.createRigidArea(new Dimension(20, 20));
		first.add(rigidArea);

		JScrollPane scrolllicense = new JScrollPane();
		midpanel.add(scrolllicense, "Licencia"); //$NON-NLS-1$

		JEditorPane editorPane = new JEditorPane();
		editorPane.setEditable(false);
		scrolllicense.setViewportView(editorPane);

		JEditorPane editorCredit = new JEditorPane();
		midpanel.add(editorCredit, "Credito"); //$NON-NLS-1$

		URL UrlCredit = AcercaDeDialogo.class.getResource("/fichers/credit.html"); //$NON-NLS-1$
		if (UrlCredit != null) {
			try {
				editorCredit.setPage(UrlCredit);
			} catch (IOException e) {
				System.err.println("Intentado leer una URL mala:" + UrlCredit); //$NON-NLS-1$
			}
		} else {
			System.err.println("No se ha podido encontrar fichero credit.html"); //$NON-NLS-1$
		}

		URL UrlLicen = AcercaDeDialogo.class.getResource("/fichers/license.html"); //$NON-NLS-1$
		if (UrlLicen != null) {
			try {
				editorPane.setPage(UrlLicen);
			} catch (IOException e) {
				System.err.println("Intentado leer una URL mala:" + UrlLicen); //$NON-NLS-1$
			}
		} else {
			System.err.println("No se ha podido encontrar fichero license.html"); //$NON-NLS-1$
		}

		JPanel botpanel = new JPanel();
		contentPanel.add(botpanel);
		botpanel.setLayout(new BoxLayout(botpanel, BoxLayout.X_AXIS));

		btnLicencia = new JButton("Licencia"); //$NON-NLS-1$

		botpanel.add(btnLicencia);

		btnCredit = new JButton("Credito"); //$NON-NLS-1$

		Component rigidArea_1 = Box.createRigidArea(new Dimension(20, 20));
		botpanel.add(rigidArea_1);
		botpanel.add(btnCredit);

		Component glue = Box.createGlue();
		botpanel.add(glue);

		btnExit = new JButton("Salir"); //$NON-NLS-1$
		botpanel.add(btnExit);

	}

	public JButton getBtnCredito() {
		return btnCredit;
	}

	public JButton getBtnExit() {
		return btnExit;
	}

	public JButton getBtnLicencia() {
		return btnLicencia;
	}

	public CardLayout getCL() {
		return casJorcl;
	}

	public JPanel getMidPanel() {
		return midpanel;
	}
}
