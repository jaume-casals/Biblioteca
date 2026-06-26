package domini.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import domini.BibliotecaException;
import domini.SqlOp;
import domini.Tag;
import herramienta.i18n.I18n;

/**
 * Gestió d'Etiquetes ({@link Tag}) i operacions de relació llibre↔etiqueta.
 *
 * <p>Tots els mètodes segueixen el mateix contracte: muta primer la capa de
 * persistència, després actualitza la llista d'etiquetes en memòria i el
 * mapa d'índex per id atòmicament sota el
 * {@link StateContext#lock() lock d'estat}.
 */
public final class TagDelegate extends NamedEntityDelegate<Tag> {

    public TagDelegate(StateContext state) {
        super(state);
    }

    @Override protected List<Tag> list() { return state.tags(); }
    @Override protected Map<Integer, Tag> mapById() { return state.tagsById(); }
    @Override protected int createInDb(String nom) throws java.sql.SQLException { return state.persistence().crearTag(nom); }
    @Override protected void renameInDb(int id, String newNom) throws java.sql.SQLException { state.persistence().reanomenarTag(id, newNom); }
    @Override protected void deleteFromDb(int id) throws java.sql.SQLException { state.persistence().eliminarTag(id); }
    @Override protected Tag newEntity(int id, String nom) { return new Tag(id, nom); }

    public List<Tag> obtenirAllTags() { return new ArrayList<>(state.tags()); }

    public Tag obtenirTagById(int id) throws domini.BibliotecaException.NoTrobat {
        Tag t = state.withLockReturning(() -> state.tagsById().get(id));
        if (t == null) throw new BibliotecaException.NoTrobat("Etiqueta no trobada: " + id);
        return t;
    }

    public Tag afegirTag(String nom) {
        if (nom == null || nom.isBlank()) throw new BibliotecaException.Validacio(I18n.t("val_tag_blank"));
        return addInternal(nom);
    }

    public void eliminarTag(Tag tag) {
        deleteInternal(tag.obtenirId());
    }

    public void reanomenarTag(int id, String newNom) {
        renameInternal(id, newNom);
    }

    public Set<Long> obtenirLlibresWithTag(int tagId) { return state.persistence().obtenirLlibresWithTag(tagId); }
    public List<Tag> obtenirTagsForLlibre(long isbn) { return state.persistence().obtenirTagsForLlibre(isbn); }

    public void afegirLlibreToTag(long isbn, int tagId) {
        SqlOp.domain(() -> state.persistence().afegirLlibreToTag(isbn, tagId));
    }

    public void eliminarLlibreFromTag(long isbn, int tagId) {
        SqlOp.domain(() -> state.persistence().eliminarLlibreFromTag(isbn, tagId));
    }
}
