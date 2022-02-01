package presentacio.detalles.control;

import presentacio.detalles.vista.DetallesLlibrePanel;

public class DetallesLlibrePanelControl {

	private DetallesLlibrePanel vista;

	public DetallesLlibrePanelControl() {
		this.vista = new DetallesLlibrePanel();
	}

	public DetallesLlibrePanel getDetallesLlibrePanel() {
		return this.vista;
	}
}
