package interficie;

import persistencia.LlibreTagRow;
import domini.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface TagReader {
    @Deprecated ArrayList<Tag> getAllTags();
    default List<Tag> listAllTags() { return getAllTags(); }
    Tag getTagById(int id) throws Exception;
    @Deprecated ArrayList<Tag> getTagsForLlibre(long isbn);
    default List<Tag> listTagsForLlibre(long isbn) { return getTagsForLlibre(isbn); }
    List<LlibreTagRow> getAllLlibreTagRows();
    Set<Long> getLlibresWithTag(int tagId);
}
