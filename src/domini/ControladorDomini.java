package domini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import herramienta.BackupService;
import herramienta.FiltreUtils;
import interficie.BibliotecaWriter;
import persistencia.ControladorPersistencia;
import java.util.Set;

public class ControladorDomini implements BibliotecaWriter {
	private static volatile ControladorDomini inst;
	private ControladorPersistencia cp;
	private ArrayList<Llibre> bib;
	private ArrayList<Llista> llistes;
	private ArrayList<Tag> tags;

	private static final Comparator<Llibre> compararISBN =
		(a, b) -> a.getISBN().compareTo(b.getISBN());

	private static Llibre searchKey(long isbn) {
		return new Llibre(isbn, "", "", 0, "", 0.0, 0.0, false, "");
	}

	public static synchronized ControladorDomini getInstance() {
		if (ControladorDomini.inst == null)
			ControladorDomini.inst = new ControladorDomini(ControladorPersistencia.getInstance());
		return ControladorDomini.inst;
	}

	public static void resetForTest() { inst = null; }

	public static void resetForProfileSwitch() {
		inst = null;
		ControladorPersistencia.resetForProfileSwitch();
	}

	private ControladorDomini(ControladorPersistencia cp) {
		this.cp = cp;
		bib = new ArrayList<Llibre>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
		tags = new ArrayList<>(cp.getAllTags());
		if (!"true".equals(System.getProperty("biblioteca.test"))) {
			new BackupService(cp).scheduleAutoBackup(bib, llistes, tags);
		}
	}

	public ArrayList<Llibre> aplicarFiltres(LlibreFilter f) {
		return aplicarFiltres(bib, f);
	}

	public ArrayList<Llibre> aplicarFiltres(ArrayList<Llibre> font, LlibreFilter f) {
		if (font == bib && bib.size() >= SQL_FILTER_THRESHOLD) {
			return searchLlibresSQL(f);
		}
		Set<Long> tagISBNs = f.tagId != null ? cp.getLlibresWithTag(f.tagId) : null;
		ArrayList<Llibre> resultat = new ArrayList<>();
		for (Llibre l : font) {
			if ((f.autor == null || FiltreUtils.matchString(f.autor, l.getAutor()))
					&& (f.nom == null || FiltreUtils.matchString(f.nom, l.getNom()))
					&& (f.isbn == null || FiltreUtils.matchISBN(f.isbn, l.getISBN()))
					&& (f.anyMin == null || (l.getAny() != null && l.getAny() >= f.anyMin))
					&& (f.anyMax == null || (l.getAny() != null && l.getAny() <= f.anyMax))
					&& (f.valoracioMin == null || (l.getValoracio() != null && l.getValoracio() >= f.valoracioMin))
					&& (f.valoracioMax == null || (l.getValoracio() != null && l.getValoracio() <= f.valoracioMax))
					&& (f.preuMin == null || (l.getPreu() != null && l.getPreu() >= f.preuMin))
					&& (f.preuMax == null || (l.getPreu() != null && l.getPreu() <= f.preuMax))
					&& (f.llegit == null || f.llegit.equals(l.getLlegit()))
					&& (tagISBNs == null || tagISBNs.contains(l.getISBN()))
					&& (f.editorial == null || FiltreUtils.matchString(f.editorial, l.getEditorial()))
					&& (f.serie == null || FiltreUtils.matchString(f.serie, l.getSerie()))
					&& (f.format == null || f.format.equalsIgnoreCase(l.getFormat()))
					&& (f.idioma == null || FiltreUtils.matchString(f.idioma, l.getIdioma()))) {
				resultat.add(l);
			}
		}
		return resultat;
	}

	/** Threshold above which SQL-side search is used instead of in-memory scan. */
	private static final int SQL_FILTER_THRESHOLD = 2000;

	/** SQL-backed filter for large libraries (> SQL_FILTER_THRESHOLD books). */
	public ArrayList<Llibre> searchLlibresSQL(LlibreFilter f) {
		return cp.searchLlibres(f, 0, 0);
	}

