package domini.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import domini.Llibre;
import persistencia.row.LlibreLlistaRow;
import persistencia.row.LlibreTagRow;
import persistencia.row.PrestecRow;
import persistencia.row.LecturaRow;

import persistencia.dao.TagDao;
/**
 * Estadístiques de només lectura / consultes de valors diferenciats que
 * usen l'autocompletat, el panell d'estadístiques i {@code BackupService}.
 *
 * <p>{@link #getDistinctValues(String)} i {@link #getDistinctAutorNames()}
 * són els únics mètodes que toquen la captura en memòria de {@code bib};
 * la resta són simples passarel·les a la capa de persistència.
 */
public final class DelegatEstadistiques {

    /**
     * Columna en memòria → getter de Llibre, per al camí ràpid en memòria
     * de valors diferenciats. Cada entrada HA d'estar present a
     * {@code persistencia.dao.TagDao.AUTOCOMPLETE_COLUMNS} perquè la llista
     * blanca de caiguda a SQL es mantingui sincronitzada.
     */
    private static final Map<String, Function<Llibre, String>> IN_MEMORY_EXTRACTORS = Map.of(
        "editorial",        Llibre::obtenirEditorial,
        "serie",            Llibre::obtenirSerie,
        "idioma",           Llibre::obtenirIdioma,
        "format",           Llibre::obtenirFormat,
        "pais_origen",      Llibre::obtenirPaisOrigen,
        "llengua_original", Llibre::obtenirLlenguaOriginal
    );

    private final StateContext state;

    public DelegatEstadistiques(StateContext state) {
        this.state = state;
    }

    public long obtenirDbSizeBytes() { return state.persistence().obtenirDbSizeBytes(); }

    /**
     * Valors diferenciats per a l'autocompletat. Els camps String de
     * {@link Llibre} s'escanejen en memòria (biblioteca petita, ràpid);
     * la resta cau a {@code cp.getDistinctValues(column)} que consulta
     * la llista blanca de {@code persistencia.dao.TagDao.AUTOCOMPLETE_COLUMNS}.
     *
     * <p>Llista buida si la columna no és ni als extractors en memòria
     * ni a la llista blanca SQL.
     */
    public List<String> obtenirDistinctValues(String column) {
        Function<Llibre, String> extractor = IN_MEMORY_EXTRACTORS.get(column);
        if (extractor != null) {
            List<Llibre> snapshot = snapshotBibLocked();
            return snapshot.stream()
                .map(extractor)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        }
        return state.persistence().obtenirDistinctValues(column);
    }

    public List<String> obtenirDistinctAutorNames() {
        TreeSet<String> names = new TreeSet<>();
        for (Llibre l : snapshotBibLocked()) {
            // Prefereix la llista canònica d'autors; cau al camp
            // `autor` concatenat antic per als llibres que arriben
            // pel camí del validador (que només estableix la cadena
            // concatenada, no la llista).
            List<String> a = l.obtenirAutors();
            if (a != null && !a.isEmpty()) {
                a.stream().filter(s -> s != null && !s.isEmpty()).forEach(names::add);
            } else if (l.obtenirAutor() != null && !l.obtenirAutor().isBlank()) {
                for (String part : l.obtenirAutor().split(",")) {
                    String t = part.trim();
                    if (!t.isEmpty()) names.add(t);
                }
            }
        }
        return new ArrayList<>(names);
    }

    public List<LlibreLlistaRow> obtenirAllLlibreLlistaRows() { return state.persistence().obtenirAllLlibreLlista(); }
    public List<LlibreTagRow>    obtenirAllLlibreTagRows()    { return state.persistence().obtenirAllLlibreTag(); }
    public List<Object[]>        obtenirAutorsData()          { return state.persistence().obtenirAllAutors(); }
    public List<Object[]>        obtenirLlibreAutorData()     { return state.persistence().obtenirAllLlibreAutor(); }
    public List<PrestecRow>      obtenirAllPrestecs()         { return state.persistence().obtenirAllPrestecs(); }
    public List<LecturaRow>      obtenirAllLecturesData()     { return state.persistence().obtenirAllLectures(); }

    private List<Llibre> snapshotBibLocked() {
        return state.withLockReturning(() -> new ArrayList<>(state.bib()));
    }
}
