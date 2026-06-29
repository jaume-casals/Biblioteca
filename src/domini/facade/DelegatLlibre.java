package domini.facade;

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
import domini.EspecificacioOrdenacio;
import domini.SqlOp;
import herramienta.text.FiltreUtils;
import persistencia.internal.ControladorPersistencia;

/**
 * CRUD de llibres ({@link Llibre}), accés a blobs, cerca i ordenació/filtra.
 *
 * <p>És propietària de la llista {@code bib} — la rèplica en memòria canònica
 * de la taula {@code llibre}. La llista es manté ordenada per ISBN ascendent;
 * add / update / delete conserven aquesta invariante via
 * {@link Collections#binarySearch(List, Object, Comparator)}.
 *
 * <p>Totes les mutacions segueixen un contracte atòmic: el lock d'estat es
 * manté durant tota la seqüència {@code binarySearch → persistència → mutació
 * en memòria}. Mantenir el lock durant la crida JDBC és intencionat — cada
 * crida és una escriptura d'una sola fila i el lock és breu. L'antic patró
 * "alliberar entre la BBDD i la mutació" (que feia servir
 * {@code ControladorDomini} abans de la divisió en façanes) deixava una
 * finestra en què dos fils podien passar cadascun el
 * {@code binarySearch}, competir per la BBDD, i l'add(pos, …) /
 * set(pos, …) en memòria del perdedor corrompia la llista (índex
 * incorrecte, referència obsoleta o — per a
 * {@link #setLlibreBlob} — un salt silenciós quan {@code pos < 0}).
 * La forma atòmica tanca aquesta finestra.
 */
public final class DelegatLlibre {

    /** Llindar a partir del qual la cerca es fa al costat SQL en lloc d'un escaneig en memòria. */
    private static final int SQL_FILTER_THRESHOLD = 2000;

    /** Comparador d'ISBN ascendent; públic perquè la façana el pugui fer servir per a l'ordenació inicial. */
    public static final Comparator<Llibre> ISBN_COMPARATOR = EspecificacioOrdenacio.ISBN_COMPARATOR;

    /** Comparadors d'ordenació en memòria indexats pel nom de columna de l'EspecificacioOrdenacio; provenen de {@link EspecificacioOrdenacio}. */
    private static final Map<String, Comparator<Llibre>> SORT_BY = EspecificacioOrdenacio.comparators();

    private final StateContext state;

    public DelegatLlibre(StateContext state) {
        this.state = state;
    }

    // ── List views ────────────────────────────────────────────────────────────

    public List<Llibre> obtenirAllLlibres() {
        return state.withLockReturning(() -> new ArrayList<>(state.bib()));
    }

    public List<Llibre> obtenirUnmodifiableLlibres() {
        return Collections.unmodifiableList(obtenirAllLlibres());
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
        return comptarLlibresDB();
    }

    public boolean esLargeLibrary() {
        return comptarLlibresDB() >= SQL_FILTER_THRESHOLD;
    }

    public List<Llibre> obtenirRecentlyAdded() { return state.persistence().obtenirRecentlyAdded(20); }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public void afegirLlibre(Llibre l) {
        state.withLock(() -> {
            int p = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (p >= 0)
                throw new BibliotecaException.Duplicat("El llibre amb ISBN: " + l.obtenirISBN() + " ja existeix a la biblioteca");
            SqlOp.domain(() -> state.persistence().afegirLlibre(l));
            state.bib().add(-(p + 1), l);
        });
    }

    public void eliminarLlibre(Llibre l) {
        state.withLock(() -> {
            int p = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (p < 0) throw notFoundEnBaseDades(l.obtenirISBN());
            SqlOp.domain(() -> state.persistence().eliminarLlibre(l));
            state.bib().remove(p);
        });
    }

    public void eliminarLlibreByIsbn(Long ISBN) {
        if (ISBN == null) throw new BibliotecaException.Validacio("ISBN no pot ser null");
        state.withLock(() -> {
            int p = binarySearchByIsbn(state.bib(), ISBN);
            if (p < 0) throw notFoundEnBaseDades(ISBN);
            SqlOp.domain(() -> state.persistence().eliminarLlibre(ISBN));
            state.bib().remove(p);
        });
    }

    public void actualitzarLlibre(Llibre l) {
        state.withLock(() -> {
            int pos = Collections.binarySearch(state.bib(), l, ISBN_COMPARATOR);
            if (pos < 0)
                throw notFoundEnBaseDades(l.obtenirISBN());
            SqlOp.domain(() -> state.persistence().actualitzarLlibre(l));
            state.bib().set(pos, l);
        });
    }

    public boolean existsLlibre(long ISBN) {
        return state.withLockReturning(() -> binarySearchByIsbn(state.bib(), ISBN) >= 0);
    }

    public Llibre obtenirLlibre(long ISBN) {
        return state.withLockReturning(() -> {
            int idx = binarySearchByIsbn(state.bib(), ISBN);
            if (idx < 0)
                throw new BibliotecaException.NoTrobat("No existeix el llibre amb ISBN " + ISBN);
            Llibre l = state.bib().get(idx);
            if (!l.teCampsPesatsCarregats()) loadHeavyInto(l);
            return l;
        });
    }