	/** Paginated SQL query: returns pageSize books starting at offset. */
	public ArrayList<Llibre> getLlibresPage(int offset, int pageSize) {
		return cp.searchLlibres(LlibreFilter.empty(), offset, pageSize);
	}

	/** Total books in DB (used for DB-side pagination). */
	public int countLlibresDB() { return cp.countLlibres(); }

	/** True when the library is large enough to benefit from SQL-side operations. */
	public boolean isLargeLibrary() { return bib.size() >= SQL_FILTER_THRESHOLD; }

	public ArrayList<Llibre> getAllLlibres() {
		return new ArrayList<>(bib);
	}

	public ArrayList<Llibre> get10Llibres() {
		return new ArrayList<>(bib.subList(0, Math.min(10, bib.size())));
	}

	public ArrayList<Llibre> get100Llibres(int index) { // Comença des del 0 fins al final
		return new ArrayList<>(bib.subList(100 * index, Math.min(100 * index + 100, bib.size())));
	}

	public int maxIndex100Llibres() { // maxim index que li pots indicar per agafar 100 llibres
		return bib.size() / 100;
	}

	public int getSize() {
		return bib.size();
	}

	public void addLlibre(Llibre l) throws Exception {
		cp.afegirLlibre(l);
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos >= 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " ja existeix a la base de dades");

