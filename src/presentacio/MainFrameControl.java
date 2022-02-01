package presentacio;

import java.util.ArrayList;

import domini.Llibre;
import presentacio.acercade.AcercaDeDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;
import presentacio.detalles.vista.GuardarLlibresDialogoControl;
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

//		con.startConection();

//		biblio = new LinkedList<Llibre>();

//		biblio = con.getAllLlibres();

		cLlibres = ControladorDomini.getInstance();
//		cLlibres.afegirLlibre(new Llibre(1, "ala", "ala", 1231, "dsakfljhasdl�kfjhasldf", 100.01, 10.3, Boolean.TRUE));

//		for (int i = 0; i < biblio.size(); i++) {
//			System.out.println(biblio.get(i).toString());
//		}

//		cLlibres.deleteLlibre(cLlibres.buscarLlibre(4));

//		cLlibres.organitzarLlibre();
//		for (int i = 0; i < biblio.size(); i++) {
//			System.out.println(biblio.get(i).toString());
//		}

//		con.closeConection();

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
