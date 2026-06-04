package domini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import herramienta.BackupService;
import domini.BibliotecaException;
import herramienta.FiltreUtils;
import interficie.BibliotecaWriter;
import persistencia.ControladorPersistencia;
import java.util.Set;

public class ControladorDomini implements BibliotecaWriter {
	private static ControladorDomini inst;
	private ControladorPersistencia cp;
	private BackupService backupService;
	private ArrayList<Llibre> bib;
	private ArrayList<Llista> llistes;
	private ArrayList<Tag> tags;

	private static final Comparator<Llibre> compararISBN =
		(a, b) -> {
			Long ia = a.getISBN(), ib = b.getISBN();
			if (ia == null && ib == null) return 0;
			if (ia == null) return -1;
			if (ib == null) return 1;
			return ia.compareTo(ib);
		};

	private static final Map<String, Comparator<Llibre>> SORT_BY = Map.of(
		SortSpec.COL_ISBN,      compararISBN,
		SortSpec.COL_NOM,       Comparator.comparing(l -> l.getNom() != null ? l.getNom() : "", String.CASE_INSENSITIVE_ORDER),
		SortSpec.COL_ANY,       Comparator.comparing(l -> l.getAny() != null ? l.getAny() : 0),
		SortSpec.COL_VALORACIO, Comparator.comparing(l -> l.getValoracio() != null ? l.getValoracio() : 0.0),
		SortSpec.COL_PREU,      Comparator.comparing(l -> l.getPreu() != null ? l.getPreu() : 0.0)
	);

	private static Llibre searchKey(long isbn) {
		return new Llibre(isbn, "", "", 0, "", 0.0, 0.0, false, "");
	}

	public static synchronized ControladorDomini getInstance() {
		if (ControladorDomini.inst == null)
			ControladorDomini.inst = new ControladorDomini(ControladorPersistencia.getInstance());
		return ControladorDomini.inst;
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
		this.cp = cp;
		bib = new ArrayList<Llibre>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
		tags = new ArrayList<>(cp.getAllTags());
		backupService = new BackupService(cp);
		if (!"true".equals(System.getProperty("biblioteca.test"))) {
			backupService.scheduleAutoBackup();
		}
	}

	public java.util.List<Llibre> aplicarFiltres(LlibreFilter f) {
		if (bib.size() >= SQL_FILTER_THRESHOLD) return searchLlibresSQL(f);
		return filterInMemory(bib, f);
	}

	// SQL fallback only applies to the full library (1-arg overload). This overload always filters in-memory.
	public java.util.List<Llibre> aplicarFiltres(java.util.List<Llibre> font, LlibreFilter f) {
		return filterInMemory(font, f);
	}

	private ArrayList<Llibre> filterInMemory(java.util.List<Llibre> font, LlibreFilter f) {
		Set<Long> tagISBNs   = f.getTagId()   != null ? cp.getLlibresWithTag(f.getTagId())     : null;
		Set<Long> llistaISBNs = f.getLlistaId() != null ? cp.getISBNsInLlista(f.getLlistaId()) : null;
		ArrayList<Llibre> resultat = new ArrayList<>();
		for (Llibre l : font) {
			if (FiltreUtils.matches(l, f, tagISBNs, llistaISBNs)) {
				resultat.add(l);
			}
		}
		applySort(resultat, f);
		return resultat;
	}

	private static void applySort(ArrayList<Llibre> list, LlibreFilter f) {
		SortSpec sort = f.getSort();
		if (sort == null || list.size() < 2) return;
		Comparator<Llibre> cmp = SORT_BY.getOrDefault(sort.column(), compararISBN);
		if (!sort.ascending()) cmp = cmp.reversed();
		list.sort(cmp);
	}

	/** Threshold above which SQL-side search is used instead of in-memory scan. */
	private static final int SQL_FILTER_THRESHOLD = 2000;

