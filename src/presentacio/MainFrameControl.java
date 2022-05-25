package presentacio;

import java.util.ArrayList;

import domini.Llibre;
import presentacio.acercade.AcercaDeDialogoControl;
import presentacio.detalles.control.GuardarLlibresDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;
import presentacio.acercade.AcercaDeDialogo;
import domini.ControladorDomini;

public class MainFrameControl {

	private ControladorDomini cLlibres;
	private ArrayList<Llibre> biblio;
	private MostrarBibliotecaControl MostrarBibliotecaControl;
	private MainFramePanel vista;
	private GuardarLlibresDialogoControl guardarLlibresDialogoControl;
	private static MainFrameControl instance;

	private MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

		cLlibres = ControladorDomini.getInstance();

		this.vista.getaddLlibre().addActionListener(e -> new Thread(() -> crearLlibreDialogo()).start());
		this.vista.getMntmAbout().addActionListener(
				e -> new Thread(() -> new AcercaDeDialogoControl(new AcercaDeDialogo()).setVisible(true)).start());

		MostrarBibliotecaControl = new MostrarBibliotecaControl(this.vista.getMostrarBibliotecaPanel(),
				cLlibres.getAllLlibres());
	}

	public static MainFrameControl getInstance(MainFramePanel vista) {
		if (instance == null) {
			instance = new MainFrameControl(vista);
		}
		return instance;
	}

	protected ArrayList<Llibre> aplicarFiltres(String nomAutor, String nomLlibre, Integer ISBN, Integer iniciAny,
			Integer fiAny) {

		return cLlibres.aplicarFiltres(nomAutor, nomLlibre, ISBN, iniciAny, fiAny, null);

	}

	private void crearLlibreDialogo() {
		this.guardarLlibresDialogoControl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo());
		this.guardarLlibresDialogoControl.getVista().setLocationRelativeTo(this.vista);
		this.guardarLlibresDialogoControl.getVista().setVisible(true);
	}

	protected Llibre getLlibreIsbn(int ISBN) {

		try {
			return cLlibres.getLlibre(ISBN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}

}
