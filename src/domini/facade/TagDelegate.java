package domini.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domini.BibliotecaException;
import domini.Tag;
import herramienta.I18n;

/**
 * Gestió d'Etiquetes ({@link Tag}) i operacions de relació llibre↔etiqueta.
 *
 * <p>Tots els mètodes segueixen el mateix contracte: muta primer la capa de
 * persistència, després actualitza la llista d'etiquetes en memòria i el
 * mapa d'índex per id atòmicament sota el
 * {@link StateContext#lock() lock d'estat}.
 */
public final class TagDelegate {

    private final StateContext state;

    public TagDelegate(StateContext state) {
        this.state = state;
    }

    public List<Tag> obtenirAllTags() { return new ArrayList<>(state.tags()); }

    public Tag obtenirTagById(int id) throws domini.BibliotecaException.NoTrobat {
        Tag t = state.withLockReturning(() -> state.tagsById().get(id));
        if (t == null) throw new BibliotecaException.NoTrobat("Etiqueta no trobada: " + id);
        return t;
    }

    public Tag afegirTag(String nom) {
        if (nom == null || nom.isBlank()) throw new BibliotecaException.Validacio(I18n.t("val_tag_blank"));
        return state.withLockReturning(() -> {
            try {
                int id = state.persistence().crearTag(nom);
                Tag t = new Tag(id, nom);
                state.tags().add(t);
                state.tagsById().put(id, t);
                return t;
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        });
    }

    public void eliminarTag(Tag tag) {
        state.withLock(() -> {
            try {
                state.persistence().eliminarTag(tag.obtenirId());
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            state.tags().remove(tag);
            state.tagsById().remove(tag.obtenirId());
        });
    }

    public void reanomenarTag(int id, String newNom) {
        state.withLock(() -> {
            try {
                state.persistence().reanomenarTag(id, newNom);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Tag t = state.tagsById().get(id);
            if (t != null) t.posarNom(newNom);
        });
    }

    public Set<Long> obtenirLlibresWithTag(int tagId) { return state.persistence().obtenirLlibresWithTag(tagId); }
    public List<Tag> obtenirTagsForLlibre(long isbn) { return state.persistence().obtenirTagsForLlibre(isbn); }

    public void afegirLlibreToTag(long isbn, int tagId) {
        try { state.persistence().afegirLlibreToTag(isbn, tagId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void eliminarLlibreFromTag(long isbn, int tagId) {
        try { state.persistence().eliminarLlibreFromTag(isbn, tagId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }
}
