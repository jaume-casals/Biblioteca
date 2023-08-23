package presentacio.acercade;

public class AcercaDeDialogoControl {
	private AcercaDeDialogo vista;

	public AcercaDeDialogoControl(AcercaDeDialogo vista) {
		this.vista = vista;
		this.vista.getBtnExit().addActionListener(e -> dispose());
		this.vista.getBtnCredito().addActionListener(e -> credito());
		this.vista.getBtnLicencia().addActionListener(e -> licencia());

	}

	private void credito() {
		System.out.println();
		this.vista.getCL().show(this.vista.getMidPanel(), "Credito");
	}

	private void dispose() {
		this.vista.dispose();
	}

	private void licencia() {
		this.vista.getCL().show(this.vista.getMidPanel(), "Licencia");

	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}
}
