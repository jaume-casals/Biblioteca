package domini;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import domini.facade.DelegatCopiaSeguretat;
import domini.facade.DelegatLlibre;
import domini.facade.DelegatPrestec;
import domini.facade.DelegatPrestatgeria;
import domini.facade.StateContext;
import domini.facade.DelegatEstadistiques;
import domini.facade.TagDelegate;
import herramienta.ServeiCopiaSeguretat;
import interficie.EscritorBiblioteca;
import persistencia.ControladorPersistencia;
import persistencia.LecturaRow;
import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import persistencia.PrestecRow;

/**
 * Façana singleton sobre l'estat en memòria de la biblioteca i la persistència.
 *
 * <p>Responsabilitats:
 * <ul>
 *   <li>Cicle de vida del singleton ({@link #getInstance()}, {@link #reinicialitzarForTest()},
 *       {@link #reinicialitzarForProfileSwitch()}, {@link #create(ControladorPersistencia)}).</li>
 *   <li>Posseeix el {@link StateContext}: les tres llistes de suport
 *       ({@code bib}, {@code llistes}, {@code tags}), el lock en memòria
 *       ({@code BIB_LOCK}) i els mapes d'índex per id.</li>
 *   <li>Delega cada operació de negoci a una de les sis classes col·laboradores
 *       específiques de {@code domini.facade} — Prestatgeria, Etiqueta,
 *       Préstec, Llibre, Estadístiques, CòpiaDeSeguretat.</li>
 * </ul>
 *
 * <p>Implementa només {@link EscritorBiblioteca} — les sub-interfícies
 * ({@code EscritorLlibre}, {@code EscritorPrestatgeria},
 * {@code EscritorEtiqueta}, {@code EscritorPrestec}) s'hereten per la
 * cadena {@code extends}, de manera que cada mètode d'escriptura es
 * continua implementant exactament un cop aquí.
 */
public class ControladorDomini implements EscritorBiblioteca {

	private static ControladorDomini inst;

	private final StateContext state;
	private final ServeiCopiaSeguretat copiaSegService;
	private final DelegatPrestatgeria shelves;
	private final TagDelegate tags;
	private final DelegatPrestec loans;
	private final DelegatLlibre books;
	private final DelegatEstadistiques stats;
	private final DelegatCopiaSeguretat backup;

	public static synchronized ControladorDomini getInstance() {
		if (inst == null) inst = new ControladorDomini(ControladorPersistencia.getInstance());
		return inst;
	}

	public static synchronized void reinicialitzarForTest() { inst = null; }

