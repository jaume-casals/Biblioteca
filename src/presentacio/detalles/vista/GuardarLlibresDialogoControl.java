package presentacio.detalles.vista;

import java.awt.Dialog;

import domini.ControladorDomini;
import domini.Llibre;

public class GuardarLlibresDialogoControl {

	private GuardarLlibresDialogo vista;
	private ControladorDomini cLlibres;

	public GuardarLlibresDialogoControl(GuardarLlibresDialogo vista) {
		this.vista = vista;
		this.vista.getBtnGuardar().addActionListener(e -> crearLlibre());
		cLlibres = ControladorDomini.getInstance();
	}

	private void crearLlibre() {
		System.out.println(Integer.parseInt(vista.getTextISBN().getText()));
		boolean llegit = false;
		if (vista.getChckLlegit().isSelected()) {
			llegit = true;
		}
		try {
			cLlibres.addLlibre(new Llibre(
					Integer.parseInt(vista.getTextISBN().getText()),
					vista.getTextNom().getText(),
					vista.getTextAutor().getText(), 
					Integer.parseInt(vista.getTextAny().getText()),
					vista.getTextDescripcio().getText(), 
					Double.parseDouble(vista.getTextValoracio().getText()),
					Double.parseDouble(vista.getTextPreu().getText()),
					llegit, vista.getTextPortada().getText()));
			vista.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Dialog getVista() {
		return vista;
	}

}
