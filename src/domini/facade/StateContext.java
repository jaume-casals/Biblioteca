package domini.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import persistencia.internal.ControladorPersistencia;

/**
 * Estat compartit en memòria per a la façana {@link domini.ControladorDomini}.
 *
 * <p>Package-private a {@code domini.facade}; només els delegats i la
 * pròpia façana hi accedeixen. Exposa el lock, les tres llistes de suport
 * ({@code bib}, {@code llistes}, {@code tags}), els mapes d'índex per id
 * ({@code llistesById}, {@code tagsById}) i la referència a la persistència.
 *
 * <p>Totes les lectures i escriptures de les llistes de suport HAN DE passar
 * pel {@link #lock} perquè les llistes i els mapes d'índex per id es
 * mantinguin sincronitzats. Els helpers {@link #withLock} i
 * {@link #withLockReturning} són la manera canònica de fer-ho.
 */
public final class StateContext {

    private final Object lock = new Object();

    private ArrayList<Llibre> bib;
    private ArrayList<Llista> llistes;
    private ArrayList<Tag> tags;

    private final Map<Integer, Llista> llistesById = new HashMap<>();
    private final Map<Integer, Tag> tagsById = new HashMap<>();

    private final ControladorPersistencia cp;

    public StateContext(ControladorPersistencia cp,
                        ArrayList<Llibre> bib,
                        ArrayList<Llista> llistes,
                        ArrayList<Tag> tags) {
        this.cp = cp;
        this.bib = bib;
        this.llistes = llistes;
        this.tags = tags;
        rebuildIdIndexesLocked();
    }

    /** El monitor utilitzat per {@link #withLock} / {@link #withLockReturning}.
     *  Package-private (sense getter públic): cada delegat de
     *  {@code domini.facade} passa per {@code withLock(...)} de manera
     *  que no hi ha cap raó perquè un consumidor agafi el lock directament.
     *  L'accés package-private es preserva per al paquet de la façana
     *  (p.ex. tests que necessiten un {@code synchronized (state.lock) { ... }}
     *  directe), però la superfície pública es manté neta. */
    Object lock() { return lock; }
    public ControladorPersistencia persistence() { return cp; }
    /** Retorna la llista de suport viva. Els consumidors HAN DE tenir
     *  {@link #lock()} agafat abans de llegir o mutar; iterar sense el lock
     *  pot provocar {@link java.util.ConcurrentModificationException} i
     *  mutar sense el lock pot desincronitzar la llista respecte dels
     *  mapes d'índex per id. Per a patrons de lectura + còpia, preferiu
     *  {@link #withLock} / {@link #withLockReturning}. */
    public ArrayList<Llibre> bib() { return bib; }
    /** Consulta {@link #bib()} per al contracte de lock. */
    public ArrayList<Llista> llistes() { return llistes; }
    /** Consulta {@link #bib()} per al contracte de lock. */
    public ArrayList<Tag> tags() { return tags; }
    public Map<Integer, Llista> llistesById() { return llistesById; }
    public Map<Integer, Tag> tagsById() { return tagsById; }

    /** Captura defensiva atòmica de les tres llistes de suport sota el lock. */
    public record Snapshot(ArrayList<Llibre> bib, ArrayList<Llista> llistes, ArrayList<Tag> tags) {}

    /** Pren una còpia defensiva de {@code bib}, {@code llistes} i {@code tags} sota el lock. */
    public Snapshot snapshotAll() {
        synchronized (lock) {
            return new Snapshot(new ArrayList<>(bib), new ArrayList<>(llistes), new ArrayList<>(tags));
        }
    }

    /** Substitueix les tres llistes de suport atòmicament (usat per restaurarFromSQL). */
    public void replaceAll(ArrayList<Llibre> newBib,
                           ArrayList<Llista> newLlistes,
                           ArrayList<Tag> newTags) {
        synchronized (lock) {
            this.bib = newBib;
            this.llistes = newLlistes;
            this.tags = newTags;
            rebuildIdIndexesLocked();
        }
    }

    /** Buida les tres llistes de suport i els mapes d'índex per id (usat per netejarAll).
     *  Delega a {@link #replaceAll(ArrayList, ArrayList, ArrayList)} amb llistes
     *  buides perquè ambdós camins comparteixin un sol cos — futures addicions
     *  (p.ex. un nou mapa d'índex per id) són un canvi d'una sola línia. */
    public void netejarAll() {
        replaceAll(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /** Reconstrueix els mapes d'índex per id a partir de les llistes de suport actuals. S'ha de cridar amb el lock agafat. */
    private void rebuildIdIndexesLocked() {
        llistesById.clear();
        for (Llista l : llistes) llistesById.put(l.obtenirId(), l);
        tagsById.clear();
        for (Tag t : tags) tagsById.put(t.obtenirId(), t);
    }

    /** Executa {@code action} amb el lock d'estat agafat. */
    public void withLock(Runnable action) {
        synchronized (lock) { action.run(); }
    }

    /** Executa {@code action} amb el lock d'estat agafat; en retorna el resultat. */
    public <T> T withLockReturning(java.util.function.Supplier<T> action) {
        synchronized (lock) { return action.get(); }
    }
}
