package presentacio;

import java.util.ArrayList;
import java.util.List;

import domini.Llibre;
import presentacio.acercade.AcercaDeDialogoControl;
import presentacio.acercade.AcercaDeDialogo;
import persistencia.ControladorLlibres;
import persistencia.ServerConect;

public class MainFrameControl {

	private static ServerConect con = new ServerConect();
	private static ControladorLlibres cLlibres;
	private static ArrayList<Llibre> biblio;
	private static MostrarBibliotecaControl MostrarBibliotecaControl;
	private static MainFramePanel vista;

	public MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

//		con.startConection();

//		biblio = new LinkedList<Llibre>();

//		biblio = con.getAllLlibres();

//		cLlibres = new ControladorLlibres(con.getConnection(), biblio);
		this.vista.getMntmAbout().addActionListener(
				e -> new Thread(() -> new AcercaDeDialogoControl(new AcercaDeDialogo()).setVisible(true)).start());

		MostrarBibliotecaControl = new MostrarBibliotecaControl(this.vista.getMostrarBibliotecaPanel(), biblio,
				con.getHeader());
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

	}

	public void setVisible(boolean b) {
		this.vista.setVisible(b);
	}

}
