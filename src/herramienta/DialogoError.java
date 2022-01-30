package herramienta;

import javax.swing.JOptionPane;

public class DialogoError {

	private JOptionPane dialog;
	private Exception e;
	private String titulo = "Error de entrada"; //$NON-NLS-1$

	public DialogoError(Exception e) {
		this.dialog = new JOptionPane();
		this.e = e;
	}

	@SuppressWarnings("static-access")
	public void showErrorMessage() {
		if (e.getCause() == null)
			JOptionPane.showMessageDialog(null, e.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
		else
			JOptionPane.showMessageDialog(null, e.getMessage(), e.getCause().getMessage(), JOptionPane.ERROR_MESSAGE);
	}
}