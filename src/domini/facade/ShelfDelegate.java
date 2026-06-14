package domini.facade;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import domini.BibliotecaException;
import domini.Llibre;
import domini.LlibreLlistaContext;
import domini.Llista;
import herramienta.I18n;
import persistencia.ControladorPersistencia;

/**
 * Shelf ({@link Llista}) management and book↔shelf relation operations.
 *
 * <p>All single-DB mutations follow an atomic contract: the state
 * lock is held for the entire {@code pre-check → persistence →
 * in-memory mutate} sequence. This closes the race that existed
 * in the pre-facade {@code ControladorDomini}, where two threads
 * renaming the same shelf could both pass the pre-check, race the
 * DB (last-writer-wins), and then both mutate the in-memory map —
 * the second mutation was silently lost or the in-memory state
 * diverged from the DB.
 *
 * <p>For the move-up / move-down helpers the lock is also held
 * across the two single-row updates
 * ({@link #swapLlistesOrdreLocked}); both writes are brief and the
 * caller already holds the lock, so a release-and-reacquire dance
 * would add complexity for negligible throughput gain.
 */
public final class ShelfDelegate {

    private final StateContext state;

    public ShelfDelegate(StateContext state) {
        this.state = state;
    }

    public List<Llista> getAllLlistes() { return new ArrayList<>(state.llistes()); }

    public Llista getLlistaById(int id) throws Exception {
        Llista l = state.withLockReturning(() -> state.llistesById().get(id));
        if (l == null) throw new BibliotecaException.NotFound("Shelf not found: " + id);
        return l;
    }

    public Llista addLlista(String nom) {
        if (nom == null || nom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
        return state.withLockReturning(() -> {
            try {
                int id = state.persistence().createLlista(nom);
                Llista l = new Llista(id, nom);
                state.llistes().add(l);
                state.llistesById().put(id, l);
                return l;
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        });
    }

    public void deleteLlista(Llista llista) {
        state.withLock(() -> {
            try {
                state.persistence().deleteLlista(llista.getId());
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            state.llistes().remove(llista);
            state.llistesById().remove(llista.getId());
        });
    }

    public void renameLlista(int id, String newNom) {
        if (newNom == null || newNom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
        state.withLock(() -> {
            try {
                state.persistence().renameLlista(id, newNom);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Llista l = state.llistesById().get(id);
            if (l != null) l.setNom(newNom);
        });
    }

    public int getCountInLlista(int llistaId) { return state.persistence().getCountInLlista(llistaId); }
    public Map<Integer, Integer> getAllCountsInLlistes() { return state.persistence().getAllCountsInLlistes(); }

    public List<Llibre> getLlibresInLlista(int llistaId) { return state.persistence().getLlibresInLlista(llistaId); }
    public List<Llista> getLlistesForLlibre(long isbn) { return state.persistence().getLlistesForLlibre(isbn); }
    public List<LlibreLlistaContext> getLlistesForLlibreContext(long isbn) {
        return state.persistence().getLlistesForLlibreContext(isbn);
    }

    public void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
        try { state.persistence().addLlibreToLlista(isbn, llistaId, valoracio, llegit); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void removeLlibreFromLlista(long isbn, int llistaId) {
        try { state.persistence().removeLlibreFromLlista(isbn, llistaId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
        try { state.persistence().updateLlibreInLlista(isbn, llistaId, valoracio, llegit); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void moveLlistaUp(int id) {
        state.withLock(() -> {
            int idx = indexOfLlistaLocked(id);
            if (idx > 0) swapLlistesOrdreLocked(idx, idx - 1, id);
        });
    }

    public void moveLlistaDown(int id) {
        state.withLock(() -> {
            int idx = indexOfLlistaLocked(id);
            if (idx >= 0 && idx < state.llistes().size() - 1) swapLlistesOrdreLocked(idx, idx + 1, id);
        });
    }

    public void setLlistaColor(int id, String color) {
        if (!Llista.isValidColor(color))
            throw new BibliotecaException.Validation(I18n.t("val_color_invalid", color));
        state.withLock(() -> {
            try {
                state.persistence().updateLlistaColor(id, color);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Llista l = state.llistesById().get(id);
            if (l != null) l.setColor(color);
        });
    }

    /** Caller MUST hold the state lock. */
    private int indexOfLlistaLocked(int id) {
        for (int i = 0; i < state.llistes().size(); i++)
            if (state.llistes().get(i).getId() == id) return i;
        return -1;
    }

    /**
     * Caller MUST hold the state lock. Holds it across the two DB
     * updates as well — both are single-row writes so the lock is
     * only held for milliseconds. The original code already
     * followed this pattern; it is preserved here for consistency
     * with the rest of the delegate and because the caller
     * ({@link #moveLlistaUp} / {@link #moveLlistaDown}) is itself
     * already inside a {@code withLock} block, making a
     * release-and-reacquire dance unnecessarily complex.
     */
    private void swapLlistesOrdreLocked(int i, int j, int id) {
        int size = state.llistes().size();
        if (i < 0 || j < 0 || i >= size || j >= size) return;
        Llista a = state.llistes().get(i);
        Llista b = state.llistes().get(j);
        if (a.getId() != id && b.getId() != id) return;
        int ordreA = a.getOrdre();
        int ordreB = b.getOrdre();
        ControladorPersistencia cp = state.persistence();
        try {
            a.setOrdre(ordreB);
            b.setOrdre(ordreA);
            Collections.swap(state.llistes(), i, j);
            cp.updateLlistaOrdre(a.getId(), ordreB);
            cp.updateLlistaOrdre(b.getId(), ordreA);
        } catch (SQLException e) {
            a.setOrdre(ordreA);
            b.setOrdre(ordreB);
            Collections.swap(state.llistes(), i, j);
            throw new BibliotecaException(e.getMessage(), e);
        }
    }
}