	/** SQL-backed filter for large libraries (> SQL_FILTER_THRESHOLD books). */
	public java.util.List<Llibre> searchLlibresSQL(LlibreFilter f) {
		return cp.searchLlibres(f, 0, 0);
	}

	/** Paginated SQL query: returns pageSize books starting at offset. */
	public java.util.List<Llibre> getLlibresPage(int offset, int pageSize) {
		return cp.searchLlibres(LlibreFilter.empty(), offset, pageSize);
	}

	/** Total books in DB (used for DB-side pagination). */
	public int countLlibresDB() { return cp.countLlibres(); }

	/** True when the library is large enough to benefit from SQL-side operations. */
	public boolean isLargeLibrary() { return bib.size() >= SQL_FILTER_THRESHOLD; }

	public java.util.List<Llibre> getAllLlibres() {
		return new ArrayList<>(bib);
	}

	public java.util.List<Llibre> getUnmodifiableLlibres() {
		return Collections.unmodifiableList(bib);
	}

	public java.util.List<Llibre> get10Llibres() {
		return new ArrayList<>(bib.subList(0, Math.min(10, bib.size())));
	}

public java.util.List<Llibre> get100Llibres(int index) { // Starts from index*100
        int from = Math.min(100 * index, bib.size());
        int to = Math.min(from + 100, bib.size());
        return new ArrayList<>(bib.subList(from, to));
    }

public int maxIndex100Llibres() { // maxim index que li pots indicar per agafar 100 llibres
        return Math.max(0, (bib.size() - 1) / 100);
    }

	public int getSize() {
		return bib.size();
	}

	public void addLlibre(Llibre l) {
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos >= 0)
			throw new BibliotecaException.Duplicate("El llibre amb ISBN: " + l.getISBN() + " ja existeix a la biblioteca");
		try { cp.afegirLlibre(l); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		pos = -(pos + 1);
		bib.add(pos, l);
	}

	public void deleteLlibre(Llibre l) {
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos < 0) throw new BibliotecaException.NotFound("El llibre amb ISBN: " + l.getISBN() + " no existeix a la base de dades");
		try { cp.eliminarLlibre(l); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		bib.remove(pos);
	}

