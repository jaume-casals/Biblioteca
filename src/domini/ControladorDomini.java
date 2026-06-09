package domini;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import domini.facade.BackupDelegate;
import domini.facade.BookDelegate;
import domini.facade.LoanDelegate;
import domini.facade.ShelfDelegate;
import domini.facade.StateContext;
import domini.facade.StatsDelegate;
import domini.facade.TagDelegate;
import herramienta.BackupService;
import interficie.BibliotecaWriter;
import persistencia.ControladorPersistencia;
import persistencia.LecturaRow;
import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import persistencia.PrestecRow;

/**
 * Singleton facade over the library's in-memory state + persistence.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Singleton lifecycle ({@link #getInstance()}, {@link #resetForTest()},
 *       {@link #resetForProfileSwitch()}, {@link #create(ControladorPersistencia)}).</li>
 *   <li>Owns the {@link StateContext}: the three backing lists
 *       ({@code bib}, {@code llistes}, {@code tags}), the in-memory lock
 *       ({@code BIB_LOCK}), and the id-index maps.</li>
 *   <li>Delegates every business operation to one of six focused
 *       collaborator classes in {@code domini.facade} — Shelf, Tag,
 *       Loan, Book, Stats, Backup.</li>
 * </ul>
 *
 * <p>The public API of this class is unchanged: every method that existed
 * before the split still exists, with the same signature, same
 * exceptions, and same side effects. The only externally observable
 * change is the file's line count (637 → ~150).
 */
public class ControladorDomini implements BibliotecaWriter {

	private static ControladorDomini inst;

	private final StateContext state;
	private final BackupService backupService;
	private final ShelfDelegate shelves;
	private final TagDelegate tags;
	private final LoanDelegate loans;
	private final BookDelegate books;
	private final StatsDelegate stats;
	private final BackupDelegate backup;

	public static synchronized ControladorDomini getInstance() {
		if (inst == null) inst = new ControladorDomini(ControladorPersistencia.getInstance());
		return inst;
	}

	public static synchronized void resetForTest() { inst = null; }

	public static synchronized void resetForProfileSwitch() {
		inst = null;
		ControladorPersistencia.resetForProfileSwitch();
	}

	/**
	 * Crea una instància de ControladorDomini lligada a un ControladorPersistencia concret.
	 * Útil per a tests o per composició manual de dependències; la instància retornada
	 * no s'enregistra com a singleton — convé que el codi de producció segueixi usant
	 * {@link #getInstance()}.
	 */
	public static ControladorDomini create(ControladorPersistencia cp) {
		return new ControladorDomini(cp);
	}

	public ControladorDomini(ControladorPersistencia cp) {
		this.state = new StateContext(cp,
			new java.util.ArrayList<Llibre>(cp.getAllLlibres()),
			new java.util.ArrayList<>(cp.getAllLlistes()),
			new java.util.ArrayList<>(cp.getAllTags()));
		Collections.sort(this.state.bib(), BookDelegate.ISBN_COMPARATOR);
		this.state.rebuildIdIndexesLocked();
		this.backupService = new BackupService(cp);
		this.shelves = new ShelfDelegate(this.state);
		this.tags    = new TagDelegate(this.state);
		this.loans   = new LoanDelegate(this.state);
		this.books   = new BookDelegate(this.state);
		this.stats   = new StatsDelegate(this.state);
		this.backup  = new BackupDelegate(this.state, this.backupService);
		if (!"true".equals(System.getProperty("biblioteca.test"))) {
			backupService.scheduleAutoBackup();
		}
	}

	// ── Book / search / blob ──────────────────────────────────────────────────

	public java.util.List<Llibre> aplicarFiltres(LlibreFilter f)                                  { return books.aplicarFiltres(f); }
	public java.util.List<Llibre> aplicarFiltres(java.util.List<Llibre> font, LlibreFilter f)     { return books.aplicarFiltres(font, f); }
	public java.util.List<Llibre> searchLlibresSQL(LlibreFilter f)                                { return books.searchLlibresSQL(f); }
	public java.util.List<Llibre> getLlibresPage(int offset, int pageSize)                        { return books.getLlibresPage(offset, pageSize); }
	public int countLlibresDB()                                                                   { return books.countLlibresDB(); }
	public boolean isLargeLibrary()                                                               { return books.isLargeLibrary(); }
	public java.util.List<Llibre> getAllLlibres()                                                 { return books.getAllLlibres(); }
	public java.util.List<Llibre> getUnmodifiableLlibres()                                        { return books.getUnmodifiableLlibres(); }
	public java.util.List<Llibre> get10Llibres()                                                  { return books.get10Llibres(); }
	public java.util.List<Llibre> get100Llibres(int index)                                        { return books.get100Llibres(index); }
	public int maxIndex100Llibres()                                                               { return books.maxIndex100Llibres(); }
	public int getSize()                                                                         { return books.getSize(); }
	public void addLlibre(Llibre l)                                                               { books.addLlibre(l); }
	public void deleteLlibre(Llibre l)                                                            { books.deleteLlibre(l); }
	public void deleteLlibre(Long ISBN)                                                           { books.deleteLlibreByIsbn(ISBN); }
	public void updateLlibre(Llibre l)                                                            { books.updateLlibre(l); }
	public boolean existsLlibre(long ISBN)                                                        { return books.existsLlibre(ISBN); }
	public Llibre getLlibre(long ISBN)                                                            { return books.getLlibre(ISBN); }
	@Override public void loadHeavyFields(Llibre book)                                            { books.loadHeavyFields(book); }
	public java.util.List<Llibre> getRecentlyAdded()                                              { return books.getRecentlyAdded(); }
	public byte[] getLlibreBlob(long isbn)                                                        { return books.getLlibreBlob(isbn); }
	public void setLlibreBlob(long isbn, byte[] blob)                                             { books.setLlibreBlob(isbn, blob); }

