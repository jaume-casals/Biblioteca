package domini.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domini.BibliotecaException;
import domini.Tag;
import herramienta.I18n;

/**
 * Tag ({@link Tag}) management and book↔tag relation operations.
 *
 * <p>All methods follow the same contract: mutate the persistence layer
 * first, then update the in-memory tag list + id-index map atomically
 * under the {@link StateContext#lock() state lock}.
 */
public final class TagDelegate {

    private final StateContext state;

    public TagDelegate(StateContext state) {
        this.state = state;
    }

    public List<Tag> getAllTags() { return new ArrayList<>(state.tags()); }

    public Tag getTagById(int id) throws Exception {
        Tag t = state.withLockReturning(() -> state.tagsById().get(id));
        if (t == null) throw new BibliotecaException.NotFound("Tag not found: " + id);
        return t;
    }

    public Tag addTag(String nom) {
        if (nom == null || nom.isBlank()) throw new BibliotecaException.Validation(I18n.t("val_tag_blank"));
        return state.withLockReturning(() -> {
            try {
                int id = state.persistence().createTag(nom);
                Tag t = new Tag(id, nom);
                state.tags().add(t);
                state.tagsById().put(id, t);
                return t;
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
        });
    }

    public void deleteTag(Tag tag) {
        state.withLock(() -> {
            try {
                state.persistence().deleteTag(tag.getId());
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            state.tags().remove(tag);
            state.tagsById().remove(tag.getId());
        });
    }

    public void renameTag(int id, String newNom) {
        state.withLock(() -> {
            try {
                state.persistence().renameTag(id, newNom);
            } catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
            Tag t = state.tagsById().get(id);
            if (t != null) t.setNom(newNom);
        });
    }

    public Set<Long> getLlibresWithTag(int tagId) { return state.persistence().getLlibresWithTag(tagId); }
    public List<Tag> getTagsForLlibre(long isbn) { return state.persistence().getTagsForLlibre(isbn); }

    public void addLlibreToTag(long isbn, int tagId) {
        try { state.persistence().addLlibreToTag(isbn, tagId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void removeLlibreFromTag(long isbn, int tagId) {
        try { state.persistence().removeLlibreFromTag(isbn, tagId); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }
}
