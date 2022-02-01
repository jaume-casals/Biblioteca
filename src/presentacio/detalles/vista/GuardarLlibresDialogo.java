package presentacio.detalles.vista;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;

public class GuardarLlibresDialogo extends JDialog {

	private JScrollPane pane;
	private JButton btnGuardar;

	public GuardarLlibresDialogo() {
		setResizable(false);
		setModal(true);
		setTitle("Nou Llibre");
		setBounds(50, 50, 506, 463);
		getContentPane().setLayout(null);

		pane = new JScrollPane();
		add(pane);

		btnGuardar = new JButton("Guardar"); //$NON-NLS-1$
		btnGuardar.setBounds(24, 37, 83, 34);
		getContentPane().add(btnGuardar);
	}

	public JButton getBtnGuardar() {
		return btnGuardar;
	}
}
