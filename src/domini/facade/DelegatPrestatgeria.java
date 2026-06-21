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
import herramienta.i18n.I18n;
import persistencia.internal.ControladorPersistencia;

/**
 * Gestió de Prestatgeries ({@link Llista}) i operacions de relació llibre↔prestatge.
 *
 * <p>Totes les mutacions individuals a la BBDD segueixen un contracte atòmic:
 * el lock d'estat es manté durant tota la seqüència
 * {@code pre-comprovació → persistència → mutació en memòria}. Això tanca la
 * cursa que existia a l'antic {@code ControladorDomini} (abans de la divisió
 * en façanes), en què dos fils reanonenant el mateix prestatge podien passar
 * cadascun la pre-comprovació, competir per la BBDD (l'últim en escriure
 * guanya), i tot seguit mutar tots dos el mapa en memòria — la segona
 * mutació es perdia silenciosament o l'estat en memòria es desviava de la BBDD.
 *
 * <p>Per als helpers de moure amunt / moure avall el lock també es manté
 * durant les dues actualitzacions individuals
 * ({@link #swapLlistesOrdreLocked}); ambdues escriptures són breus i el
 * consumidor ja té el lock agafat, de manera que una operació
 * d'alliberar i tornar a agafar afegiria complexitat per un guany de
 * rendiment negligible.
 */
public final class DelegatPrestatgeria {

    private final StateContext state;

    public DelegatPrestatgeria(StateContext state) {
        this.state = state;
    }

    public List<Llista> obtenirAllLlistes() {
        return state.withLockReturning(() -> new ArrayList<>(state.llistes()));
    }

    public Llista obtenirLlistaById(int id) throws domini.BibliotecaException.NoTrobat {
        Llista l = state.withLockReturning(() -> state.llistesById().get(id));
        if (l == null) throw new BibliotecaException.NoTrobat("Prestatge no trobat: " + id);
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
            if (l != null) l.posarColor(color);
        });
    }

    /** El consumidor HA DE tenir el lock d'estat agafat. */
    private int indexOfLlistaLocked(int id) {
        for (int i = 0; i < state.llistes().size(); i++)
            if (state.llistes().get(i).obtenirId() == id) return i;
        return -1;
    }

    /**
     * El consumidor HA DE tenir el lock d'estat agafat. El manté durant les
     * dues actualitzacions de la BBDD també — totes dues són escriptures
     * d'una sola fila, de manera que el lock només es manté durant
     * mil·lisegons. El codi original ja seguia aquest patró; es preserva
     * aquí per consistència amb la resta del delegat i perquè el consumidor
     * ({@link #moureLlistaUp} / {@link #moureLlistaDown}) ja es troba dins
     * d'un bloc {@code withLock}, cosa que fa innecessàriament complexa
     * una operació d'alliberar i tornar a agafar.
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
