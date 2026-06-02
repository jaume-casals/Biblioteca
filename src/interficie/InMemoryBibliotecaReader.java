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
 */
public class InMemoryBibliotecaReader implements BibliotecaReader {

    public final ArrayList<Llibre> books = new ArrayList<>();
    public final ArrayList<Llista> llistes = new ArrayList<>();
    public final ArrayList<Tag> tags = new ArrayList<>();
    public final List<PrestecRow> loans = new ArrayList<>();

    @Override public List<Llibre> getAllLlibres() { return new ArrayList<>(books); }
    @Override public Llibre getLlibre(long isbn) throws Exception {
        for (Llibre l : books) if (l.getISBN() == isbn) return l;
        throw new Exception("Not found: " + isbn);
    }
    @Override public int getSize() { return books.size(); }
    @Override public boolean existsLlibre(long isbn) { return books.stream().anyMatch(l -> l.getISBN() == isbn); }
    @Override public List<Llibre> get10Llibres() { return new ArrayList<>(books.subList(0, Math.min(10, books.size()))); }
    @Override public List<Llibre> get100Llibres(int index) {
        int from = index * 100, to = Math.min(from + 100, books.size());
        if (from >= books.size()) return new ArrayList<>();
        return new ArrayList<>(books.subList(from, to));
    }
    @Override public int maxIndex100Llibres() { return Math.max(0, (books.size() - 1) / 100); }
    @Override public List<Llibre> getRecentlyAdded() { return new ArrayList<>(books.subList(Math.max(0, books.size() - 20), books.size())); }
    @Override public boolean isLargeLibrary() { return false; }
    @Override public int countLlibresDB() { return books.size(); }
    @Override public List<Llibre> getLlibresPage(int offset, int pageSize) {
        int to = Math.min(offset + pageSize, books.size());
        return offset >= books.size() ? new ArrayList<>() : new ArrayList<>(books.subList(offset, to));
    }
    @Override public List<Llibre> aplicarFiltres(LlibreFilter f) { return new ArrayList<>(books); }
    @Override public List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f) { return new ArrayList<>(font); }
    @Override public List<Llibre> searchLlibresSQL(LlibreFilter f) { return new ArrayList<>(books); }
    @Override public List<Llista> getAllLlistes() { return new ArrayList<>(llistes); }
    @Override public Llista getLlistaById(int id) throws Exception {
        for (Llista l : llistes) if (l.getId() == id) return l;
        throw new Exception("Shelf not found: " + id);
    }
    @Override public int getCountInLlista(int llistaId) { return 0; }
    @Override public Map<Integer, Integer> getAllCountsInLlistes() { return new HashMap<>(); }
    @Override public List<Llibre> getLlibresInLlista(int llistaId) { return new ArrayList<>(); }
    @Override public List<LlibreLlistaRow> getAllLlibreLlistaRows() { return new ArrayList<>(); }
    @Override public List<Llista> getLlistesForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<domini.LlibreLlistaContext> getLlistesForLlibreContext(long isbn) { return new ArrayList<>(); }
    @Override public List<Tag> getAllTags() { return new ArrayList<>(tags); }
    @Override public Tag getTagById(int id) throws Exception {
        for (Tag t : tags) if (t.getId() == id) return t;
        throw new Exception("Tag not found: " + id);
    }
    @Override public List<Tag> getTagsForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<LlibreTagRow> getAllLlibreTagRows() { return new ArrayList<>(); }
    @Override public Set<Long> getLlibresWithTag(int tagId) { return new HashSet<>(); }
    @Override public Set<Long> getLoanedISBNs() {
        Set<Long> s = new HashSet<>();
        for (PrestecRow r : loans) if (!r.retornat()) s.add(r.isbn());
        return s;
    }
    @Override public List<PrestecRow> getAllActiveLoans() {
        List<PrestecRow> out = new ArrayList<>();
        for (PrestecRow r : loans) if (!r.retornat()) out.add(r);
        return out;
    }
    @Override public List<PrestecRow> getLoansForIsbn(long isbn) {
        List<PrestecRow> out = new ArrayList<>();
        for (PrestecRow r : loans) if (r.isbn() == isbn) out.add(r);
        return out;
    }
    @Override public List<Object[]> getAllOverdueLoans(int daysThreshold) { return new ArrayList<>(); }
    @Override public int countLoans(long isbn) {
        int c = 0;
        for (PrestecRow r : loans) if (r.isbn() == isbn) c++;
        return c;
    }
    @Override public byte[] getLlibreBlob(long isbn) { return null; }
    @Override public long getDbSizeBytes() { return 0; }
    @Override public List<String> getDistinctValues(String column) { return new ArrayList<>(); }
    @Override public List<String> getDistinctAutorNames() { return new ArrayList<>(); }
}
