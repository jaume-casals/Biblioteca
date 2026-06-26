package domini.facade;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import domini.BibliotecaException;
import domini.EntitatNomenada;
import domini.SqlOp;

/**
 * Base comuna per a delegats que gestionen entitats {@link EntitatNomenada}
 * (és a dir, parell {@code id} + {@code nom}). Centralitza el patró CRUD
 * {@code withLock { SqlOp.domain(...) + llista/map mutations }} que
 * {@link DelegatPrestatgeria} i {@link TagDelegate} repetien byte a byte.
 *
 * <p>Les subclasses declaren on és emmagatzemada l'entitat (llista + mapById),
 * com es crea/canvia/elimina a la BBDD, i el constructor concret
 * ({@code Llista::new}, {@code Tag::new}).
 *
 * <p>El patró de validació (nom no buit) es queda al delegat concret perquè
 * el missatge i el subtipus de {@link BibliotecaException} varien entre
 * {@link DelegatPrestatgeria} i {@link TagDelegate}.
 */
abstract class NamedEntityDelegate<T extends EntitatNomenada> {

    protected final StateContext state;

    protected NamedEntityDelegate(StateContext state) {
        this.state = state;
    }

    protected abstract List<T> list();
    protected abstract Map<Integer, T> mapById();
    protected abstract int createInDb(String nom) throws SQLException;
    protected abstract void renameInDb(int id, String newNom) throws SQLException;
    protected abstract void deleteFromDb(int id) throws SQLException;
    protected abstract T newEntity(int id, String nom);

    protected final T addInternal(String nom) {
        return state.withLockReturning(() -> {
            try {
                int id = createInDb(nom);
                T e = newEntity(id, nom);
                list().add(e);
                mapById().put(id, e);
                return e;
            } catch (SQLException ex) { throw new BibliotecaException(ex.getMessage(), ex); }
        });
    }

    protected final void deleteInternal(int id) {
        state.withLock(() -> {
            SqlOp.domain(() -> deleteFromDb(id));
            T e = mapById().remove(id);
            if (e != null) list().remove(e);
        });
    }

    protected final void renameInternal(int id, String newNom) {
        state.withLock(() -> {
            SqlOp.domain(() -> renameInDb(id, newNom));
            T e = mapById().get(id);
            if (e != null) e.posarNom(newNom);
        });
    }
}
