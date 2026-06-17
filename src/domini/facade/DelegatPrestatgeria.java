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
public final class DelegatPrestatgeria {

    private final StateContext state;

    public DelegatPrestatgeria(StateContext state) {
        this.state = state;
    }

    public List<Llista> obtenirAllLlistes() { return new ArrayList<>(state.llistes()); }

    public Llista obtenirLlistaById(int id) throws Exception {
        Llista l = state.withLockReturning(() -> state.llistesById().get(id));
        if (l == null) throw new BibliotecaException.NoTrobat("Shelf not found: " + id);
        return l;
    }

    public Llista afegirLlista(String nom) {
        if (nom == null || nom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
        return state.withLockReturning(() -> {
            try {
                int id = state.persistence().crearLlista(nom);
                Llista l = new Llista(id, nom);
                state.llistes().add(l);
                state.llistesById().put(id, l);
                return l;
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        });
    }

    public void eliminarLlista(Llista llista) {
        state.withLock(() -> {
            try {
                state.persistence().eliminarLlista(llista.obtenirId());
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            state.llistes().remove(llista);
            state.llistesById().remove(llista.obtenirId());
        });
    }

    public void reanomenarLlista(int id, String newNom) {
        if (newNom == null || newNom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
        state.withLock(() -> {
            try {
                state.persistence().reanomenarLlista(id, newNom);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Llista l = state.llistesById().get(id);
            if (l != null) l.posarNom(newNom);
        });
    }

    public int obtenirCountInLlista(int llistaId) { return state.persistence().obtenirCountInLlista(llistaId); }
    public Map<Integer, Integer> obtenirAllCountsInLlistes() { return state.persistence().obtenirAllCountsInLlistes(); }

    public List<Llibre> obtenirLlibresInLlista(int llistaId) { return state.persistence().obtenirLlibresInLlista(llistaId); }
    public List<Llista> obtenirLlistesForLlibre(long isbn) { return state.persistence().obtenirLlistesForLlibre(isbn); }
    public List<LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) {
        return state.persistence().obtenirLlistesForLlibreContext(isbn);
    }

    public void afegirLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
        try { state.persistence().afegirLlibreToLlista(isbn, llistaId, valoracio, llegit); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void eliminarLlibreFromLlista(long isbn, int llistaId) {
        try { state.persistence().eliminarLlibreFromLlista(isbn, llistaId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void actualitzarLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) {
        try { state.persistence().actualitzarLlibreInLlista(isbn, llistaId, valoracio, llegit); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void moureLlistaUp(int id) {
        state.withLock(() -> {
            int idx = indexOfLlistaLocked(id);
            if (idx > 0) swapLlistesOrdreLocked(idx, idx - 1, id);
        });
    }

    public void moureLlistaDown(int id) {
        state.withLock(() -> {
            int idx = indexOfLlistaLocked(id);
            if (idx >= 0 && idx < state.llistes().size() - 1) swapLlistesOrdreLocked(idx, idx + 1, id);
        });
    }

    public void posarLlistaColor(int id, String color) {
        if (!Llista.esValidColor(color))
            throw new BibliotecaException.Validacio(I18n.t("val_color_invalid", color));
        state.withLock(() -> {
            try {
                state.persistence().actualitzarLlistaColor(id, color);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Llista l = state.llistesById().get(id);
            if (l != null) l.setColor(color);
        });
    }

    /** Caller MUST hold the state lock. */
    private int indexOfLlistaLocked(int id) {
        for (int i = 0; i < state.llistes().size(); i++)
            if (state.llistes().get(i).obtenirId() == id) return i;
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
        if (a.obtenirId() != id && b.obtenirId() != id) return;
        int ordreA = a.obtenirOrdre();
        int ordreB = b.obtenirOrdre();
        ControladorPersistencia cp = state.persistence();
        try {
            a.posarOrdre(ordreB);
            b.posarOrdre(ordreA);
            Collections.swap(state.llistes(), i, j);
            cp.actualitzarLlistaOrdre(a.obtenirId(), ordreB);
            cp.actualitzarLlistaOrdre(b.obtenirId(), ordreA);
        } catch (SQLException e) {
            a.posarOrdre(ordreA);
            b.posarOrdre(ordreB);
            Collections.swap(state.llistes(), i, j);
            throw new BibliotecaException(e.getMessage(), e);
        }
    }
}
