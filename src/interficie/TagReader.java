package interficie;

import persistencia.LlibreTagRow;
import domini.Tag;
import java.util.List;
import java.util.Set;

public interface TagReader {
    List<Tag> obtenirAllTags();
    Tag obtenirTagById(int id) throws Exception;
    List<Tag> obtenirTagsForLlibre(long isbn);
    List<LlibreTagRow> obtenirAllLlibreTagRows();
    Set<Long> obtenirLlibresWithTag(int tagId);
}
