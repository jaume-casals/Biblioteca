package presentacio;

import java.util.ArrayList;

import domini.Llibre;
import presentacio.acercade.AcercaDeDialogoControl;
import presentacio.detalles.vista.GuardarLlibresDialogo;
import presentacio.detalles.vista.GuardarLlibresDialogoControl;
import presentacio.acercade.AcercaDeDialogo;
import persistencia.ControladorPersistencia;
import persistencia.ServerConect;

public class MainFrameControl {

	private ServerConect con = new ServerConect();
	private ControladorPersistencia cLlibres;
	private ArrayList<Llibre> biblio;
	private MostrarBibliotecaControl MostrarBibliotecaControl;
	private MainFramePanel vista;
	private GuardarLlibresDialogoControl guardarLlibresDialogoControl;

	public MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

//		con.startConection();

//		biblio = new LinkedList<Llibre>();

//		biblio = con.getAllLlibres();

//		cLlibres = new ControladorLlibres(con.getConnection(), biblio);

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

		MostrarBibliotecaControl = new MostrarBibliotecaControl(this.vista.getMostrarBibliotecaPanel(), biblio,
				con.getHeader());
	}

	private void crearLlibreDialogo() {
		this.guardarLlibresDialogoControl = new GuardarLlibresDialogoControl(new GuardarLlibresDialogo());
		this.guardarLlibresDialogoControl.getVista().setLocationRelativeTo(this.vista);
		this.guardarLlibresDialogoControl.getVista().setVisible(true);
	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}

}
