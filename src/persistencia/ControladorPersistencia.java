
package persistencia;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.Config;

public class ControladorPersistencia {

	private static volatile ControladorPersistencia inst;
	private final ServerConect sc;
	private final LlibreDao libreDao;
	private final LlistaDao llistaDao;
	private final TagDao tagDao;
	private final PrestecDao prestecDao;
	private final AutorDao autorDao;

	public static synchronized ControladorPersistencia getInstance() {
		if (ControladorPersistencia.inst == null)
			ControladorPersistencia.inst = new ControladorPersistencia();
		return ControladorPersistencia.inst;
	}

	public static void resetForTest() {
		if (inst != null) {
			try { inst.libreDao.clearAllData(); } catch (Exception ignored) {}
		}
		inst = null;
	}

	public static void resetForProfileSwitch() {
		if (inst != null) {
			try { inst.sc.closeConection(); } catch (Exception ignored) {}
		}
		inst = null;
	}

	private ControladorPersistencia() {
		String testUrl = System.getProperty("biblioteca.h2.url");
		ConnectionConfig cfg = new ConnectionConfig(
			testUrl != null ? "h2" : Config.getDbType(),
			Config.getDbHost(),
			Config.getDbUser(),
			Config.getDbPassword(),
			Config.getDbProfile(),
			testUrl
		);
		sc = new ServerConect();
		sc.createDatabase(cfg);
		java.sql.Connection con = sc.getConnection();
		libreDao  = new LlibreDao(con);
		llistaDao = new LlistaDao(con);
		tagDao    = new TagDao(con);
		prestecDao = new PrestecDao(con);
		autorDao  = new AutorDao(con);
	}

	public ArrayList<Llibre> getAllLlibres() { return libreDao.getAll(); }

	public void replaceAllLlibres(ArrayList<Llibre> llibres) throws java.sql.SQLException {
		libreDao.clearAllData();
		for (Llibre l : llibres) libreDao.insert(l);
	}

	public void afegirLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.insert(llibre); }
	public void eliminarLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.delete(llibre); }
	public void eliminarLlibre(long ISBN) throws java.sql.SQLException { libreDao.delete(ISBN); }
	public void executeSQLFile(java.io.File file) throws Exception { libreDao.executeSQLFile(file); }
	public void updateLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.update(llibre); }
	public ArrayList<Llibre> getRecentlyAdded(int n) { return libreDao.getRecentlyAdded(n); }
	public byte[] getLlibreBlob(long isbn) { return libreDao.getBlob(isbn); }
	public void setLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException { libreDao.setBlob(isbn, blob); }
	public void clearAllData() throws java.sql.SQLException { libreDao.clearAllData(); }
	public long getDbSizeBytes() { return libreDao.getDbSizeBytes(); }
	public int countLlibres() { return libreDao.count(); }

	public ArrayList<Llista> getAllLlistes() { return llistaDao.getAll(); }
	public int createLlista(String nom) throws SQLException { return llistaDao.create(nom); }
	public void deleteLlista(int id) throws SQLException { llistaDao.delete(id); }
	public int getCountInLlista(int llistaId) { return llistaDao.getCount(llistaId); }
	public java.util.List<Object[]> getAllLlibreLlista() { return llistaDao.getAllLlibreLlista(); }
	public ArrayList<Llibre> getLlibresInLlista(int llistaId) { return llistaDao.getLlibres(llistaId); }
	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.addLlibre(isbn, llistaId, valoracio, llegit); }
	public void removeLlibreFromLlista(long isbn, int llistaId) throws SQLException { llistaDao.removeLlibre(isbn, llistaId); }
	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.updateLlibre(isbn, llistaId, valoracio, llegit); }
	public ArrayList<Llista> getLlistesForLlibre(long isbn) { return llistaDao.getLlistesForLlibre(isbn); }
	public void updateLlistaOrdre(int id, int ordre) throws java.sql.SQLException { llistaDao.updateOrdre(id, ordre); }
	public void updateLlistaColor(int id, String color) throws java.sql.SQLException { llistaDao.updateColor(id, color); }

	public ArrayList<Tag> getAllTags() { return tagDao.getAll(); }
	public int createTag(String nom) throws java.sql.SQLException { return tagDao.create(nom); }
	public void deleteTag(int id) throws java.sql.SQLException { tagDao.delete(id); }
	public ArrayList<Tag> getTagsForLlibre(long isbn) { return tagDao.getForLlibre(isbn); }
	public void addLlibreToTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.addToLlibre(isbn, tagId); }
	public void removeLlibreFromTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.removeFromLlibre(isbn, tagId); }
	public java.util.List<Object[]> getAllLlibreTag() { return tagDao.getAllLlibreTag(); }
	public java.util.Set<Long> getLlibresWithTag(int tagId) { return tagDao.getLlibresWithTag(tagId); }
	public java.util.List<String> getDistinctValues(String column) { return tagDao.getDistinctValues(column); }
	public java.util.List<String> getDistinctAutorNames() { return tagDao.getDistinctAutorNames(); }

	public java.util.List<Object[]> getAllPrestecs() { return prestecDao.getAll(); }
	public java.util.List<Object[]> getLoansForIsbn(long isbn) { return prestecDao.getForIsbn(isbn); }
	public java.util.List<Object[]> getAllOverdueLoans(int days) { return prestecDao.getOverdue(days); }
	public void addPrestec(long isbn, String nom) throws java.sql.SQLException { prestecDao.add(isbn, nom); }
	public void returnPrestec(long isbn) throws java.sql.SQLException { prestecDao.returnLoan(isbn); }
	public java.util.Set<Long> getLoanedISBNs() { return prestecDao.getLoanedISBNs(); }

	public java.util.List<Object[]> getAllAutors() { return autorDao.getAll(); }
	public java.util.List<Object[]> getAllLlibreAutor() { return autorDao.getAllLlibreAutor(); }

	public ArrayList<Llibre> searchLlibres(domini.LlibreFilter f, int offset, int pageSize) {
		return libreDao.search(f, offset, pageSize);
	}
}