	// ── Shelf ─────────────────────────────────────────────────────────────────

	public java.util.List<Llista> getAllLlistes()                                                 { return shelves.getAllLlistes(); }
	public Llista getLlistaById(int id) throws Exception                                          { return shelves.getLlistaById(id); }
	public Llista addLlista(String nom)                                                           { return shelves.addLlista(nom); }
	public void deleteLlista(Llista llista)                                                       { shelves.deleteLlista(llista); }
	public void renameLlista(int id, String newNom)                                               { shelves.renameLlista(id, newNom); }
	public int getCountInLlista(int llistaId)                                                     { return shelves.getCountInLlista(llistaId); }
	public java.util.Map<Integer, Integer> getAllCountsInLlistes()                                { return shelves.getAllCountsInLlistes(); }
	public java.util.List<Llibre> getLlibresInLlista(int llistaId)                                { return shelves.getLlibresInLlista(llistaId); }
	public java.util.List<Llista> getLlistesForLlibre(long isbn)                                  { return shelves.getLlistesForLlibre(isbn); }
	public java.util.List<LlibreLlistaContext> getLlistesForLlibreContext(long isbn)               { return shelves.getLlistesForLlibreContext(isbn); }
	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit)      { shelves.addLlibreToLlista(isbn, llistaId, valoracio, llegit); }
	public void removeLlibreFromLlista(long isbn, int llistaId)                                   { shelves.removeLlibreFromLlista(isbn, llistaId); }
	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit)  { shelves.updateLlibreInLlista(isbn, llistaId, valoracio, llegit); }
	public void moveLlistaUp(int id)                                                              { shelves.moveLlistaUp(id); }
	public void moveLlistaDown(int id)                                                            { shelves.moveLlistaDown(id); }
	public void setLlistaColor(int id, String color)                                              { shelves.setLlistaColor(id, color); }

	// ── Tag ───────────────────────────────────────────────────────────────────

	public java.util.List<Tag> getAllTags()                                                       { return tags.getAllTags(); }
	public Tag getTagById(int id) throws Exception                                                { return tags.getTagById(id); }
	public Tag addTag(String nom)                                                                 { return tags.addTag(nom); }
	public void deleteTag(Tag tag)                                                                { tags.deleteTag(tag); }
	public void renameTag(int id, String newNom)                                                  { tags.renameTag(id, newNom); }
	public java.util.Set<Long> getLlibresWithTag(int tagId)                                       { return tags.getLlibresWithTag(tagId); }
	public java.util.List<Tag> getTagsForLlibre(long isbn)                                        { return tags.getTagsForLlibre(isbn); }
	public void addLlibreToTag(long isbn, int tagId)                                              { tags.addLlibreToTag(isbn, tagId); }
	public void removeLlibreFromTag(long isbn, int tagId)                                         { tags.removeLlibreFromTag(isbn, tagId); }

	// ── Loan ──────────────────────────────────────────────────────────────────

	public void prestarLlibre(long isbn, String nom)                                              { loans.prestarLlibre(isbn, nom); }
	public void retornarLlibre(long isbn)                                                         { loans.retornarLlibre(isbn); }
	public java.util.Set<Long> getLoanedISBNs()                                                   { return loans.getLoanedISBNs(); }
	public java.util.List<PrestecRow> getAllActiveLoans()                                         { return loans.getAllActiveLoans(); }
	public java.util.List<PrestecRow> getLoansForIsbn(long isbn)                                  { return loans.getLoansForIsbn(isbn); }
	public java.util.List<Object[]> getAllOverdueLoans(int daysThreshold)                         { return loans.getAllOverdueLoans(daysThreshold); }
	public int countLoans(long isbn)                                                              { return loans.countLoans(isbn); }

	// ── Stats / autocomplete / backup payload ────────────────────────────────

	public java.util.List<String> getDistinctValues(String column)                                { return stats.getDistinctValues(column); }
	public java.util.List<String> getDistinctAutorNames()                                         { return stats.getDistinctAutorNames(); }
	public java.util.List<LlibreLlistaRow> getAllLlibreLlistaRows()                               { return stats.getAllLlibreLlistaRows(); }
	public java.util.List<LlibreTagRow>    getAllLlibreTagRows()                                  { return stats.getAllLlibreTagRows(); }
	public java.util.List<Object[]>        getAutorsData()                                        { return stats.getAutorsData(); }
	public java.util.List<Object[]>        getLlibreAutorData()                                   { return stats.getLlibreAutorData(); }
	public java.util.List<PrestecRow>      getAllPrestecs()                                       { return stats.getAllPrestecs(); }
	public java.util.List<LecturaRow>      getAllLecturesData()                                   { return stats.getAllLecturesData(); }
	public long getDbSizeBytes()                                                                  { return stats.getDbSizeBytes(); }

	// ── Backup / restore / clearAll ───────────────────────────────────────────

	public void backupToSQL(File file)                                                            { backup.backupToSQL(file); }
	public void restoreFromSQL(File file)                                                         { backup.restoreFromSQL(file); }
	@Override public void clearAll()                                                              { backup.clearAll(); }
}
