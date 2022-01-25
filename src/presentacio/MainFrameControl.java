package presentacio;

import java.util.LinkedList;
import java.util.List;

import domini.Llibre;
import persistencia.ControladorLlibres;
import persistencia.ServerConect;

public class MainFrameControl {

	private static ServerConect con = new ServerConect();
	private static ControladorLlibres cLlibres;
	private static List<Llibre> biblio;
	private static Table table = new Table();
	private static TableController tableController;
	private static MainFramePanel vista;

	public MainFrameControl(MainFramePanel vista) {
		this.vista = vista;

		con.startConection();

		biblio = new LinkedList<Llibre>();

		biblio = con.getAllLlibres();

		cLlibres = new ControladorLlibres(con.getConnection(), biblio);

		tableController = new TableController(table, biblio, con.getHeader());

//		cLlibres.afegirLlibre(new Llibre(1, "ala", "ala", 1231, "dsakfljhasdl�kfjhasldf", 100.01, 10.3, Boolean.TRUE));

//		for (int i = 0; i < biblio.size(); i++) {
//			System.out.println(biblio.get(i).toString());
//		}

//		cLlibres.deleteLlibre(cLlibres.buscarLlibre(4));

//		cLlibres.organitzarLlibre();
//		for (int i = 0; i < biblio.size(); i++) {
//			System.out.println(biblio.get(i).toString());
//		}

		con.closeConection();

	}

}
