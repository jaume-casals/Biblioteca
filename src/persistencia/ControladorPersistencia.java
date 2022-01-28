
package persistencia;

import java.util.ArrayList;

import domini.Llibre;

public class ControladorPersistencia {

	private static ControladorPersistencia inst;
	private ServerConect sc;

	public static ControladorPersistencia getInstance() {
		if (ControladorPersistencia.inst == null) ControladorPersistencia.inst = new ControladorPersistencia();
		return ControladorPersistencia.inst;
	}

	private ControladorPersistencia() {
		sc = new ServerConect();
		sc.createDatabase();
	}

	public ArrayList<Llibre> getAllLlibres() {
		return sc.getAllLlibres();
	}

	public void replaceAllLlibres(ArrayList<Llibre> llibres) {
		sc.resetDatabase();
		for (int i = 0; i < llibres.size(); ++i) {
			sc.afegirLlibre(llibres.get(i));
		}
	}

	public void afegirLlibre(Llibre llibre) {
		sc.afegirLlibre(llibre);
	}

	public void eliminarLlibre(Llibre llibre) {
		sc.deleteLlibre(llibre);
	}

	public void eliminarLlibre(int ISBN) {
		sc.deleteLlibre(new Llibre(ISBN, "nom", "autor", 1, "descripcio", 0.0, 0.0, false, "portada"));
	}
}
