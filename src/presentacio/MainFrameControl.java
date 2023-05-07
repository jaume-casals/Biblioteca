package presentacio;

import java.awt.event.WindowEvent;
import java.util.ArrayList;

import domini.ControladorDomini;
import domini.Llibre;
import herramienta.DialogoError;
import interficie.EnActualizarBBDD;
import presentacio.acercade.AcercaDeDialogo;
import presentacio.acercade.AcercaDeDialogoControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;

public class MainFrameControl implements EnActualizarBBDD {

	private ControladorDomini cLlibres;
	private ArrayList<Llibre> biblio;
	private MostrarBibliotecaControl MostrarBibliotecaControl;
	private MainFramePanel vista;
	private GuardarLlibresDialogoControl guardarLlibresDialogoControl;
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

		cLlibres = ControladorDomini.getInstance();

		this.vista.getaddLlibre().addActionListener(e -> new Thread(() -> crearGuardarLlibreDialogo()).start());
		this.vista.getMntmAbout().addActionListener(
				e -> new Thread(() -> new AcercaDeDialogoControl(new AcercaDeDialogo()).setVisible(true)).start());

		MostrarBibliotecaControl = new MostrarBibliotecaControl(this.vista.getMostrarBibliotecaPanel(),
				cLlibres.getAllLlibres(), this);
	}

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null) {
			instance = new MainFrameControl(vista);
		}
		return instance;
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Integer ISBN, Integer iniciAny,
			Integer fiAny, Boolean Llegit) {

		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny, Llegit);

	}

	private WindowEvent caca() {
		MostrarBibliotecaControl.refresh();
		return null;

	}

	private void crearGuardarLlibreDialogo() {
		this.guardarLlibresDialogoControl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo());
		this.guardarLlibresDialogoControl.getVista().setLocationRelativeTo(this.vista);
		this.guardarLlibresDialogoControl.getVista().setVisible(true);
		this.guardarLlibresDialogoControl.windowClosed(caca());
	}

	protected Llibre getLlibreIsbn(int ISBN) {

		try {
			return cLlibres.getLlibre(ISBN);
		} catch (Exception e) {
			new DialogoError(e).showErrorMessage();
		}
		return null;
	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}

	@Override
	public void actualitzarLlibre(Llibre l, boolean nuevo) {
		this.MostrarBibliotecaControl.refreshLlibre(l, nuevo);

	}

}
