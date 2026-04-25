
package persistencia;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import domini.Tag;

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
		sc.deleteLlibre(ISBN);
	}

	public void executeSQLFile(java.io.File file) throws Exception {
		sc.executeSQLFile(file);
	}

	public ArrayList<Llista> getAllLlistes() { return sc.getAllLlistes(); }
	public int createLlista(String nom) throws SQLException { return sc.createLlista(nom); }
	public void deleteLlista(int id) throws SQLException { sc.deleteLlista(id); }
	public int getCountInLlista(int llistaId) { return sc.getCountInLlista(llistaId); }
	public java.util.List<Object[]> getAllLlibreLlista() { return sc.getAllLlibreLlista(); }
	public java.util.List<Object[]> getAllPrestecs() { return sc.getAllPrestecs(); }
	public ArrayList<Llibre> getLlibresInLlista(int llistaId) { return sc.getLlibresInLlista(llistaId); }
	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { sc.addLlibreToLlista(isbn, llistaId, valoracio, llegit); }
	public void removeLlibreFromLlista(long isbn, int llistaId) throws SQLException { sc.removeLlibreFromLlista(isbn, llistaId); }
	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { sc.updateLlibreInLlista(isbn, llistaId, valoracio, llegit); }
	public ArrayList<Llista> getLlistesForLlibre(long isbn) { return sc.getLlistesForLlibre(isbn); }
	public void updateLlistaOrdre(int id, int ordre) throws java.sql.SQLException { sc.updateLlistaOrdre(id, ordre); }
	public void updateLlistaColor(int id, String color) throws java.sql.SQLException { sc.updateLlistaColor(id, color); }
	public ArrayList<Llibre> getRecentlyAdded(int n) { return sc.getRecentlyAdded(n); }
	public byte[] getLlibreBlob(long isbn) { return sc.getLlibreBlob(isbn); }
	public void addPrestec(long isbn, String nom) throws java.sql.SQLException { sc.addPrestec(isbn, nom); }
	public void returnPrestec(long isbn) throws java.sql.SQLException { sc.returnPrestec(isbn); }
	public java.util.Set<Long> getLoanedISBNs() { return sc.getLoanedISBNs(); }
	public void clearAllData() throws java.sql.SQLException { sc.clearAllData(); }
	public long getDbSizeBytes() { return sc.getDbSizeBytes(); }

	public ArrayList<Tag> getAllTags() { return sc.getAllTags(); }
	public int createTag(String nom) throws java.sql.SQLException { return sc.createTag(nom); }
	public void deleteTag(int id) throws java.sql.SQLException { sc.deleteTag(id); }
	public ArrayList<Tag> getTagsForLlibre(long isbn) { return sc.getTagsForLlibre(isbn); }
	public void addLlibreToTag(long isbn, int tagId) throws java.sql.SQLException { sc.addLlibreToTag(isbn, tagId); }
	public void removeLlibreFromTag(long isbn, int tagId) throws java.sql.SQLException { sc.removeLlibreFromTag(isbn, tagId); }
	public java.util.List<Object[]> getAllLlibreTag() { return sc.getAllLlibreTag(); }
	public java.util.Set<Long> getLlibresWithTag(int tagId) { return sc.getLlibresWithTag(tagId); }
	public java.util.List<Object[]> getAllAutors() { return sc.getAllAutors(); }
	public java.util.List<Object[]> getAllLlibreAutor() { return sc.getAllLlibreAutor(); }
	public java.util.List<String> getDistinctValues(String column) { return sc.getDistinctValues(column); }
	public java.util.List<String> getDistinctAutorNames() { return sc.getDistinctAutorNames(); }
}
