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
 * <p>All methods follow the same contract: mutate the persistence layer
 * first, then update the in-memory lists + id-index maps atomically under
 * the {@link StateContext#lock() state lock}.
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
        try {
            int id = state.persistence().createLlista(nom);
            Llista l = new Llista(id, nom);
            state.withLock(() -> {
                state.llistes().add(l);
                state.llistesById().put(id, l);
            });
            return l;
        } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void deleteLlista(Llista llista) {
        try { state.persistence().deleteLlista(llista.getId()); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> {
            state.llistes().remove(llista);
            state.llistesById().remove(llista.getId());
        });
    }

    public void renameLlista(int id, String newNom) {
        if (newNom == null || newNom.isBlank()) throw new BibliotecaException("El nom del prestatge no pot estar buit");
        try { state.persistence().renameLlista(id, newNom); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> {
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
        try { state.persistence().updateLlistaColor(id, color); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        state.withLock(() -> {
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

    /** Caller MUST hold the state lock. */
    private void swapLlistesOrdreLocked(int i, int j, int id) {
        ControladorPersistencia cp = state.persistence();
        int size = state.llistes().size();
        if (i < 0 || j < 0 || i >= size || j >= size) return;
        Llista a = state.llistes().get(i);
        Llista b = state.llistes().get(j);
        if (a.getId() != id && b.getId() != id) return;
        int ordreA = a.getOrdre();
        int ordreB = b.getOrdre();
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
