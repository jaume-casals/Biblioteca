
package persistencia.internal;

import java.sql.SQLException;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.config.Configuracio;
import persistencia.dao.AutorDao;
import persistencia.dao.LlibreBlobDao;
import persistencia.dao.LlibreDaoCore;
import persistencia.dao.LlibreLecturaDao;
import persistencia.dao.LlibreSearchDao;
import persistencia.dao.LlistaDao;
import persistencia.dao.PrestecDao;
import persistencia.dao.TagDao;
import persistencia.row.LlibreAutorRow;
import persistencia.row.LecturaRow;

import persistencia.row.AutorRow;
import persistencia.row.LlibreLlistaRow;
import persistencia.row.LlibreTagRow;
import persistencia.row.PrestecEndarrerit;
import persistencia.row.PrestecRow;
/**
 * Façana singleton sobre tots els DAO. Tots els mètodes públics són {@code synchronized}
 * perquè els consumidors (ControladorDomini i la capa d'API) obtinguin accés thread-safe
 * sense necessitat de bloquejar els DAO individuals.
 *
 * <p>Aquest embolcall existeix per centralitzar el cicle de vida (singleton, reinicialització
 * de test, canvi de perfil, ganxo de tancada) i per donar a {@code ControladorDomini} una sola
 * dependència. Els DAO no es sincronitzen individualment — cada ruta de crida passa per un
 * mètode {@code synchronized} aquí, de manera que un {@code synchronized} a nivell de DAO
 * seria un doble bloqueig redundant.
 *
 * <p>Cicle de vida: {@link #getInstance()} obre una connexió JDBC i inicialitza els DAO;
 * {@link #reinicialitzarForTest()} buida les dades i descarta la instància (entre grups de test);
 * {@link #reinicialitzarForProfileSwitch()} tanca la connexió (canvi de perfil H2 ↔ MariaDB).
 *
 * <p><b>TODO (perf):</b> substituir el {@code synchronized} per un
 * {@link java.util.concurrent.locks.ReadWriteLock} (lectures concurrents, escriptures
 * exclusives). Conversió mecànica però tediosa.
 *
 * <p><b>Contracte de concurrència:</b> tot l'accés als DAO ha de passar per aquesta
 * façana; els DAO individuals <em>no</em> són thread-safe per si mateixos.
 */
