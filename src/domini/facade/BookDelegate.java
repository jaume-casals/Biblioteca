package domini.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import domini.BibliotecaException;
import domini.Llibre;
import domini.LlibreFilter;
import domini.SortSpec;
import herramienta.FiltreUtils;
import persistencia.ControladorPersistencia;

/**
 * Book ({@link Llibre}) CRUD, blob access, search, and sort/filter.
 *
 * <p>Owns the {@code bib} list â€” the canonical in-memory mirror of the
 * {@code llibre} table. The list is kept sorted by ISBN ascending; add /
 * update / delete preserve that invariant via
 * {@link Collections#binarySearch(List, Object, Comparator)}.
 *
 * <p>All mutations follow the same contract: write through the
 * persistence layer first, then update the in-memory list under the
 * {@link StateContext#lock() state lock}.
 *
 * <p>Known race window (not closed here â€” same as the original
 * {@code ControladorDomini}): {@code binarySearch â†’ DB â†’ in-memory} is
 * not atomic; two threads adding the same ISBN can both pass the
 * {@code binarySearch} before either commits.
 */
public final class BookDelegate {

    /** Threshold above which SQL-side search is used instead of in-memory scan. */
    private static final int SQL_FILTER_THRESHOLD = 2000;

    /** ISBN-ascending comparator; public so the facade can use it for initial sort. */
    public static final Comparator<Llibre> ISBN_COMPARATOR = (a, b) -> {
        Long ia = a.getISBN(), ib = b.getISBN();
        if (ia == null && ib == null) return 0;
        if (ia == null) return -1;
        if (ib == null) return 1;
        return ia.compareTo(ib);
    };

    private static final Map<String, Comparator<Llibre>> SORT_BY = Map.of(
        SortSpec.COL_ISBN,      ISBN_COMPARATOR,
        SortSpec.COL_NOM,       Comparator.comparing(l -> l.getNom() != null ? l.getNom() : "", String.CASE_INSENSITIVE_ORDER),
        SortSpec.COL_ANY,       Comparator.comparing(l -> l.getAny() != null ? l.getAny() : 0),
        SortSpec.COL_VALORACIO, Comparator.comparing(l -> l.getValoracio() != null ? l.getValoracio() : 0.0),
        SortSpec.COL_PREU,      Comparator.comparing(l -> l.getPreu() != null ? l.getPreu() : 0.0)
    );

    private final StateContext state;

    public BookDelegate(StateContext state) {
        this.state = state;
    }

