
package persistencia;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.Configuracio;

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

	public static synchronized void reinicialitzarForTest() {
		if (inst != null) {
			try { inst.libreDaoCore.netejarAllData(); } catch (Exception e) {
				LOG.log(java.util.logging.Level.WARNING, "Warning: failed to clear test data", e);
			}
		}
		inst = null;
	}

	public static synchronized void reinicialitzarForProfileSwitch() {
		if (inst != null) {
			try { inst.sc.tancarConnection(); } catch (Exception ignored) {}
		}
		inst = null;
	}

	private ControladorPersistencia() {
		String testUrl = System.getProperty("biblioteca.h2.url");
		ConnectionConfig cfg = new ConnectionConfig(
			testUrl != null ? "h2" : Configuracio.obtenirDbType(),
			Configuracio.obtenirDbHost(),
			Configuracio.obtenirDbUser(),
			Configuracio.obtenirDbPassword(),
			Configuracio.obtenirDbProfile(),
			testUrl
		);
		sc = new ServerConect();
		sc.crearDatabase(cfg);
		java.sql.Connection con = sc.getConnection();
		libreDaoCore     = new LlibreDaoCore(con);
		libreBlobDao     = new LlibreBlobDao(con);
		libreSearchDao   = new LlibreSearchDao(con);
		libreLecturaDao  = new LlibreLecturaDao(con);
		llistaDao = new LlistaDao(con);
		tagDao    = new TagDao(con);
		prestecDao = new PrestecDao(con);
		autorDao  = new AutorDao(con);
		registrarShutdownHook();
	}

	private void registrarShutdownHook() {
		if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
			main.ShutdownHooks.register(() -> {
				try { sc.tancarConnection(); } catch (Exception e) {
					LOG.log(java.util.logging.Level.FINE, "Error closing DB on shutdown", e);
				}
			});
		}
	}

	public synchronized ArrayList<Llibre> obtenirAllLlibres() { return libreDaoCore.obtenirAll(); }

	/**
	 * Alias for {@link #getAllLlibres()} retained for caller compatibility.
	 * The DAO already returns the light view (no descripcio/notes); heavy
	 * fields must be loaded per-book via {@link #loadHeavyFields}.
	 */
	public synchronized ArrayList<Llibre> obtenirAllLlibresSummary() { return libreDaoCore.obtenirAll(); }

	public synchronized void afegirLlibre(Llibre llibre) throws java.sql.SQLException { libreDaoCore.insert(llibre); }
	public synchronized void eliminarLlibre(Llibre llibre) throws java.sql.SQLException {
		libreDaoCore.delete(llibre);
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized void eliminarLlibre(long ISBN) throws java.sql.SQLException {
		libreDaoCore.delete(ISBN);
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized void executarSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException { libreDaoCore.executarSQLFile(file); }
	/**
	 * Atomically restore the database from a SQL backup file: clears
	 * all data and executes the file's statements inside a single JDBC
	 * transaction. If any statement in the file fails, the entire
	 * transaction is rolled back and the database is unchanged. Used by
	 * {@link domini.facade.BackupDelegate#restoreFromSQL(java.io.File)} to
	 * close the "kill the app between clearAllData() and executeSQLFile()"
	 * window. The pre-restore snapshot to a temp file is the user-facing
	 * undo path; this method is the in-process safety net.
	 */
	public synchronized void restaurarFromSQLFile(java.io.File file) throws java.io.IOException, java.sql.SQLException {
		libreDaoCore.restaurarFromSQL(file);
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized void actualitzarLlibre(Llibre llibre) throws java.sql.SQLException { libreDaoCore.update(llibre); }
	public synchronized ArrayList<Llibre> obtenirRecentlyAdded(int n) { return libreDaoCore.obtenirRecentlyAdded(n); }
	public synchronized byte[] obtenirLlibreBlob(long isbn) { return libreBlobDao.obtenirBlob(isbn); }

	public synchronized void carregarHeavyFields(long isbn, domini.Llibre target) {
		libreBlobDao.carregarHeavyFields(isbn, target);
	}
	/**
	 * Batched counterpart to {@link #loadHeavyFields(long, Llibre)}: one
	 * round-trip for N books instead of N. The caller supplies an ISBN
	 * list AND a lookup function that maps each ISBN to the in-memory
	 * {@link Llibre} to mutate. The DAO does not depend on the
	 * in-memory state; this is the only path that closes the N+1
	 * pre-load in {@link domini.facade.BackupDelegate}.
	 */
	public synchronized void carregarHeavyFieldsBatched(java.util.List<Long> isbns,
	                                                java.util.function.LongFunction<domini.Llibre> targetLookup) {
		java.util.Map<Long, domini.Llibre> targets = new java.util.HashMap<>();
		for (Long isbn : isbns) {
			domini.Llibre l = targetLookup.apply(isbn);
			if (l != null) targets.put(isbn, l);
		}
		libreBlobDao.carregarHeavyFieldsBatched(isbns, targets);
	}
	public synchronized void posarLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException { libreBlobDao.setBlob(isbn, blob); }
	/**
	 * Exposes the underlying JDBC connection for callers that need to run
	 * multi-statement transactions (e.g. {@link herramienta.BackupService}
	 * takes a SERIALIZABLE snapshot of all tables). Callers MUST
	 * synchronize on this {@code ControladorPersistencia} instance while
	 * holding the connection so the DAOs do not observe a half-set
	 * transaction state — every DAO method is {@code synchronized} on
	 * this same monitor, so a {@code synchronized(cp)} block around the
	 * transaction keeps the connection state consistent.
	 */
	public java.sql.Connection getConnection() { return sc.getConnection(); }
	public synchronized void netejarAllData() throws java.sql.SQLException {
		libreDaoCore.netejarAllData();
		tagDao.invalidateLlibreTagCache();
	}
	public synchronized long obtenirDbSizeBytes() { return libreDaoCore.obtenirDbSizeBytes(); }
	public synchronized int comptarLlibres() { return libreDaoCore.count(); }

	public synchronized ArrayList<Llista> obtenirAllLlistes() { return llistaDao.obtenirAll(); }
	public synchronized int crearLlista(String nom) throws SQLException { return llistaDao.create(nom); }
	public synchronized void eliminarLlista(int id) throws SQLException { llistaDao.delete(id); }
	public synchronized void reanomenarLlista(int id, String newNom) throws SQLException { llistaDao.actualitzarNom(id, newNom); }
	public synchronized int obtenirCountInLlista(int llistaId) { return llistaDao.getCount(llistaId); }
	public synchronized java.util.Map<Integer,Integer> obtenirAllCountsInLlistes() { return llistaDao.obtenirAllCounts(); }
	public synchronized java.util.List<persistencia.LlibreLlistaRow> obtenirAllLlibreLlista() { return llistaDao.obtenirAllLlibreLlista(); }
	public synchronized ArrayList<Llibre> obtenirLlibresInLlista(int llistaId) { return llistaDao.obtenirLlibres(llistaId); }
	public synchronized java.util.Set<Long> obtenirISBNsInLlista(int llistaId) { return llistaDao.obtenirISBNsInLlista(llistaId); }
	public synchronized void afegirLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.afegirLlibre(isbn, llistaId, valoracio, llegit); }
	public synchronized void eliminarLlibreFromLlista(long isbn, int llistaId) throws SQLException { llistaDao.eliminarLlibre(isbn, llistaId); }
	public synchronized void actualitzarLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException { llistaDao.actualitzarLlibre(isbn, llistaId, valoracio, llegit); }
	public synchronized ArrayList<Llista> obtenirLlistesForLlibre(long isbn) { return llistaDao.obtenirLlistesForLlibre(isbn); }
	public synchronized java.util.List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) { return llistaDao.obtenirLlistesForLlibreContext(isbn); }
	public synchronized void actualitzarLlistaOrdre(int id, int ordre) throws java.sql.SQLException { llistaDao.actualitzarOrdre(id, ordre); }
	public synchronized void actualitzarLlistaColor(int id, String color) throws java.sql.SQLException { llistaDao.actualitzarColor(id, color); }

	public synchronized ArrayList<Tag> obtenirAllTags() { return tagDao.obtenirAll(); }
	public synchronized int crearTag(String nom) throws java.sql.SQLException { return tagDao.create(nom); }
	public synchronized void eliminarTag(int id) throws java.sql.SQLException { tagDao.delete(id); }
	public synchronized void reanomenarTag(int id, String nom) throws java.sql.SQLException { tagDao.rename(id, nom); }
	public synchronized ArrayList<Tag> obtenirTagsForLlibre(long isbn) { return tagDao.obtenirForLlibre(isbn); }
	public synchronized void afegirLlibreToTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.afegirToLlibre(isbn, tagId); }
	public synchronized void eliminarLlibreFromTag(long isbn, int tagId) throws java.sql.SQLException { tagDao.eliminarFromLlibre(isbn, tagId); }
	public synchronized java.util.List<persistencia.LlibreTagRow> obtenirAllLlibreTag() { return tagDao.obtenirAllLlibreTag(); }
	public synchronized java.util.Set<Long> obtenirLlibresWithTag(int tagId) { return tagDao.obtenirLlibresWithTag(tagId); }
	public synchronized java.util.List<String> obtenirDistinctValues(String column) { return tagDao.obtenirDistinctValues(column); }
	public synchronized java.util.List<String> obtenirDistinctAutorNames() { return autorDao.obtenirDistinctAutorNames(); }

	public synchronized java.util.List<persistencia.PrestecRow> obtenirAllPrestecs() { return prestecDao.obtenirAll(); }
	public synchronized java.util.List<persistencia.PrestecRow> obtenirAllActiveLoans() {
		return prestecDao.obtenirActiveLoans();
	}
	public synchronized java.util.List<persistencia.PrestecRow> obtenirLoansForIsbn(long isbn) { return prestecDao.obtenirForIsbn(isbn); }
	public synchronized java.util.List<persistencia.OverdueLoan> obtenirAllOverdueLoans(int days) { return prestecDao.obtenirOverdue(days); }
	public synchronized void afegirPrestec(long isbn, String nom) throws java.sql.SQLException { prestecDao.add(isbn, nom); }
	public synchronized void returnPrestec(long isbn) throws java.sql.SQLException { prestecDao.returnLoan(isbn); }
	public synchronized java.util.Set<Long> obtenirLoanedISBNs() { return prestecDao.obtenirLoanedISBNs(); }
	public synchronized int comptarLoans(long isbn) { return prestecDao.count(isbn); }

	public synchronized java.util.List<persistencia.AutorRow> obtenirAllAutorRows() {
		return autorDao.obtenirAll();
	}
	/** @deprecated use {@link #getAllAutorRows()} — kept for callers that need the Object[] shape. */
	@Deprecated
	public synchronized java.util.List<Object[]> obtenirAllAutors() {
		return autorDao.obtenirAll().stream()
				.map(r -> new Object[]{r.id(), r.nom()})
				.collect(java.util.stream.Collectors.toList());
	}
	/** @deprecated use {@link #getAllLlibreAutorRows()} — kept for callers that need the Object[] shape. */
	@Deprecated
	public synchronized java.util.List<Object[]> obtenirAllLlibreAutor() {
		return autorDao.obtenirAllLlibreAutor().stream()
				.map(r -> new Object[]{r.isbn(), r.autorId()})
				.collect(java.util.stream.Collectors.toList());
	}

	public synchronized java.util.List<LlibreAutorRow> obtenirAllLlibreAutorRows() {
		return autorDao.obtenirAllLlibreAutor();
	}
	/** For backup/export only — no UI consumer yet. */
	public synchronized java.util.List<LecturaRow> obtenirAllLectures() { return libreLecturaDao.obtenirAllLectures(); }

	public synchronized ArrayList<Llibre> cercarLlibres(domini.LlibreFilter f, int offset, int pageSize) {
		return libreSearchDao.search(f, offset, pageSize);
	}

	public synchronized ArrayList<Llibre> cercarAll(int offset, int pageSize) {
		return libreSearchDao.search(domini.LlibreFilter.empty(), offset, pageSize);
	}
}
