package persistencia.contract;

import domini.Llibre;
import domini.LlibreFilter;
import persistencia.row.LlibreLlistaRow;
import persistencia.row.LlibreTagRow;
import domini.Llista;
import persistencia.row.PrestecRow;
import domini.Tag;

import java.util.*;

import persistencia.row.PrestecEndarrerit;
/**
 * Pure-in-memory {@link LectorBiblioteca} for fast UI / pure-logic tests that don't need JDBC.
 * Read-only; mutate the supplied lists between operations.
 *
 * <p>Note: {@link #aplicarFiltres} returns the full list unchanged — this
 * class does NOT honour filter criteria. Tests of filter logic must use a
 * real {@link LectorBiblioteca} (e.g. via {@code ControladorDomini}).
 */
public class LectorBibliotecaEnMemoria implements LectorBiblioteca {

    // Llistes privades — l'accés extern passa pels mètodes afegirXxx/
    // eliminarXxx que mantenen l'índex {@code byIsbn} consistent. Un
    // getter de només lectura ({@link #llibresReadOnly}) s'exposa per a
    // les proves que volen iterar sense mutar.
    private final List<Llibre> books = new ArrayList<>();
    private final List<Llista> llistes = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();
    private final List<PrestecRow> loans = new ArrayList<>();
    private final Map<Long, Llibre> byIsbn = new HashMap<>();
    private boolean byIsbnDirty = true;

    /** Vista de només lectura de la llista de llibres. Útil per a proves
     *  que iteren però no muten — qualsevol intent de modificar-la
     *  llença {@link UnsupportedOperationException}. */
    public List<Llibre> llibresReadOnly() { return Collections.unmodifiableList(books); }
    public List<Llista> llistesReadOnly() { return Collections.unmodifiableList(llistes); }
    public List<Tag>    tagsReadOnly()    { return Collections.unmodifiableList(tags); }
    public List<PrestecRow> loansReadOnly() { return Collections.unmodifiableList(loans); }

    /** Afegeix un llibre a la col·lecció i invalida l'índex per ISBN. */
    public void addLlibre(Llibre l) {
        books.add(l);
        byIsbnDirty = true;
    }
    public void removeLlibre(Llibre l) {
        books.remove(l);
        byIsbnDirty = true;
    }
    public void addLlista(Llista l)   { llistes.add(l); }
    public void removeLlista(Llista l){ llistes.remove(l); }
    public void addTag(Tag t)         { tags.add(t); }
    public void removeTag(Tag t)      { tags.remove(t); }
    public void addLoan(PrestecRow r) { loans.add(r); }
    public void removeLoan(PrestecRow r) { loans.remove(r); }

    @Override public List<Llibre> obtenirAllLlibres() { return new ArrayList<>(books); }
    @Override public Llibre obtenirLlibre(long isbn) throws domini.BibliotecaException.NoTrobat {
        if (byIsbnDirty) rebuildIndex();
        Llibre l = byIsbn.get(isbn);
        if (l == null) throw new domini.BibliotecaException.NoTrobat("No trobat: " + isbn);
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
    private List<Llibre> slice(int from, int to) {
        return from >= books.size()
            ? new ArrayList<>()
            : new ArrayList<>(books.subList(from, Math.min(to, books.size())));
    }

    @Override public List<Llibre> get100Llibres(int index) {
        int from = index * 100;
        return slice(from, from + 100);
    }
    @Override public int maxIndex100Llibres() { return Math.max(0, (books.size() - 1) / 100); }
    @Override public List<Llibre> obtenirRecentlyAdded() { return new ArrayList<>(books.subList(Math.max(0, books.size() - 20), books.size())); }
    @Override public boolean esLargeLibrary() { return false; }
    @Override public int comptarLlibresDB() { return books.size(); }
    @Override public List<Llibre> obtenirLlibresPage(int offset, int pageSize) {
        return slice(offset, offset + pageSize);
    }
    private static <T> T noSuportat(String que) {
        throw new UnsupportedOperationException(
            "LectorBibliotecaEnMemoria does not honour " + que + " — use a real LectorBiblioteca (see class Javadoc).");
    }

    @Override public List<Llibre> aplicarFiltres(LlibreFilter f) { return noSuportat("filter criteria"); }
    @Override public List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f) { return noSuportat("filter criteria"); }
    @Override public List<Llibre> cercarLlibresSQL(LlibreFilter f) { return noSuportat("search criteria"); }
    @Override public List<Llista> obtenirAllLlistes() { return new ArrayList<>(llistes); }
    @Override public Llista obtenirLlistaById(int id) throws domini.BibliotecaException.NoTrobat {
        for (Llista l : llistes) if (l.obtenirId() == id) return l;
        throw new domini.BibliotecaException.NoTrobat("Prestatge no trobat: " + id);
    }
    @Override public int obtenirCountInLlista(int llistaId) { return 0; }
    @Override public Map<Integer, Integer> obtenirAllCountsInLlistes() { return new HashMap<>(); }
    @Override public List<Llibre> obtenirLlibresInLlista(int llistaId) { return new ArrayList<>(); }
    @Override public List<LlibreLlistaRow> obtenirAllLlibreLlistaRows() { return new ArrayList<>(); }
    @Override public List<Llista> obtenirLlistesForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) { return new ArrayList<>(); }
    @Override public List<Tag> obtenirAllTags() { return new ArrayList<>(tags); }
    @Override public Tag obtenirTagById(int id) throws domini.BibliotecaException.NoTrobat {
        for (Tag t : tags) if (t.obtenirId() == id) return t;
        throw new domini.BibliotecaException.NoTrobat("Etiqueta no trobada: " + id);
    }
    @Override public List<Tag> obtenirTagsForLlibre(long isbn) { return new ArrayList<>(); }
    @Override public List<LlibreTagRow> obtenirAllLlibreTagRows() { return new ArrayList<>(); }
    @Override public Set<Long> obtenirLlibresWithTag(int tagId) { return new HashSet<>(); }
    private java.util.stream.Stream<PrestecRow> activeLoansStream() {
        return loans.stream().filter(r -> !r.retornat());
    }

    private java.util.stream.Stream<PrestecRow> loansForIsbnStream(long isbn) {
        return loans.stream().filter(r -> r.isbn() == isbn);
    }

    @Override public Set<Long> obtenirLoanedISBNs() {
        return activeLoansStream().map(PrestecRow::isbn)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }
    @Override public List<PrestecRow> obtenirAllActiveLoans() {
        return activeLoansStream().toList();
    }
    @Override public List<PrestecRow> obtenirLoansForIsbn(long isbn) {
        return loansForIsbnStream(isbn).toList();
    }
    @Override public List<persistencia.row.PrestecEndarrerit> obtenirAllOverdueLoans(int daysThreshold) { return new ArrayList<>(); }
    @Override public int comptarLoans(long isbn) {
        return (int) loansForIsbnStream(isbn).count();
    }
    @Override public byte[] obtenirLlibreBlob(long isbn) { return new byte[0]; }
    @Override public long obtenirDbSizeBytes() { return 0; }
    @Override public List<String> obtenirDistinctValues(String column) { return new ArrayList<>(); }
    @Override public List<String> obtenirDistinctAutorNames() { return new ArrayList<>(); }
}