public class ControladorPersistencia {

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ControladorPersistencia.class.getName());

	private static volatile ControladorPersistencia inst;
	private static final java.util.concurrent.atomic.AtomicBoolean SHUTDOWN_HOOK_REGISTERED =
		new java.util.concurrent.atomic.AtomicBoolean(false);
	private final ConnexioServidor sc;
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
				LOG.log(java.util.logging.Level.WARNING, "Avís: no s'han pogut netejar les dades de test", e);
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
		sc = new ConnexioServidor();
		sc.crearDatabase(cfg);
		java.sql.Connection con = sc.obtenirConnexio();
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
					LOG.log(java.util.logging.Level.FINE, "Error en tancar la BD en sortir", e);
				}
			});
		}
	}

	public synchronized ArrayList<Llibre> obtenirAllLlibres() { return libreDaoCore.obtenirAll(); }

	/**
	 * Àlies de {@link #obtenirAllLlibres()} conservat per compatibilitat amb els consumidors.
	 * El DAO ja retorna la vista lleugera (sense descripcio/notes); els camps pesats
	 * s'han de carregar per llibre amb {@link #carregarHeavyFields}.
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
	 * Restaura atòmicament la base de dades des d'un fitxer SQL de còpia de seguretat:
	 * buida totes les dades i executa les sentències del fitxer dins d'una sola transacció
	 * JDBC. Si alguna sentència del fitxer falla, tota la transacció es reverteix i la
	 * base de dades queda intacta. Usat per {@link domini.facade.DelegatCopiaSeguretat#restaurarFromSQL(java.io.File)}
	 * per tancar la finestra "matar l'aplicació entre clearAllData() i executarSQLFile()".
	 * La instantània pre-restauració a un fitxer temporal és el camí de desfer visible
	 * per a l'usuari; aquest mètode és la xarxa de seguretat dins del procés.
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
	 * Contrapartida per lots de {@link #carregarHeavyFields(long, Llibre)}: un sol viatge
	 * d'anada i tornada per a N llibres en lloc de N. El consumidor subministra una llista
	 * d'ISBN I una funció de cerca que mapeja cada ISBN al {@link Llibre} en memòria a mutar.
	 * El DAO no depèn de l'estat en memòria; aquest és l'únic camí que tanca la precàrrega
	 * N+1 a {@link domini.facade.DelegatCopiaSeguretat}.
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
	 * Exposa la connexió JDBC subjacent per a consumidors que necessiten executar
	 * transaccions multi-sentència (p.ex. {@link herramienta.io.ServeiCopiaSeguretat}
	 * pren una instantània SERIALIZABLE de totes les taules). Els consumidors HAN DE
	 * sincronitzar sobre aquesta instància de {@code ControladorPersistencia} mentre
	 * tenen la connexió, perquè els DAO no observin un estat de transacció a mig
	 * configurar — cada mètode de DAO és {@code synchronized} sobre el mateix monitor,
	 * de manera que un bloc {@code synchronized(cp)} al voltant de la transacció
	 * manté l'estat de la connexió consistent.
	 */
	public java.sql.Connection obtenirConnexio() { return sc.obtenirConnexio(); }
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
	public synchronized int obtenirCountInLlista(int llistaId) { return llistaDao.obtenirRecompte(llistaId); }
	public synchronized java.util.Map<Integer,Integer> obtenirAllCountsInLlistes() { return llistaDao.obtenirAllCounts(); }
	public synchronized java.util.List<persistencia.row.LlibreLlistaRow> obtenirAllLlibreLlista() { return llistaDao.obtenirAllLlibreLlista(); }
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
	public synchronized java.util.List<persistencia.row.LlibreTagRow> obtenirAllLlibreTag() { return tagDao.obtenirAllLlibreTag(); }
	public synchronized java.util.Set<Long> obtenirLlibresWithTag(int tagId) { return tagDao.obtenirLlibresWithTag(tagId); }
	public synchronized java.util.List<String> obtenirDistinctValues(String column) { return tagDao.obtenirDistinctValues(column); }
	public synchronized java.util.List<String> obtenirDistinctAutorNames() { return autorDao.obtenirDistinctAutorNames(); }

	public synchronized java.util.List<persistencia.row.PrestecRow> obtenirAllPrestecs() { return prestecDao.obtenirAll(); }
	public synchronized java.util.List<persistencia.row.PrestecRow> obtenirAllActiveLoans() {
		return prestecDao.obtenirActiveLoans();
	}
	public synchronized java.util.List<persistencia.row.PrestecRow> obtenirLoansForIsbn(long isbn) { return prestecDao.obtenirForIsbn(isbn); }
	public synchronized java.util.List<persistencia.row.PrestecEndarrerit> obtenirAllOverdueLoans(int days) { return prestecDao.obtenirOverdue(days); }
	public synchronized void afegirPrestec(long isbn, String nom) throws java.sql.SQLException { prestecDao.add(isbn, nom); }
	public synchronized void returnPrestec(long isbn) throws java.sql.SQLException { prestecDao.returnLoan(isbn); }
	public synchronized java.util.Set<Long> obtenirLoanedISBNs() { return prestecDao.obtenirLoanedISBNs(); }
	public synchronized int comptarLoans(long isbn) { return prestecDao.count(isbn); }

	public synchronized java.util.List<persistencia.row.AutorRow> obtenirAllAutorRows() {
		return autorDao.obtenirAll();
	}

	/** Captura atòmica de totes les files relacionals necessàries per
	 *  produir un fitxer de còpia de seguretat coherent. Totes les
	 *  consultes es fan sota el monitor d'aquesta instància — un fil
	 *  d'edició concurrent (EDT) que intenti mutar qualsevol taula
	 *  durant la captura esperarà fins que la captura acabi, de
	 *  manera que el fitxer resultant no pot contenir files
	 *  {@code llibre_autor}/{@code llibre_llista}/{@code llibre_tag}
	 *  amb ISBN absent del {@code Llibre} capturat. */
	public BackupSnapshot snapshotForBackup() {
		synchronized (this) {
			BackupSnapshot s = new BackupSnapshot();
			s.bib = libreDaoCore.obtenirAll();
			s.llistes = llistaDao.obtenirAll();
			s.tags = tagDao.obtenirAll();
			s.autors = autorDao.obtenirAll();
			s.llibreAutors = autorDao.obtenirAllLlibreAutor();
			s.llibreLlistes = llistaDao.obtenirAllLlibreLlista();
			s.llibreTags = tagDao.obtenirAllLlibreTag();
			s.prestecs = prestecDao.obtenirAll();
			s.lectures = libreLecturaDao.obtenirAllLectures();
			return s;
		}
	}

	public static final class BackupSnapshot {
		public java.util.ArrayList<Llibre> bib;
		public java.util.ArrayList<Llista> llistes;
		public java.util.ArrayList<Tag> tags;
		public java.util.List<persistencia.row.AutorRow> autors;
		public java.util.List<persistencia.row.LlibreAutorRow> llibreAutors;
		public java.util.List<persistencia.row.LlibreLlistaRow> llibreLlistes;
		public java.util.List<persistencia.row.LlibreTagRow> llibreTags;
		public java.util.List<persistencia.row.PrestecRow> prestecs;
		public java.util.List<persistencia.row.LecturaRow> lectures;
	}
	/** @deprecated usa {@link #obtenirAllAutorRows()} — conservat per a consumidors que necessiten la forma Object[]. */
	@Deprecated
	public synchronized java.util.List<Object[]> obtenirAllAutors() {
		return autorDao.obtenirAll().stream()
				.map(r -> new Object[]{r.id(), r.nom()})
				.collect(java.util.stream.Collectors.toList());
	}
	/** @deprecated usa {@link #obtenirAllLlibreAutorRows()} — conservat per a consumidors que necessiten la forma Object[]. */
	@Deprecated
	public synchronized java.util.List<Object[]> obtenirAllLlibreAutor() {
		return autorDao.obtenirAllLlibreAutor().stream()
				.map(r -> new Object[]{r.isbn(), r.autorId()})
				.collect(java.util.stream.Collectors.toList());
	}

	public synchronized java.util.List<LlibreAutorRow> obtenirAllLlibreAutorRows() {
		return autorDao.obtenirAllLlibreAutor();
	}
	/** Només per a còpia/exportació — sense consumidor d'IU encara. */
	public synchronized java.util.List<LecturaRow> obtenirAllLectures() { return libreLecturaDao.obtenirAllLectures(); }

	public synchronized ArrayList<Llibre> cercarLlibres(domini.LlibreFilter f, int offset, int pageSize) {
		return libreSearchDao.search(f, offset, pageSize);
	}

	public synchronized ArrayList<Llibre> cercarAll(int offset, int pageSize) {
		return libreSearchDao.search(domini.LlibreFilter.empty(), offset, pageSize);
	}
}