	public void deleteLlibre(Long ISBN) {
		if (ISBN == null) throw new BibliotecaException.NotFound("ISBN és null");
		int pos = Collections.binarySearch(bib, searchKey(ISBN), compararISBN);
		if (pos < 0) throw new BibliotecaException.NotFound("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");
		try { cp.eliminarLlibre(ISBN); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		bib.remove(pos);
	}

	public void updateLlibre(Llibre l) {
		try { cp.updateLlibre(l); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos >= 0) bib.set(pos, l);
	}

	public boolean existsLlibre(long ISBN) {
		return Collections.binarySearch(bib, searchKey(ISBN), compararISBN) >= 0;
	}

	public Llibre getLlibre(long ISBN) {
		int index = Collections.binarySearch(bib, searchKey(ISBN), compararISBN);
		if (index < 0)
			throw new BibliotecaException.NotFound("No existeix el llibre amb ISBN " + ISBN);
		Llibre l = bib.get(index);
		if (!l.isHeavyFieldsLoaded()) loadHeavyFields(l);
		return l;
	}

	@Override
	public void loadHeavyFields(Llibre book) {
		if (book == null || book.isHeavyFieldsLoaded()) return;
		try {
			cp.loadHeavyFields(book.getISBN(), book);
			int index = Collections.binarySearch(bib, book, compararISBN);
			if (index >= 0) bib.set(index, book);
		} catch (RuntimeException e) {
			throw new BibliotecaException("No s'han pogut carregar els camps pesats del llibre amb ISBN " + book.getISBN(), e);
		}
	}

	public void backupToSQL(java.io.File file) {
		ArrayList<Llibre> bibSnapshot;
		ArrayList<Llista> llistesSnapshot;
		ArrayList<Tag> tagsSnapshot;
		synchronized (this) {
			for (Llibre l : bib) loadHeavyFields(l);
			bibSnapshot = new ArrayList<>(bib);
			llistesSnapshot = new ArrayList<>(llistes);
			tagsSnapshot = new ArrayList<>(tags);
		}
		try { backupService.backupToSQL(file, bibSnapshot, llistesSnapshot, tagsSnapshot); }
		catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void restoreFromSQL(java.io.File file) {
		try {
			java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
			java.io.File backup = new java.io.File(tmpDir, "biblioteca_restore_backup_" + System.currentTimeMillis() + ".sql");
			backup.deleteOnExit();
			backupService.backupToSQL(backup, new ArrayList<>(bib), new ArrayList<>(llistes), new ArrayList<>(tags));
			try {
				cp.clearAllData();
				cp.executeSQLFile(file);
			} catch (Exception e) {
				try { cp.executeSQLFile(backup); } catch (Exception ex) { throw new BibliotecaException("Restore failed and rollback also failed: " + e.getMessage() + "; rollback error: " + ex.getMessage(), e); }
				throw new BibliotecaException("Restore failed: " + e.getMessage(), e);
			}
		} catch (Exception e) { throw new BibliotecaException(e.getMessage(), e); }
		bib = new ArrayList<>(cp.getAllLlibres());
		if (bib.isEmpty()) throw new BibliotecaException("Restore completat però no s'han carregat llibres — el fitxer pot estar buit o corrupte");
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
		tags = new ArrayList<>(cp.getAllTags());
	}

	public void clearAll() {
		try { cp.clearAllData(); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		bib.clear();
		llistes.clear();
		tags.clear();
	}

	public long getDbSizeBytes() { return cp.getDbSizeBytes(); }

	// ── Llista (shelf) management ──────────────────────────────────────────────

	public java.util.List<Llista> getAllLlistes() { return new java.util.ArrayList<>(llistes); }

	public Llista getLlistaById(int id) throws Exception {
		for (Llista l : llistes) if (l.getId() == id) return l;
		throw new BibliotecaException.NotFound("Shelf not found: " + id);
	}

	public Llista addLlista(String nom) {
		if (nom == null || nom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
		try {
			int id = cp.createLlista(nom);
			Llista l = new Llista(id, nom);
			llistes.add(l);
			return l;
		} catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void deleteLlista(Llista llista) {
		try { cp.deleteLlista(llista.getId()); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		llistes.remove(llista);
	}

	public void renameLlista(int id, String newNom) {
		if (newNom == null || newNom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
		try { cp.renameLlista(id, newNom); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		llistes.stream().filter(l -> l.getId() == id).findFirst().ifPresent(l -> l.setNom(newNom));
	}

	public int getCountInLlista(int llistaId) { return cp.getCountInLlista(llistaId); }
	public java.util.Map<Integer, Integer> getAllCountsInLlistes() { return cp.getAllCountsInLlistes(); }

	public java.util.List<Llibre> getLlibresInLlista(int llistaId) {
		return cp.getLlibresInLlista(llistaId);
	}

	public java.util.List<Llista> getLlistesForLlibre(long isbn) {
		return cp.getLlistesForLlibre(isbn);
	}

	public java.util.List<domini.LlibreLlistaContext> getLlistesForLlibreContext(long isbn) {
		return cp.getLlistesForLlibreContext(isbn);
	}

	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
		try { cp.addLlibreToLlista(isbn, llistaId, valoracio, llegit); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void removeLlibreFromLlista(long isbn, int llistaId) {
		try { cp.removeLlibreFromLlista(isbn, llistaId); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
		try { cp.updateLlibreInLlista(isbn, llistaId, valoracio, llegit); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void moveLlistaUp(int id) {
		int idx = indexOfLlista(id);
		if (idx > 0) swapLlistesOrdre(idx, idx - 1);
	}

	public void moveLlistaDown(int id) {
		int idx = indexOfLlista(id);
		if (idx >= 0 && idx < llistes.size() - 1) swapLlistesOrdre(idx, idx + 1);
	}

	private int indexOfLlista(int id) {
		for (int i = 0; i < llistes.size(); i++)
			if (llistes.get(i).getId() == id) return i;
		return -1;
	}

private void swapLlistesOrdre(int i, int j) {
        Llista a = llistes.get(i);
        Llista b = llistes.get(j);
        int ordreA = a.getOrdre();
        int ordreB = b.getOrdre();
        try {
            cp.updateLlistaOrdre(a.getId(), ordreB);
            cp.updateLlistaOrdre(b.getId(), ordreA);
            a.setOrdre(ordreB);
            b.setOrdre(ordreA);
            Collections.swap(llistes, i, j);
        } catch (java.sql.SQLException e) {
            a.setOrdre(ordreA);
            b.setOrdre(ordreB);
            throw new BibliotecaException(e.getMessage(), e);
        }
    }

	public java.util.List<Llibre> getRecentlyAdded() {
		return cp.getRecentlyAdded(20);
	}

	public void setLlistaColor(int id, String color) {
		if (!Llista.isValidColor(color))
			throw new BibliotecaException.Validation(herramienta.I18n.t("val_color_invalid", color));
		try { cp.updateLlistaColor(id, color); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		for (Llista l : llistes) {
			if (l.getId() == id) { l.setColor(color); break; }
		}
	}

	public void prestarLlibre(long isbn, String nom) {
		try { cp.addPrestec(isbn, nom); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void retornarLlibre(long isbn) {
		try { cp.returnPrestec(isbn); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public java.util.Set<Long> getLoanedISBNs() {
		return cp.getLoanedISBNs();
	}

	public java.util.List<persistencia.PrestecRow> getAllActiveLoans() {
		return cp.getAllActiveLoans();
	}

	public java.util.List<persistencia.PrestecRow> getLoansForIsbn(long isbn) {
		return cp.getLoansForIsbn(isbn);
	}

	public java.util.List<Object[]> getAllOverdueLoans(int daysThreshold) {
		return cp.getAllOverdueLoans(daysThreshold);
	}

	public int countLoans(long isbn) { return cp.countLoans(isbn); }

	public byte[] getLlibreBlob(long isbn) {
		return cp.getLlibreBlob(isbn);
	}

public void setLlibreBlob(long isbn, byte[] blob) {
        try {
            cp.setLlibreBlob(isbn, blob);
            for (Llibre l : bib) {
                if (java.util.Objects.equals(l.getISBN(), isbn)) { l.setImatgeBlob(blob); l.setHasBlob(true); break; }
            }
        }
        catch (Exception e) { throw new BibliotecaException("Failed to set cover blob: " + e.getMessage(), e); }
    }

	// ── Tag management ─────────────────────────────────────────────────────────

	public java.util.List<Tag> getAllTags() { return new java.util.ArrayList<>(tags); }

	public Tag getTagById(int id) throws Exception {
		for (Tag t : tags) if (t.getId() == id) return t;
		throw new BibliotecaException.NotFound("Tag not found: " + id);
	}

	public Tag addTag(String nom) {
		if (nom == null || nom.isBlank()) throw new BibliotecaException("El nom de l'etiqueta no pot estar buit");
		try {
			int id = cp.createTag(nom);
			Tag t = new Tag(id, nom);
			tags.add(t);
			return t;
		} catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void deleteTag(Tag tag) {
		try { cp.deleteTag(tag.getId()); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		tags.remove(tag);
	}

	public void renameTag(int id, String newNom) {
		try { cp.renameTag(id, newNom); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
		for (Tag t : tags) { if (t.getId() == id) { t.setNom(newNom); break; } }
	}

	public java.util.Set<Long> getLlibresWithTag(int tagId) { return cp.getLlibresWithTag(tagId); }

	public java.util.List<Tag> getTagsForLlibre(long isbn) {
		return cp.getTagsForLlibre(isbn);
	}

	public void addLlibreToTag(long isbn, int tagId) {
		try { cp.addLlibreToTag(isbn, tagId); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	public void removeLlibreFromTag(long isbn, int tagId) {
		try { cp.removeLlibreFromTag(isbn, tagId); } catch (java.sql.SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
	}

	/**
	 * Llista els valors diferents d'una columna per a l'autocompletat de camps.
	 *
	 * <p>Comportament amb doble camí (intencionat, no error):
	 * <ul>
	 *   <li><b>Camí en memòria</b>: columnes que són camps String de {@link Llibre}
	 *       ({@code editorial}, {@code serie}, {@code idioma}, {@code format},
	 *       {@code pais_origen}, {@code llengua_original}). Escaneja la llista
	 *       {@code bib} en memòria — és la font de veritat quan la biblioteca és
	 *       petita i tots els llibres són a memòria. Ràpid però només reflecteix
	 *       els llibres ja carregats.</li>
	 *   <li><b>Camí SQL (caiguda)</b>: per a qualsevol altra columna. Delega a
	 *       {@code cp.getDistinctValues(column)}, que consulta la taula {@code llibre}
	 *       directament. També retorna llista buida si la columna no és a
	 *       {@code persistencia.TagDao.AUTOCOMPLETE_COLUMNS} (llista blanca).</li>
	 * </ul>
	 *
	 * <p>Si la columna demanada no és ni al switch en memòria ni a la llista blanca
	 * SQL, el resultat és una llista buida — no es llança excepció. Els dos conjunts
	 * de columnes s'han de mantenir sincronitzats; el switch en memòria és un
	 * subconjunt d'{@code AUTOCOMPLETE_COLUMNS}.
	 */
	public java.util.List<String> getDistinctValues(String column) {
		java.util.function.Function<Llibre, String> extractor = switch (column) {
			case "editorial"       -> Llibre::getEditorial;
			case "serie"           -> Llibre::getSerie;
			case "idioma"          -> Llibre::getIdioma;
			case "format"          -> Llibre::getFormat;
			case "pais_origen"     -> Llibre::getPaisOrigen;
			case "llengua_original"-> Llibre::getLlenguaOriginal;
			default                -> null;
		};
		if (extractor != null) {
			return bib.stream()
				.map(extractor)
				.filter(s -> s != null && !s.isEmpty())
				.distinct()
				.sorted()
				.collect(java.util.stream.Collectors.toList());
		}
		return cp.getDistinctValues(column);
	}

	public java.util.List<String> getDistinctAutorNames() {
		java.util.TreeSet<String> names = new java.util.TreeSet<>();
		for (Llibre l : bib) {
			java.util.List<String> a = l.getAutors();
			if (a != null && !a.isEmpty()) {
				a.stream().filter(s -> s != null && !s.isEmpty()).forEach(names::add);
			} else if (l.getAutor() != null && !l.getAutor().isEmpty()) {
				names.add(l.getAutor());
			}
		}
		return new java.util.ArrayList<>(names);
	}

	public java.util.List<persistencia.LlibreLlistaRow> getAllLlibreLlistaRows() { return cp.getAllLlibreLlista(); }
	public java.util.List<persistencia.LlibreTagRow>    getAllLlibreTagRows()    { return cp.getAllLlibreTag(); }

	public java.util.List<Object[]> getAutorsData() { return cp.getAllAutors(); }
	public java.util.List<Object[]> getLlibreAutorData() { return cp.getAllLlibreAutor(); }
	public java.util.List<persistencia.PrestecRow> getAllPrestecs() { return cp.getAllPrestecs(); }
	/**
	 * Retorna totes les sessions de lectura. Avui només l'usa
	 * {@link herramienta.BackupService} per fer còpies de seguretat.
	 * La UI no l'ha implementat encara — veure {@code todo.txt} [1]
	 * ConnectionConfig LecturaRow — si mai es vol una vista "historial
	 * de lectura", s'ha de connectar aquí.
	 */
	public java.util.List<persistencia.LecturaRow> getAllLecturesData() { return cp.getAllLectures(); }
}
