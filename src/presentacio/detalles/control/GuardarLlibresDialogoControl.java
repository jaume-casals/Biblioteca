package presentacio.detalles.control;

import java.awt.Dialog;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import presentacio.detalles.vista.GuardarLlibresDialogo;

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

		try {
			cLlibres.addLlibre(new Llibre(Integer.parseInt(vista.getTextISBN().getText()), vista.getTextNom().getText(),
					vista.getTextAutor().getText(), Integer.parseInt(vista.getTextAny().getText()),
					vista.getTextDescripcio().getText(), Double.parseDouble(vista.getTextValoracio().getText()),
					Double.parseDouble(vista.getTextPreu().getText()), vista.getChckLlegit().isSelected(),
					vista.getTextPortada().getText()));
			vista.dispose();
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}

	}

	public Dialog getVista() {
		return vista;
	}

}