    public void carregarHeavyFields(Llibre book) {
        if (book == null || book.teCampsPesatsCarregats()) return;
        state.withLock(() -> {
            loadHeavyInto(book);
            // La referència `book` pot no ser la que emmagatzema bib() a
            // la ranura de l'ISBN corresponent (una updateLlibre concurrent
            // l'ha pogut substituir), però el DAO ja ha mutat `book` in
            // situ. La ranura actual conserva la seva pròpia referència;
            // la propera getLlibre() la tornarà a carregar sota demanda
            // si la referència de la seva ranura encara és la lleugera.
            // Acceptable: els camps pesats són contingut immutable
            // (descripció/notes), i updateLlibre els conserva via
            // bindUpdateableFields, de manera que la ranura ja porta
            // els valors més recents.
        });
    }

    // ── Blob (cover) access ───────────────────────────────────────────────────

    public byte[] obtenirLlibreBlob(long isbn) { return state.persistence().obtenirLlibreBlob(isbn); }

    public void posarLlibreBlob(long isbn, byte[] blob) {
        state.withLock(() -> {
            int pos = binarySearchByIsbn(state.bib(), isbn);
            if (pos < 0)
                throw new BibliotecaException.NoTrobat("El llibre amb ISBN " + isbn + " no existeix a la biblioteca");
            try {
                state.persistence().posarLlibreBlob(isbn, blob);
            } catch (Exception e) {
                throw new BibliotecaException("No s'ha pogut desar el blob de coberta: " + e.getMessage(), e);
            }
            Llibre l = state.bib().get(pos);
            l.posarImatgeBlob(blob);
            l.posarHasBlob(true);
        });
    }

    // ── Search / filter ──────────────────────────────────────────────────────

    public List<Llibre> aplicarFiltres(LlibreFilter f) {
        return state.withLockReturning(() -> {
            if (state.bib().size() >= SQL_FILTER_THRESHOLD) return cercarLlibresSQL(f);
            return filtrarInMemory(state.bib(), f);
        });
    }

    /** La caiguda a SQL només s'aplica a la biblioteca sencera. Aquesta sobrecàrrega sempre filtra en memòria. */
    public List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f) {
        return filtrarInMemory(font, f);
    }

    public List<Llibre> cercarLlibresSQL(LlibreFilter f) { return state.persistence().cercarLlibres(f, 0, 0); }

    public List<Llibre> obtenirLlibresPage(int offset, int pageSize) {
        return state.persistence().cercarLlibres(LlibreFilter.empty(), offset, pageSize);
    }

    public int comptarLlibresDB() {
        return state.withLockReturning(state.bib()::size);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void loadHeavyInto(Llibre l) {
        try {
            state.persistence().carregarHeavyFields(l.obtenirISBN(), l);
        } catch (RuntimeException e) {
            throw new BibliotecaException("No s'han pogut carregar els camps pesats del llibre amb ISBN " + l.obtenirISBN(), e);
        }
    }

    private ArrayList<Llibre> filtrarInMemory(List<Llibre> font, LlibreFilter f) {
        ControladorPersistencia cp = state.persistence();
        Set<Long> tagISBNs    = f.obtenirTagId()    != null ? cp.obtenirLlibresWithTag(f.obtenirTagId())     : null;
        Set<Long> llistaISBNs = f.obtenirLlistaId() != null ? cp.obtenirISBNsInLlista(f.obtenirLlistaId()) : null;
        ArrayList<Llibre> resultat = new ArrayList<>();
        for (Llibre l : font) {
            if (FiltreUtils.matches(l, f, tagISBNs, llistaISBNs)) {
                resultat.add(l);
            }
        }
        aplicarSort(resultat, f);
        return resultat;
    }

    private static void aplicarSort(ArrayList<Llibre> list, LlibreFilter f) {
        EspecificacioOrdenacio sort = f.obtenirSort();
        if (sort == null || list.size() < 2) return;
        Comparator<Llibre> cmp = EspecificacioOrdenacio.comparator(sort.column());
        if (!sort.ascending()) cmp = cmp.reversed();
        list.sort(cmp);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareByIsbn(Object l, long isbn) {
        Long i = ((Llibre) l).obtenirISBN();
        return i == null ? -1 : Long.compare(i, isbn);
    }

    private static int binarySearchByIsbn(ArrayList<Llibre> bib, long isbn) {
        return Collections.binarySearch(bib, (Object) isbn, (l, k) -> compareByIsbn(l, (long) k));
    }

    private static int binarySearchByIsbn(ArrayList<Llibre> bib, Long isbn) {
        return binarySearchByIsbn(bib, isbn == null ? -1L : isbn);
    }

    private static BibliotecaException.NoTrobat notFoundEnBaseDades(long isbn) {
        return new BibliotecaException.NoTrobat("El llibre amb ISBN: " + isbn + " no existeix a la base de dades");
    }
}
