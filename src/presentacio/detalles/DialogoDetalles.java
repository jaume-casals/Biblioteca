package presentacio.detalles;

import java.awt.BorderLayout;
import java.awt.Rectangle;

import javax.swing.JDialog;

import domini.Llibre;
import presentacio.detalles.control.DetallesLlibrePanelControl;

public class DialogoDetalles extends JDialog {

	public DialogoDetalles() {
		setName("DialogDetalles");
		dialogInit();
		setBounds(100, 100, 712, 618);
		getContentPane().setLayout(new BorderLayout());
		setModal(true);
		setResizable(false);

	}

	public void abrirDetalles(Object objeto) {
		if (objeto instanceof Llibre) {
			setTitle("Expedient del llibre " + ((Llibre) objeto).getNom());
			setBounds(new Rectangle(0, 0, 675, 550));
			getContentPane().add(new DetallesLlibrePanelControl().getDetallesLlibrePanel());
		}
	}

}