	public static synchronized void reinicialitzarForProfileSwitch() {
		inst = null;
		ControladorPersistencia.reinicialitzarForProfileSwitch();
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
			new java.util.ArrayList<Llibre>(cp.obtenirAllLlibres()),
			new java.util.ArrayList<>(cp.obtenirAllLlistes()),
			new java.util.ArrayList<>(cp.obtenirAllTags()));
		Collections.sort(this.state.bib(), DelegatLlibre.ISBN_COMPARATOR);
		this.copiaSegService = new ServeiCopiaSeguretat(cp);
		this.shelves = new DelegatPrestatgeria(this.state);
		this.tags    = new TagDelegate(this.state);
		this.loans   = new DelegatPrestec(this.state);
		this.books   = new DelegatLlibre(this.state);
		this.stats   = new DelegatEstadistiques(this.state);
		this.backup  = new DelegatCopiaSeguretat(this.state, this.copiaSegService);
		if (!"true".equals(System.getProperty("biblioteca.test"))) {
			copiaSegService.scheduleAutoBackup();
		}
	}

	// ── Book / search / blob ──────────────────────────────────────────────────

	public java.util.List<Llibre> aplicarFiltres(LlibreFilter f)                                  { return books.aplicarFiltres(f); }
	public java.util.List<Llibre> aplicarFiltres(java.util.List<Llibre> font, LlibreFilter f)     { return books.aplicarFiltres(font, f); }
	public java.util.List<Llibre> cercarLlibresSQL(LlibreFilter f)                                { return books.cercarLlibresSQL(f); }
	public java.util.List<Llibre> obtenirLlibresPage(int offset, int pageSize)                        { return books.obtenirLlibresPage(offset, pageSize); }
	public int comptarLlibresDB()                                                                   { return books.comptarLlibresDB(); }
	public boolean esLargeLibrary()                                                               { return books.esLargeLibrary(); }
	public java.util.List<Llibre> obtenirAllLlibres()                                                 { return books.obtenirAllLlibres(); }
	public java.util.List<Llibre> obtenirUnmodifiableLlibres()                                        { return books.obtenirUnmodifiableLlibres(); }
	public java.util.List<Llibre> get10Llibres()                                                  { return books.get10Llibres(); }
	public java.util.List<Llibre> get100Llibres(int index)                                        { return books.get100Llibres(index); }
	public int maxIndex100Llibres()                                                               { return books.maxIndex100Llibres(); }
	public int getSize()                                                                         { return books.getSize(); }
	public void afegirLlibre(Llibre l)                                                               { books.afegirLlibre(l); }
	public void eliminarLlibre(Llibre l)                                                            { books.eliminarLlibre(l); }
	public void eliminarLlibre(Long ISBN)                                                           { books.eliminarLlibreByIsbn(ISBN); }
	public void actualitzarLlibre(Llibre l)                                                            { books.actualitzarLlibre(l); }
	public boolean existsLlibre(long ISBN)                                                        { return books.existsLlibre(ISBN); }
	public Llibre obtenirLlibre(long ISBN)                                                            { return books.obtenirLlibre(ISBN); }
	@Override public void carregarHeavyFields(Llibre book)                                            { books.carregarHeavyFields(book); }
	public java.util.List<Llibre> obtenirRecentlyAdded()                                              { return books.obtenirRecentlyAdded(); }
	public byte[] obtenirLlibreBlob(long isbn)                                                        { return books.obtenirLlibreBlob(isbn); }
	public void posarLlibreBlob(long isbn, byte[] blob)                                             { books.posarLlibreBlob(isbn, blob); }

	// ── Shelf ─────────────────────────────────────────────────────────────────

	public java.util.List<Llista> obtenirAllLlistes()                                                 { return shelves.obtenirAllLlistes(); }
	public Llista obtenirLlistaById(int id) throws domini.BibliotecaException.NoTrobat              { return shelves.obtenirLlistaById(id); }
	public Llista afegirLlista(String nom)                                                           { return shelves.afegirLlista(nom); }
	public void eliminarLlista(Llista llista)                                                       { shelves.eliminarLlista(llista); }
	public void reanomenarLlista(int id, String newNom)                                               { shelves.reanomenarLlista(id, newNom); }
	public int obtenirCountInLlista(int llistaId)                                                     { return shelves.obtenirCountInLlista(llistaId); }
	public java.util.Map<Integer, Integer> obtenirAllCountsInLlistes()                                { return shelves.obtenirAllCountsInLlistes(); }
	public java.util.List<Llibre> obtenirLlibresInLlista(int llistaId)                                { return shelves.obtenirLlibresInLlista(llistaId); }
	public java.util.List<Llista> obtenirLlistesForLlibre(long isbn)                                  { return shelves.obtenirLlistesForLlibre(isbn); }
	public java.util.List<LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn)               { return shelves.obtenirLlistesForLlibreContext(isbn); }
	public void afegirLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit)      { shelves.afegirLlibreToLlista(isbn, llistaId, valoracio, llegit); }
	public void eliminarLlibreFromLlista(long isbn, int llistaId)                                   { shelves.eliminarLlibreFromLlista(isbn, llistaId); }
	public void actualitzarLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit)  { shelves.actualitzarLlibreInLlista(isbn, llistaId, valoracio, llegit); }
	public void moureLlistaUp(int id)                                                              { shelves.moureLlistaUp(id); }
	public void moureLlistaDown(int id)                                                            { shelves.moureLlistaDown(id); }
	public void posarLlistaColor(int id, String color)                                              { shelves.posarLlistaColor(id, color); }

	// ── Tag ───────────────────────────────────────────────────────────────────

	public java.util.List<Tag> obtenirAllTags()                                                       { return tags.obtenirAllTags(); }
	public Tag obtenirTagById(int id) throws domini.BibliotecaException.NoTrobat                    { return tags.obtenirTagById(id); }
	public Tag afegirTag(String nom)                                                                 { return tags.afegirTag(nom); }
	public void eliminarTag(Tag tag)                                                                { tags.eliminarTag(tag); }
	public void reanomenarTag(int id, String newNom)                                                  { tags.reanomenarTag(id, newNom); }
	public java.util.Set<Long> obtenirLlibresWithTag(int tagId)                                       { return tags.obtenirLlibresWithTag(tagId); }
	public java.util.List<Tag> obtenirTagsForLlibre(long isbn)                                        { return tags.obtenirTagsForLlibre(isbn); }
	public void afegirLlibreToTag(long isbn, int tagId)                                              { tags.afegirLlibreToTag(isbn, tagId); }
	public void eliminarLlibreFromTag(long isbn, int tagId)                                         { tags.eliminarLlibreFromTag(isbn, tagId); }

	// ── Loan ──────────────────────────────────────────────────────────────────

	public void prestarLlibre(long isbn, String nom)                                              { loans.prestarLlibre(isbn, nom); }
	public void retornarLlibre(long isbn)                                                         { loans.retornarLlibre(isbn); }
	public java.util.Set<Long> obtenirLoanedISBNs()                                                   { return loans.obtenirLoanedISBNs(); }
	public java.util.List<PrestecRow> obtenirAllActiveLoans()                                         { return loans.obtenirAllActiveLoans(); }
	public java.util.List<PrestecRow> obtenirLoansForIsbn(long isbn)                                  { return loans.obtenirLoansForIsbn(isbn); }
	public java.util.List<persistencia.PrestecEndarrerit> obtenirAllOverdueLoans(int daysThreshold)          { return loans.obtenirAllOverdueLoans(daysThreshold); }
	public int comptarLoans(long isbn)                                                              { return loans.comptarLoans(isbn); }

	// ── Stats / autocomplete / backup payload ────────────────────────────────

	public java.util.List<String> obtenirDistinctValues(String column)                                { return stats.obtenirDistinctValues(column); }
	public java.util.List<String> obtenirDistinctAutorNames()                                         { return stats.obtenirDistinctAutorNames(); }
	public java.util.List<LlibreLlistaRow> obtenirAllLlibreLlistaRows()                               { return stats.obtenirAllLlibreLlistaRows(); }
	public java.util.List<LlibreTagRow>    obtenirAllLlibreTagRows()                                  { return stats.obtenirAllLlibreTagRows(); }
	public java.util.List<Object[]>        obtenirAutorsData()                                        { return stats.obtenirAutorsData(); }
	public java.util.List<Object[]>        obtenirLlibreAutorData()                                   { return stats.obtenirLlibreAutorData(); }
	public java.util.List<PrestecRow>      obtenirAllPrestecs()                                       { return stats.obtenirAllPrestecs(); }
	public java.util.List<LecturaRow>      obtenirAllLecturesData()                                   { return stats.obtenirAllLecturesData(); }
	public long obtenirDbSizeBytes()                                                                  { return stats.obtenirDbSizeBytes(); }

	// ── Backup / restore / clearAll ───────────────────────────────────────────

	public void copiaSegToSQL(File file)                                                            { backup.copiaSegToSQL(file); }
	public void restaurarFromSQL(File file)                                                         { backup.restaurarFromSQL(file); }
	@Override public void netejarAll()                                                              { backup.netejarAll(); }
}
