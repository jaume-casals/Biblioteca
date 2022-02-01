package presentacio.detalles.vista;

import java.awt.Dialog;

public class GuardarLlibresDialogoControl {

	private GuardarLlibresDialogo vista;

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista) {
		this.vista = vista;
		this.vista.getBtnGuardar().addActionListener(e -> crearLlibre());
	}

	private void crearLlibre() {
	}

	public Dialog getVista() {
		return vista;
	}

}
