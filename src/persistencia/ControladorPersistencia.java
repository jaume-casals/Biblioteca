
package persistencia;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.Config;

/**
 * Singleton facade over all DAOs. All public methods are {@code synchronized} so that
 * callers (ControladorDomini and the API layer) get thread-safe access without needing
 * to lock individual DAOs.
 *
 * <p>Lifecycle:
 * <ul>
 *   <li>{@link #getInstance()} — creates the singleton, opens a JDBC connection and
 *       initialises DAOs. Called once at startup.</li>
 *   <li>{@link #resetForTest()} — clears all data and discards the instance; for
 *       integration tests that need a clean slate between test groups.</li>
 *   <li>{@link #resetForProfileSwitch()} — closes the JDBC connection and discards the
 *       instance; called when the user switches DB profile (H2 ↔ MariaDB).</li>
 * </ul>
 *
 * <p>DAOs are <em>not</em> individually synchronised because every call path goes through
 * a {@code synchronized} method on this class. Adding {@code synchronized} on DAO methods
 * would be redundant double-locking.
 */
public class ControladorPersistencia {

	private static ControladorPersistencia inst;
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
            try { inst.libreDao.clearAllData(); } catch (Exception e) {
                System.err.println("Warning: failed to clear test data: " + e.getMessage());
            }
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

	public synchronized ArrayList<Llibre> getAllLlibres() { return libreDao.getAll(); }

	public synchronized void afegirLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.insert(llibre); }
	public synchronized void eliminarLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.delete(llibre); }
	public synchronized void eliminarLlibre(long ISBN) throws java.sql.SQLException { libreDao.delete(ISBN); }
	public synchronized void executeSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException { libreDao.executeSQLFile(file); }
	public synchronized void updateLlibre(Llibre llibre) throws java.sql.SQLException { libreDao.update(llibre); }
	public synchronized ArrayList<Llibre> getRecentlyAdded(int n) { return libreDao.getRecentlyAdded(n); }
	public synchronized byte[] getLlibreBlob(long isbn) { return libreDao.getBlob(isbn); }

	public synchronized void loadHeavyFields(long isbn, domini.Llibre target) {
		libreDao.loadHeavyFields(isbn, target);
	}
	public synchronized void setLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException { libreDao.setBlob(isbn, blob); }
	public synchronized void clearAllData() throws java.sql.SQLException { libreDao.clearAllData(); }
	public synchronized long getDbSizeBytes() { return libreDao.getDbSizeBytes(); }
	public synchronized int countLlibres() { return libreDao.count(); }

	public synchronized ArrayList<Llista> getAllLlistes() { return llistaDao.getAll(); }
	public synchronized int createLlista(String nom) throws SQLException { return llistaDao.create(nom); }
	public synchronized void deleteLlista(int id) throws SQLException { llistaDao.delete(id); }
	public synchronized void renameLlista(int id, String newNom) throws SQLException { llistaDao.updateNom(id, newNom); }
	public synchronized int getCountInLlista(int llistaId) { return llistaDao.getCount(llistaId); }
	public synchronized java.util.Map<Integer,Integer> getAllCountsInLlistes() { return llistaDao.getAllCounts(); }
	public synchronized java.util.List<persistencia.LlibreLlistaRow> getAllLlibreLlista() { return llistaDao.getAllLlibreLlista(); }
	public synchronized ArrayList<Llibre> getLlibresInLlista(int llistaId) { return llistaDao.getLlibres(llistaId); }
	public synchronized java.util.Set<Long> getISBNsInLlista(int llistaId) { return llistaDao.getISBNsInLlista(llistaId); }
	public synchronized void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.addLlibre(isbn, llistaId, valoracio, llegit); }
	public synchronized void removeLlibreFromLlista(long isbn, int llistaId) throws SQLException { llistaDao.removeLlibre(isbn, llistaId); }
	public synchronized void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.updateLlibre(isbn, llistaId, valoracio, llegit); }
	public synchronized ArrayList<Llista> getLlistesForLlibre(long isbn) { return llistaDao.getLlistesForLlibre(isbn); }
	public synchronized void updateLlistaOrdre(int id, int ordre) throws java.sql.SQLException { llistaDao.updateOrdre(id, ordre); }
	public synchronized void updateLlistaColor(int id, String color) throws java.sql.SQLException { llistaDao.updateColor(id, color); }

	public synchronized ArrayList<Tag> getAllTags() { return tagDao.getAll(); }
	public synchronized int createTag(String nom) throws java.sql.SQLException { return tagDao.create(nom); }
	public synchronized void deleteTag(int id) throws java.sql.SQLException { tagDao.delete(id); }
	public synchronized void renameTag(int id, String nom) throws java.sql.SQLException { tagDao.rename(id, nom); }
	public synchronized ArrayList<Tag> getTagsForLlibre(long isbn) { return tagDao.getForLlibre(isbn); }
	public synchronized void addLlibreToTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.addToLlibre(isbn, tagId); }
	public synchronized void removeLlibreFromTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.removeFromLlibre(isbn, tagId); }
	public synchronized java.util.List<persistencia.LlibreTagRow> getAllLlibreTag() { return tagDao.getAllLlibreTag(); }
	public synchronized java.util.Set<Long> getLlibresWithTag(int tagId) { return tagDao.getLlibresWithTag(tagId); }
	public synchronized java.util.List<String> getDistinctValues(String column) { return tagDao.getDistinctValues(column); }
	public synchronized java.util.List<String> getDistinctAutorNames() { return autorDao.getDistinctAutorNames(); }

	public synchronized java.util.List<persistencia.PrestecRow> getAllPrestecs() { return prestecDao.getAll(); }
	public synchronized java.util.List<persistencia.PrestecRow> getAllActiveLoans() {
		return prestecDao.getActiveLoans();
	}
	public synchronized java.util.List<persistencia.PrestecRow> getLoansForIsbn(long isbn) { return prestecDao.getForIsbn(isbn); }
	public synchronized java.util.List<Object[]> getAllOverdueLoans(int days) { return prestecDao.getOverdue(days); }
	public synchronized void addPrestec(long isbn, String nom) throws java.sql.SQLException { prestecDao.add(isbn, nom); }
	public synchronized void returnPrestec(long isbn) throws java.sql.SQLException { prestecDao.returnLoan(isbn); }
	public synchronized java.util.Set<Long> getLoanedISBNs() { return prestecDao.getLoanedISBNs(); }
	public synchronized int countLoans(long isbn) { return prestecDao.count(isbn); }

	public synchronized java.util.List<Object[]> getAllAutors() {
		return autorDao.getAll().stream()
				.map(r -> new Object[]{r.id(), r.nom()})
				.collect(java.util.stream.Collectors.toList());
	}
	public synchronized java.util.List<Object[]> getAllLlibreAutor() {
		return autorDao.getAllLlibreAutor().stream()
				.map(r -> new Object[]{r.isbn(), r.autorId()})
				.collect(java.util.stream.Collectors.toList());
	}

	public synchronized java.util.List<LlibreAutorRow> getAllLlibreAutorRows() {
		return autorDao.getAllLlibreAutor();
	}
	/** For backup/export only — no UI consumer yet. */
	public synchronized java.util.List<LecturaRow> getAllLectures() { return libreDao.getAllLectures(); }

	public synchronized ArrayList<Llibre> searchLlibres(domini.LlibreFilter f, int offset, int pageSize) {
		return libreDao.search(f, offset, pageSize);
	}

	public synchronized ArrayList<Llibre> searchAll(int offset, int pageSize) {
		return libreDao.search(domini.LlibreFilter.empty(), offset, pageSize);
	}
}
