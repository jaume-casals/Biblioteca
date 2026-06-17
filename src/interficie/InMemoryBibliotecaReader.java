package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import domini.Llista;
import persistencia.PrestecRow;
import domini.Tag;

import java.util.*;

/**
 * Pure-in-memory {@link BibliotecaReader} for fast UI / pure-logic tests that don't need JDBC.
 * Read-only; mutate the supplied lists between operations.
 *
 * <p>Note: {@link #aplicarFiltres} returns the full list unchanged — this
 * class does NOT honour filter criteria. Tests of filter logic must use a
 * real {@link BibliotecaReader} (e.g. via {@code ControladorDomini}).
 */
public class InMemoryBibliotecaReader implements BibliotecaReader {

    public final List<Llibre> books = new ArrayList<>();
    public final List<Llista> llistes = new ArrayList<>();
    public final List<Tag> tags = new ArrayList<>();
    public final List<PrestecRow> loans = new ArrayList<>();
    private final Map<Long, Llibre> byIsbn = new HashMap<>();
    private boolean byIsbnDirty = true;

    @Override public List<Llibre> obtenirAllLlibres() { return new ArrayList<>(books); }
    @Override public Llibre obtenirLlibre(long isbn) throws Exception {
        if (byIsbnDirty) rebuildIndex();
        Llibre l = byIsbn.get(isbn);
        if (l == null) throw new domini.BibliotecaException.NoTrobat("Not found: " + isbn);
        return l;
    }

    /** Rebuilds the ISBN→Llibre index. Callers that mutate {@link #books} should
     *  treat the index as dirty (handled by the dirty flag). */
    private void rebuildIndex() {
        byIsbn.clear();
        for (Llibre l : books) if (l.obtenirISBN() != null) byIsbn.put(l.obtenirISBN(), l);
        byIsbnDirty = false;
    }
    @Override public int getSize() { return books.size(); }
    @Override public boolean existsLlibre(long isbn) { return books.stream().anyMatch(l -> java.util.Objects.equals(l.obtenirISBN(), isbn)); }
    @Override public List<Llibre> get10Llibres() { return new ArrayList<>(books.subList(0, Math.min(10, books.size()))); }
    @Override public List<Llibre> get100Llibres(int index) {
        int from = index * 100, to = Math.min(from + 100, books.size());
        if (from >= books.size()) return new ArrayList<>();
        return new ArrayList<>(books.subList(from, to));
    }
    @Override public int maxIndex100Llibres() { return Math.max(0, (books.size() - 1) / 100); }
    @Override public List<Llibre> obtenirRecentlyAdded() { return new ArrayList<>(books.subList(Math.max(0, books.size() - 20), books.size())); }
    @Override public boolean esLargeLibrary() { return false; }
    @Override public int comptarLlibresDB() { return books.size(); }
    @Override public List<Llibre> obtenirLlibresPage(int offset, int pageSize) {
        int to = Math.min(offset + pageSize, books.size());
        return offset >= books.size() ? new ArrayList<>() : new ArrayList<>(books.subList(offset, to));
    }
    @Override public List<Llibre> aplicarFiltres(LlibreFilter f) {
        throw new UnsupportedOperationException(
            "InMemoryBibliotecaReader does not honour filter criteria — use a real BibliotecaReader (see class Javadoc).");
    }
    @Override public List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f) {
        throw new UnsupportedOperationException(
            "InMemoryBibliotecaReader does not honour filter criteria — use a real BibliotecaReader (see class Javadoc).");
    }
    @Override public List<Llibre> cercarLlibresSQL(LlibreFilter f) {
        throw new UnsupportedOperationException(
            "InMemoryBibliotecaReader does not honour search criteria — use a real BibliotecaReader (see class Javadoc).");
    }
    @Override public List<Llista> obtenirAllLlistes() { return new ArrayList<>(llistes); }
    @Override public Llista obtenirLlistaById(int id) throws Exception {
        for (Llista l : llistes) if (l.obtenirId() == id) return l;
        throw new Exception("Shelf not found: " + id);
    }
    @Override public int obtenirCountInLlista(int llistaId) { return 0; }
    @Override public Map<Integer, Integer> obtenirAllCountsInLlistes() { return new HashMap<>(); }
    @Override public List<Llibre> obtenirLlibresInLlista(int llistaId) { return new ArrayList<>(); }
    @Override public List<LlibreLlistaRow> obtenirAllLlibreLlistaRows() { return new ArrayList<>(); }
    @Override public List<Llista> obtenirLlistesForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) { return new ArrayList<>(); }
    @Override public List<Tag> obtenirAllTags() { return new ArrayList<>(tags); }
    @Override public Tag obtenirTagById(int id) throws Exception {
        for (Tag t : tags) if (t.obtenirId() == id) return t;
        throw new Exception("Tag not found: " + id);
    }
    @Override public List<Tag> obtenirTagsForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<LlibreTagRow> obtenirAllLlibreTagRows() { return new ArrayList<>(); }
    @Override public Set<Long> obtenirLlibresWithTag(int tagId) { return new HashSet<>(); }
    @Override public Set<Long> obtenirLoanedISBNs() {
        Set<Long> s = new HashSet<>();
        for (PrestecRow r : loans) if (!r.retornat()) s.add(r.isbn());
        return s;
    }
    @Override public List<PrestecRow> obtenirAllActiveLoans() {
        List<PrestecRow> out = new ArrayList<>();
        for (PrestecRow r : loans) if (!r.retornat()) out.add(r);
        return out;
    }
    @Override public List<PrestecRow> obtenirLoansForIsbn(long isbn) {
        List<PrestecRow> out = new ArrayList<>();
        for (PrestecRow r : loans) if (r.isbn() == isbn) out.add(r);
        return out;
    }
    @Override public List<persistencia.OverdueLoan> obtenirAllOverdueLoans(int daysThreshold) { return new ArrayList<>(); }
    @Override public int comptarLoans(long isbn) {
        int c = 0;
        for (PrestecRow r : loans) if (r.isbn() == isbn) c++;
        return c;
    }
    @Override public byte[] obtenirLlibreBlob(long isbn) { return null; }
    @Override public long obtenirDbSizeBytes() { return 0; }
    @Override public List<String> obtenirDistinctValues(String column) { return new ArrayList<>(); }
    @Override public List<String> obtenirDistinctAutorNames() { return new ArrayList<>(); }
}
