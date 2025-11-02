package herramienta;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

public class DialogoError {

	private Exception e;
	private String titulo = "Error de entrada"; //$NON-NLS-1$

	public DialogoError(Exception e) {
		this.e = e;
	}

	@SuppressWarnings("static-access")
	public void showErrorMessage() {
		// Check if running in headless environment
		if (GraphicsEnvironment.isHeadless()) {
			// Fallback to console output when no display is available
			System.err.println("ERROR: " + titulo);
			if (e.getCause() == null) {
				System.err.println(e.getMessage());
			} else {
				System.err.println(e.getMessage());
				System.err.println("Caused by: " + e.getCause().getMessage());
			}
			e.printStackTrace();
			return;
		}
		
		// Show GUI dialog when display is available
		try {
			if (e.getCause() == null)
				JOptionPane.showMessageDialog(null, e.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
			else
				JOptionPane.showMessageDialog(null, e.getMessage(), e.getCause().getMessage(), JOptionPane.ERROR_MESSAGE);
		} catch (java.awt.HeadlessException he) {
			// Fallback if HeadlessException is thrown despite check
			System.err.println("ERROR: " + titulo);
			if (e.getCause() == null) {
				System.err.println(e.getMessage());
			} else {
				System.err.println(e.getMessage());
				System.err.println("Caused by: " + e.getCause().getMessage());
			}
			e.printStackTrace();
		}
	}
}