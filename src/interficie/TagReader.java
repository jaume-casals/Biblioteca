package interficie;

import persistencia.LlibreTagRow;
import domini.Tag;
import java.util.List;
import java.util.Set;

public interface TagReader {
    List<Tag> getAllTags();
    Tag getTagById(int id) throws Exception;
    List<Tag> getTagsForLlibre(long isbn);
    List<LlibreTagRow> getAllLlibreTagRows();
    Set<Long> getLlibresWithTag(int tagId);
}