    // â”€â”€ List views â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public List<Llibre> getAllLlibres() {
        return state.withLockReturning(() -> new ArrayList<>(state.bib()));
    }

    public List<Llibre> getUnmodifiableLlibres() {
        return state.withLockReturning(() -> Collections.unmodifiableList(new ArrayList<>(state.bib())));
    }

    public List<Llibre> get10Llibres() {
        return state.withLockReturning(() -> {
            ArrayList<Llibre> src = state.bib();
            return new ArrayList<>(src.subList(0, Math.min(10, src.size())));
        });
    }

    public List<Llibre> get100Llibres(int index) {
        return state.withLockReturning(() -> {
            ArrayList<Llibre> src = state.bib();
            int from = Math.min(100 * index, src.size());
            int to = Math.min(from + 100, src.size());
            return new ArrayList<>(src.subList(from, to));
        });
    }

    public int maxIndex100Llibres() {
        return state.withLockReturning(() -> Math.max(0, (state.bib().size() - 1) / 100));
    }

    public int getSize() {
        return state.withLockReturning(state.bib()::size);
    }

    public boolean isLargeLibrary() {
        return state.withLockReturning(() -> state.bib().size() >= SQL_FILTER_THRESHOLD);
    }

    public List<Llibre> getRecentlyAdded() { return state.persistence().getRecentlyAdded(20); }

    // â”€â”€ CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void addLlibre(Llibre l) {
        int pos = state.withLockReturning(() -> {
            int p = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (p >= 0)
                throw new BibliotecaException.Duplicate("El llibre amb ISBN: " + l.getISBN() + " ja existeix a la biblioteca");
            return -(p + 1);
        });
        try { state.persistence().afegirLlibre(l); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> state.bib().add(pos, l));
    }

    public void deleteLlibre(Llibre l) {
        int pos = state.withLockReturning(() -> {
            int p = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (p < 0) throw new BibliotecaException.NotFound("El llibre amb ISBN: " + l.getISBN() + " no existeix a la base de dades");
            return p;
        });
        try { state.persistence().eliminarLlibre(l); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> state.bib().remove(pos));
    }

    public void deleteLlibreByIsbn(Long ISBN) {
        if (ISBN == null) throw new BibliotecaException.Validation("ISBN no pot ser null");
        int pos = state.withLockReturning(() -> {
            int p = Collections.binarySearch(state.bib(), searchKey(ISBN), ISBN_COMPARATOR);
            if (p < 0) throw new BibliotecaException.NotFound("El llibre amb ISBN: " + ISBN + " no existeix a la base de dades");
            return p;
        });
        try { state.persistence().eliminarLlibre(ISBN); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> state.bib().remove(pos));
    }

    public void updateLlibre(Llibre l) {
        try { state.persistence().updateLlibre(l); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> {
            int pos = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (pos >= 0) state.bib().set(pos, l);
        });
    }

    public boolean existsLlibre(long ISBN) {
        return state.withLockReturning(() ->
            Collections.binarySearch(state.bib(), searchKey(ISBN), ISBN_COMPARATOR) >= 0);
    }

    public Llibre getLlibre(long ISBN) {
        Llibre[] holder = new Llibre[1];
        int[] indexHolder = new int[] { -1 };
        state.withLock(() -> {
            indexHolder[0] = Collections.binarySearch(state.bib(), searchKey(ISBN), ISBN_COMPARATOR);
            if (indexHolder[0] < 0)
                throw new BibliotecaException.NotFound("No existeix el llibre amb ISBN " + ISBN);
            holder[0] = state.bib().get(indexHolder[0]);
        });
        Llibre l = holder[0];
        if (!l.isHeavyFieldsLoaded()) loadHeavyFields(l);
        return l;
    }

    public void loadHeavyFields(Llibre book) {
        if (book == null || book.isHeavyFieldsLoaded()) return;
        try {
            state.persistence().loadHeavyFields(book.getISBN(), book);
            state.withLock(() -> {
                int index = Collections.binarySearch(state.bib(), book, ISBN_COMPARATOR);
                if (index >= 0) state.bib().set(index, book);
            });
        } catch (RuntimeException e) {
            throw new BibliotecaException("No s'han pogut carregar els camps pesats del llibre amb ISBN " + book.getISBN(), e);
        }
    }

    // â”€â”€ Blob (cover) access â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public byte[] getLlibreBlob(long isbn) { return state.persistence().getLlibreBlob(isbn); }

    public void setLlibreBlob(long isbn, byte[] blob) {
        try {
            state.persistence().setLlibreBlob(isbn, blob);
            state.withLock(() -> {
                int pos = Collections.binarySearch(state.bib(), searchKey(isbn), ISBN_COMPARATOR);
                if (pos >= 0) {
                    Llibre l = state.bib().get(pos);
                    l.setImatgeBlob(blob);
                    l.setHasBlob(true);
                }
            });
        }
        catch (Exception e) { throw new BibliotecaException("Failed to set cover blob: " + e.getMessage(), e); }
    }

    // â”€â”€ Search / filter â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public List<Llibre> aplicarFiltres(LlibreFilter f) {
        return state.withLockReturning(() -> {
            if (state.bib().size() >= SQL_FILTER_THRESHOLD) return searchLlibresSQL(f);
            return filterInMemory(state.bib(), f);
        });
    }

    /** SQL fallback only applies to the full library. This overload always filters in-memory. */
    public List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f) {
        return filterInMemory(font, f);
    }

    public List<Llibre> searchLlibresSQL(LlibreFilter f) { return state.persistence().searchLlibres(f, 0, 0); }

    public List<Llibre> getLlibresPage(int offset, int pageSize) {
        return state.persistence().searchLlibres(LlibreFilter.empty(), offset, pageSize);
    }

    public int countLlibresDB() { return state.persistence().countLlibres(); }

    // â”€â”€ Internals â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private ArrayList<Llibre> filterInMemory(List<Llibre> font, LlibreFilter f) {
        ControladorPersistencia cp = state.persistence();
        Set<Long> tagISBNs    = f.getTagId()    != null ? cp.getLlibresWithTag(f.getTagId())     : null;
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
        Comparator<Llibre> cmp = SORT_BY.getOrDefault(sort.column(), ISBN_COMPARATOR);
        if (!sort.ascending()) cmp = cmp.reversed();
        list.sort(cmp);
    }

    private static Llibre searchKey(long isbn) {
        return Llibre.builder().isbn(isbn).nom("").autor("").any(0)
            .descripcio("").valoracio(0.0).preu(0.0).llegit(false).imatge("").build();
    }
}
