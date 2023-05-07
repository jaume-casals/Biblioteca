package presentacio.detalles.control;

import java.awt.Dialog;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class GuardarLlibresDialogoControl implements WindowListener{

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

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

}