		pos = -(pos + 1);
		bib.add(pos, l);
	}

	public void deleteLlibre(Llibre l) throws Exception {
		cp.eliminarLlibre(l);

		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + l.getISBN() + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public void deleteLlibre(Long ISBN) throws Exception {
		cp.eliminarLlibre(ISBN);

		int pos = Collections.binarySearch(bib, searchKey(ISBN), compararISBN);
		if (pos < 0)
			throw new Exception("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");

		bib.remove(pos);
	}

	public void updateLlibre(Llibre l) throws Exception {
		cp.updateLlibre(l);
		int pos = Collections.binarySearch(bib, l, compararISBN);
		if (pos >= 0) bib.set(pos, l);
	}

	public boolean existsLlibre(long ISBN) {
		return Collections.binarySearch(bib, searchKey(ISBN), compararISBN) >= 0;
	}

	public Llibre getLlibre(long ISBN) throws Exception {

		int index = Collections.binarySearch(bib, searchKey(ISBN), compararISBN);

		if (index < 0)
			throw new Exception("No existeix el llibre amb ISBN " + ISBN);

		return bib.get(index);
	}

	public void backupToSQL(java.io.File file) throws Exception {
		new BackupService(cp).backupToSQL(file, bib, llistes, tags);
	}

	public void restoreFromSQL(java.io.File file) throws Exception {
		cp.executeSQLFile(file);
		bib = new ArrayList<>(cp.getAllLlibres());
		Collections.sort(bib, compararISBN);
		llistes = new ArrayList<>(cp.getAllLlistes());
		tags = new ArrayList<>(cp.getAllTags());
	}

	public void clearAll() throws Exception {
		cp.clearAllData();
		bib.clear();
		llistes.clear();
		tags.clear();
	}

	public long getDbSizeBytes() { return cp.getDbSizeBytes(); }

	// ── Llista (shelf) management ──────────────────────────────────────────────

	public ArrayList<Llista> getAllLlistes() { return llistes; }

	public Llista addLlista(String nom) throws Exception {
		int id = cp.createLlista(nom);
		Llista l = new Llista(id, nom);
		llistes.add(l);
		return l;
	}

	public void deleteLlista(Llista llista) throws Exception {
		cp.deleteLlista(llista.getId());
		llistes.remove(llista);
	}

	public int getCountInLlista(int llistaId) { return cp.getCountInLlista(llistaId); }

	public ArrayList<Llibre> getLlibresInLlista(int llistaId) {
		return cp.getLlibresInLlista(llistaId);
	}

	public ArrayList<Llista> getLlistesForLlibre(long isbn) {
		return cp.getLlistesForLlibre(isbn);
	}

	public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception {
		cp.addLlibreToLlista(isbn, llistaId, valoracio, llegit);
	}

	public void removeLlibreFromLlista(long isbn, int llistaId) throws Exception {
		cp.removeLlibreFromLlista(isbn, llistaId);
	}

	public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws Exception {
		cp.updateLlibreInLlista(isbn, llistaId, valoracio, llegit);
	}

	public void moveLlistaUp(int id) throws Exception {
		int idx = indexOfLlista(id);
		if (idx > 0) swapLlistesOrdre(idx, idx - 1);
	}

	public void moveLlistaDown(int id) throws Exception {
		int idx = indexOfLlista(id);
		if (idx >= 0 && idx < llistes.size() - 1) swapLlistesOrdre(idx, idx + 1);
	}

	private int indexOfLlista(int id) {
		for (int i = 0; i < llistes.size(); i++)
			if (llistes.get(i).getId() == id) return i;
		return -1;
	}

	private void swapLlistesOrdre(int i, int j) throws Exception {
		for (int k = 0; k < llistes.size(); k++) {
			llistes.get(k).setOrdre(k);
			cp.updateLlistaOrdre(llistes.get(k).getId(), k);
		}
		Llista a = llistes.get(i);
		Llista b = llistes.get(j);
		a.setOrdre(j);
		b.setOrdre(i);
		cp.updateLlistaOrdre(a.getId(), j);
		cp.updateLlistaOrdre(b.getId(), i);
		Collections.swap(llistes, i, j);
	}

	public ArrayList<Llibre> getRecentlyAdded() {
		return cp.getRecentlyAdded(20);
	}

	public void setLlistaColor(int id, String color) throws Exception {
		cp.updateLlistaColor(id, color);
		for (Llista l : llistes) {
			if (l.getId() == id) { l.setColor(color); break; }
		}
	}

	public void prestarLlibre(long isbn, String nom) throws Exception {
		cp.addPrestec(isbn, nom);
	}

	public void retornarLlibre(long isbn) throws Exception {
		cp.returnPrestec(isbn);
	}

	public java.util.Set<Long> getLoanedISBNs() {
		return cp.getLoanedISBNs();
	}

	public java.util.List<Object[]> getLoansForIsbn(long isbn) {
		return cp.getLoansForIsbn(isbn);
	}

	public java.util.List<Object[]> getAllOverdueLoans(int daysThreshold) {
		return cp.getAllOverdueLoans(daysThreshold);
	}

	public byte[] getLlibreBlob(long isbn) {
		return cp.getLlibreBlob(isbn);
	}

	public void setLlibreBlob(long isbn, byte[] blob) throws java.sql.SQLException {
		cp.setLlibreBlob(isbn, blob);
		try {
			Llibre l = getLlibre(isbn);
			l.setImatgeBlob(blob);
			if (blob != null) l.setHasBlob(true);
		} catch (Exception ignored) {}
	}

	// ── Tag management ─────────────────────────────────────────────────────────

	public ArrayList<Tag> getAllTags() { return tags; }

	public Tag addTag(String nom) throws Exception {
		int id = cp.createTag(nom);
		Tag t = new Tag(id, nom);
		tags.add(t);
		return t;
	}

	public void deleteTag(Tag tag) throws Exception {
		cp.deleteTag(tag.getId());
		tags.remove(tag);
	}

	public ArrayList<Tag> getTagsForLlibre(long isbn) {
		return cp.getTagsForLlibre(isbn);
	}

	public void addLlibreToTag(long isbn, int tagId) throws Exception {
		cp.addLlibreToTag(isbn, tagId);
	}

	public void removeLlibreFromTag(long isbn, int tagId) throws Exception {
		cp.removeLlibreFromTag(isbn, tagId);
	}

	public java.util.List<String> getDistinctValues(String column) {
		return cp.getDistinctValues(column);
	}

	public java.util.List<String> getDistinctAutorNames() {
		return cp.getDistinctAutorNames();
	}
}
