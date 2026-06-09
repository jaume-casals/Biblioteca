package domini.facade;

import java.util.ArrayList;
import java.util.List;
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
     * <p>Empty list if the column is neither in the in-memory switch nor
     * the SQL whitelist. The two sets must stay in sync; the in-memory
     * switch is a subset of the whitelist.
     */
    public List<String> getDistinctValues(String column) {
        Function<Llibre, String> extractor = switch (column) {
            case "editorial"        -> Llibre::getEditorial;
            case "serie"            -> Llibre::getSerie;
            case "idioma"           -> Llibre::getIdioma;
            case "format"           -> Llibre::getFormat;
            case "pais_origen"      -> Llibre::getPaisOrigen;
            case "llengua_original" -> Llibre::getLlenguaOriginal;
            default                 -> null;
        };
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
            if (a != null && !a.isEmpty()) {
                a.stream().filter(s -> s != null && !s.isEmpty()).forEach(names::add);
            } else if (l.getAutor() != null && !l.getAutor().isEmpty()) {
                names.add(l.getAutor());
            }
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
