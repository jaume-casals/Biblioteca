
package persistencia;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;

public class ControladorPersistencia {

	private static ControladorPersistencia inst;
	private ServerConect sc;

	public static ControladorPersistencia getInstance() {
		if (ControladorPersistencia.inst == null)
			ControladorPersistencia.inst = new ControladorPersistencia();
		return ControladorPersistencia.inst;
	}

	public static void resetForTest() {
		if (inst != null) {
			try { inst.sc.clearAllData(); } catch (Exception ignored) {}
		}
		inst = null;
	}

	private ControladorPersistencia() {
		sc = new ServerConect();
		sc.createDatabase();
	}

	public ArrayList<Llibre> getAllLlibres() {
		return sc.getAllLlibres();
	}

	public void replaceAllLlibres(ArrayList<Llibre> llibres) throws java.sql.SQLException {
		sc.resetDatabase();
		for (Llibre l : llibres) sc.afegirLlibre(l);
	}

	public void afegirLlibre(Llibre llibre) throws java.sql.SQLException {
		sc.afegirLlibre(llibre);
	}

	public void eliminarLlibre(Llibre llibre) throws java.sql.SQLException {
		sc.deleteLlibre(llibre);
	}

	public void eliminarLlibre(long ISBN) throws java.sql.SQLException {
		sc.deleteLlibre(new Llibre(ISBN, "nom", "autor", 1, "descripcio", 0.0, 0.0, false, "portada"));
	}

	public void executeSQLFile(java.io.File file) throws Exception {
		sc.executeSQLFile(file);
	}

	public ArrayList<Llista> getAllLlistes() { return sc.getAllLlistes(); }
	public int createLlista(String nom) throws SQLException { return sc.createLlista(nom); }
	public void deleteLlista(int id) throws SQLException { sc.deleteLlista(id); }
	public int getCountInLlista(int llistaId) { return sc.getCountInLlista(llistaId); }
	public java.util.List<Object[]> getAllLlibreLlista() { return sc.getAllLlibreLlista(); }
	public ArrayList<Llibre> getLlibresInLlista(int llistaId) { return sc.getLlibresInLlista(llistaId); }
	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { sc.addLlibreToLlista(isbn, llistaId, valoracio, llegit); }
	public void removeLlibreFromLlista(long isbn, int llistaId) throws SQLException { sc.removeLlibreFromLlista(isbn, llistaId); }
	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { sc.updateLlibreInLlista(isbn, llistaId, valoracio, llegit); }
	public ArrayList<Llista> getLlistesForLlibre(long isbn) { return sc.getLlistesForLlibre(isbn); }
	public void updateLlistaOrdre(int id, int ordre) throws java.sql.SQLException { sc.updateLlistaOrdre(id, ordre); }
	public void updateLlistaColor(int id, String color) throws java.sql.SQLException { sc.updateLlistaColor(id, color); }
	public ArrayList<Llibre> getRecentlyAdded(int n) { return sc.getRecentlyAdded(n); }
	public void addPrestec(long isbn, String nom) throws java.sql.SQLException { sc.addPrestec(isbn, nom); }
	public void returnPrestec(long isbn) throws java.sql.SQLException { sc.returnPrestec(isbn); }
	public java.util.Set<Long> getLoanedISBNs() { return sc.getLoanedISBNs(); }
}
