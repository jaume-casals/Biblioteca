
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
 * <p>This wrapper exists to centralise lifecycle (singleton, test reset, profile
 * switch, shutdown hook) and to give {@code ControladorDomini} a single dependency.
 * DAOs are not individually synchronised — every call path goes through a
 * {@code synchronized} method here, so DAO-level {@code synchronized} would be
 * redundant double-locking.
 *
 * <p>Lifecycle: {@link #getInstance()} opens a JDBC connection and initialises DAOs;
 * {@link #resetForTest()} clears data and discards the instance (between test groups);
 * {@link #resetForProfileSwitch()} closes the connection (H2 ↔ MariaDB profile switch).
 *
 * <p><b>TODO (perf):</b> substituir el {@code synchronized} per un
 * {@link java.util.concurrent.locks.ReadWriteLock} (lectures concurrents, escriptures
 * exclusives). Conversió mecànica però tediosa.
 */
public class ControladorPersistencia {

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ControladorPersistencia.class.getName());

	private static volatile ControladorPersistencia inst;
	private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
		new java.util.concurrent.atomic.AtomicBoolean(false);
	private final ServerConect sc;
	private final LlibreDaoCore libreDaoCore;
	private final LlibreBlobDao libreBlobDao;
	private final LlibreSearchDao libreSearchDao;
	private final LlibreLecturaDao libreLecturaDao;
	private final LlistaDao llistaDao;
	private final TagDao tagDao;
	private final PrestecDao prestecDao;
	private final AutorDao autorDao;

	public static synchronized ControladorPersistencia getInstance() {
		if (ControladorPersistencia.inst == null)
			ControladorPersistencia.inst = new ControladorPersistencia();
		return ControladorPersistencia.inst;
	}

	public static synchronized void resetForTest() {
		if (inst != null) {
			try { inst.libreDaoCore.clearAllData(); } catch (Exception e) {
				LOG.log(java.util.logging.Level.WARNING, "Warning: failed to clear test data", e);
			}
		}
		inst = null;
	}

	public static synchronized void resetForProfileSwitch() {
		if (inst != null) {
			try { inst.sc.closeConnection(); } catch (Exception ignored) {}
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
		libreDaoCore     = new LlibreDaoCore(con);
		libreBlobDao     = new LlibreBlobDao(con);
		libreSearchDao   = new LlibreSearchDao(con);
		libreLecturaDao  = new LlibreLecturaDao(con);
		llistaDao = new LlistaDao(con);
		tagDao    = new TagDao(con);
		prestecDao = new PrestecDao(con);
		autorDao  = new AutorDao(con);
		registerShutdownHook();
	}

	private void registerShutdownHook() {
		if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
			main.ShutdownHooks.register(() -> {
				try { sc.closeConnection(); } catch (Exception e) {
					LOG.log(java.util.logging.Level.FINE, "Error closing DB on shutdown", e);
				}
			});
		}
	}

	public synchronized ArrayList<Llibre> getAllLlibres() { return libreDaoCore.getAll(); }

	/**
	 * Alias for {@link #getAllLlibres()} retained for caller compatibility.
	 * The DAO already returns the light view (no descripcio/notes); heavy
	 * fields must be loaded per-book via {@link #loadHeavyFields}.
	 */
	public synchronized ArrayList<Llibre> getAllLlibresSummary() { return libreDaoCore.getAll(); }

	public synchronized void afegirLlibre(Llibre llibre) throws java.sql.SQLException { libreDaoCore.insert(llibre); }
	public synchronized void eliminarLlibre(Llibre llibre) throws java.sql.SQLException {
		libreDaoCore.delete(llibre);
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized void eliminarLlibre(long ISBN) throws java.sql.SQLException {
		libreDaoCore.delete(ISBN);
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized void executeSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException { libreDaoCore.executeSQLFile(file); }
	public synchronized void updateLlibre(Llibre llibre) throws java.sql.SQLException { libreDaoCore.update(llibre); }
	public synchronized ArrayList<Llibre> getRecentlyAdded(int n) { return libreDaoCore.getRecentlyAdded(n); }
	public synchronized byte[] getLlibreBlob(long isbn) { return libreBlobDao.getBlob(isbn); }

	public synchronized void loadHeavyFields(long isbn, domini.Llibre target) {
		libreBlobDao.loadHeavyFields(isbn, target);
	}
	public synchronized void setLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException { libreBlobDao.setBlob(isbn, blob); }
	public synchronized void clearAllData() throws java.sql.SQLException {
		libreDaoCore.clearAllData();
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized long getDbSizeBytes() { return libreDaoCore.getDbSizeBytes(); }
	public synchronized int countLlibres() { return libreDaoCore.count(); }

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
	public synchronized java.util.List<domini.LlibreLlistaContext> getLlistesForLlibreContext(long isbn) { return llistaDao.getLlistesForLlibreContext(isbn); }
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

	public synchronized java.util.List<persistencia.AutorRow> getAllAutorRows() {
		return autorDao.getAll();
	}
	/** @deprecated use {@link #getAllAutorRows()} — kept for callers that need the Object[] shape. */
	@Deprecated
	public synchronized java.util.List<Object[]> getAllAutors() {
		return autorDao.getAll().stream()
				.map(r -> new Object[]{r.id(), r.nom()})
				.collect(java.util.stream.Collectors.toList());
	}
	/** @deprecated use {@link #getAllLlibreAutorRows()} — kept for callers that need the Object[] shape. */
	@Deprecated
	public synchronized java.util.List<Object[]> getAllLlibreAutor() {
		return autorDao.getAllLlibreAutor().stream()
				.map(r -> new Object[]{r.isbn(), r.autorId()})
				.collect(java.util.stream.Collectors.toList());
	}

	public synchronized java.util.List<LlibreAutorRow> getAllLlibreAutorRows() {
		return autorDao.getAllLlibreAutor();
	}
	/** For backup/export only — no UI consumer yet. */
	public synchronized java.util.List<LecturaRow> getAllLectures() { return libreLecturaDao.getAllLectures(); }

	public synchronized ArrayList<Llibre> searchLlibres(domini.LlibreFilter f, int offset, int pageSize) {
		return libreSearchDao.search(f, offset, pageSize);
	}

	public synchronized ArrayList<Llibre> searchAll(int offset, int pageSize) {
		return libreSearchDao.search(domini.LlibreFilter.empty(), offset, pageSize);
	}
}
