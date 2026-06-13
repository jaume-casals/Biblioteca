package domini.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import domini.Llibre;
import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import persistencia.PrestecRow;
import persistencia.LecturaRow;

/**
 * Read-only stats / distinct-value queries used by autocompletion,
 * the statistics panel, and {@code BackupService}.
 *
 * <p>{@link #getDistinctValues(String)} and {@link #getDistinctAutorNames()}
 * are the only methods that touch the in-memory {@code bib} snapshot; the
 * rest are pure pass-throughs to the persistence layer.
 */
public final class StatsDelegate {

    /**
     * In-memory column → Llibre getter, for the in-memory distinct-values
     * fast path. Each entry MUST be present in
     * {@code persistencia.TagDao.AUTOCOMPLETE_COLUMNS} so the SQL fallback
     * whitelist stays in sync.
     */
    private static final Map<String, Function<Llibre, String>> IN_MEMORY_EXTRACTORS = Map.of(
        "editorial",        Llibre::getEditorial,
        "serie",            Llibre::getSerie,
        "idioma",           Llibre::getIdioma,
        "format",           Llibre::getFormat,
        "pais_origen",      Llibre::getPaisOrigen,
        "llengua_original", Llibre::getLlenguaOriginal
    );

    private final StateContext state;

    public StatsDelegate(StateContext state) {
        this.state = state;
    }

    public long getDbSizeBytes() { return state.persistence().getDbSizeBytes(); }

    /**
     * Distinct values for autocomplete. String fields of {@link Llibre}
     * are scanned in memory (small library, fast); everything else falls
     * through to {@code cp.getDistinctValues(column)} which queries the
     * whitelist in {@code persistencia.TagDao.AUTOCOMPLETE_COLUMNS}.
     *
     * <p>Empty list if the column is neither in the in-memory extractors
     * nor the SQL whitelist.
     */
    public List<String> getDistinctValues(String column) {
        Function<Llibre, String> extractor = IN_MEMORY_EXTRACTORS.get(column);
        if (extractor != null) {
            List<Llibre> snapshot = state.withLockReturning(() -> new ArrayList<>(state.bib()));
            return snapshot.stream()
                .map(extractor)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        }
        return state.persistence().getDistinctValues(column);
    }

    public List<String> getDistinctAutorNames() {
        TreeSet<String> names = new TreeSet<>();
        List<Llibre> snapshot = state.withLockReturning(() -> new ArrayList<>(state.bib()));
        for (Llibre l : snapshot) {
            List<String> a = l.getAutors();
            if (a != null) a.stream().filter(s -> s != null && !s.isEmpty()).forEach(names::add);
        }
        return new ArrayList<>(names);
    }

    public List<LlibreLlistaRow> getAllLlibreLlistaRows() { return state.persistence().getAllLlibreLlista(); }
    public List<LlibreTagRow>    getAllLlibreTagRows()    { return state.persistence().getAllLlibreTag(); }
    public List<Object[]>        getAutorsData()          { return state.persistence().getAllAutors(); }
    public List<Object[]>        getLlibreAutorData()     { return state.persistence().getAllLlibreAutor(); }
    public List<PrestecRow>      getAllPrestecs()         { return state.persistence().getAllPrestecs(); }
    public List<LecturaRow>      getAllLecturesData()     { return state.persistence().getAllLectures(); }
}
