package domini.facade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import persistencia.ControladorPersistencia;

/**
 * Shared in-memory state for the {@link domini.ControladorDomini} facade.
 *
 * <p>Package-private to {@code domini.facade}; only the delegates and the
 * facade itself touch this. Exposes the lock, the three backing lists
 * ({@code bib}, {@code llistes}, {@code tags}), the id-index maps
 * ({@code llistesById}, {@code tagsById}), and the persistence reference.
 *
 * <p>All reads and writes of the backing lists MUST go through the
 * {@link #lock} so the lists and the id-index maps stay in sync. The
 * {@link #withLock} and {@link #withLockReturning} helpers are the
 * canonical way to do that.
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

    public Object lock() { return lock; }
    public ControladorPersistencia persistence() { return cp; }
    /** Returns the live backing list. Callers MUST hold {@link #lock()} before
     *  reading or mutating; iterating without the lock risks
     *  {@link java.util.ConcurrentModificationException} and mutating without
     *  the lock can desync the list from the id-index maps. Prefer
     *  {@link #withLock} / {@link #withLockReturning} for read+copy patterns. */
    public ArrayList<Llibre> bib() { return bib; }
    /** See {@link #bib()} for the locking contract. */
    public ArrayList<Llista> llistes() { return llistes; }
    /** See {@link #bib()} for the locking contract. */
    public ArrayList<Tag> tags() { return tags; }
    public Map<Integer, Llista> llistesById() { return llistesById; }
    public Map<Integer, Tag> tagsById() { return tagsById; }

    /** Replace all three backing lists atomically (used by restoreFromSQL). */
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

    /** Clear all three backing lists and the id-index maps (used by clearAll).
     *  Mirrors {@link #replaceAll(ArrayList, ArrayList, ArrayList)}'s contract
     *  by going through {@link #rebuildIdIndexesLocked()} for consistency
     *  (today the two paths clear the maps directly, but the centralised
     *  call makes future additions — e.g. a new id-index map — a single
     *  line change rather than a two-line search). */
    public void clearAll() {
        synchronized (lock) {
            bib.clear();
            llistes.clear();
            tags.clear();
            rebuildIdIndexesLocked();
        }
    }

    /** Rebuild id-index maps from the current backing lists. Must be called under lock. */
    private void rebuildIdIndexesLocked() {
        llistesById.clear();
        for (Llista l : llistes) llistesById.put(l.getId(), l);
        tagsById.clear();
        for (Tag t : tags) tagsById.put(t.getId(), t);
    }

    /** Run {@code action} while holding the state lock. */
    public void withLock(Runnable action) {
        synchronized (lock) { action.run(); }
    }

    /** Run {@code action} while holding the state lock; return its result. */
    public <T> T withLockReturning(java.util.function.Supplier<T> action) {
        synchronized (lock) { return action.get(); }
    }
}
