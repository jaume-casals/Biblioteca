package persistencia.contract;

import persistencia.row.LlibreTagRow;
import domini.Tag;
import java.util.List;
import java.util.Set;

public interface LectorEtiqueta {
    List<Tag> obtenirAllTags();
    Tag obtenirTagById(int id) throws domini.BibliotecaException.NoTrobat;
    List<Tag> obtenirTagsForLlibre(long isbn);
    List<LlibreTagRow> obtenirAllLlibreTagRows();
    Set<Long> obtenirLlibresWithTag(int tagId);
}
