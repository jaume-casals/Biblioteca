package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import domini.LlibreLlistaRow;
import domini.LlibreTagRow;
import domini.Llista;
import domini.PrestecRow;
import domini.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Note: several methods return ArrayList<> (implementation type) instead of List<>.
// Changing to List<> would require updating all callers — deferred to avoid a large refactor.
public interface BibliotecaReader {

    ArrayList<Llibre> getAllLlibres();
    Llibre getLlibre(long isbn) throws Exception;
    int getSize();
    boolean existsLlibre(long isbn);
    ArrayList<Llibre> get10Llibres();
    ArrayList<Llibre> get100Llibres(int index);
    int maxIndex100Llibres();
    ArrayList<Llibre> getRecentlyAdded();
    boolean isLargeLibrary();
    int countLlibresDB();
    ArrayList<Llibre> getLlibresPage(int offset, int pageSize);

    ArrayList<Llibre> aplicarFiltres(LlibreFilter f);
    ArrayList<Llibre> aplicarFiltres(ArrayList<Llibre> font, LlibreFilter f);
    ArrayList<Llibre> searchLlibresSQL(LlibreFilter f);

    ArrayList<Llista> getAllLlistes();
    int getCountInLlista(int llistaId);
    Map<Integer, Integer> getAllCountsInLlistes();
    ArrayList<Llibre> getLlibresInLlista(int llistaId);
    List<LlibreLlistaRow> getAllLlibreLlistaRows();
    ArrayList<Llista> getLlistesForLlibre(long isbn);

    ArrayList<Tag> getAllTags();
    ArrayList<Tag> getTagsForLlibre(long isbn);
    List<LlibreTagRow> getAllLlibreTagRows();
    Set<Long> getLlibresWithTag(int tagId);

    Set<Long> getLoanedISBNs();
    List<PrestecRow> getAllActiveLoans();
    List<PrestecRow> getLoansForIsbn(long isbn);
    List<Object[]> getAllOverdueLoans(int daysThreshold);

    byte[] getLlibreBlob(long isbn);

    long getDbSizeBytes();
    List<String> getDistinctValues(String column);
    List<String> getDistinctAutorNames();
}
