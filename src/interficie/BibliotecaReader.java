package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import persistencia.LlibreLlistaRow;
import persistencia.LlibreTagRow;
import domini.Llista;
import persistencia.PrestecRow;
import domini.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BibliotecaReader {

    List<Llibre> getAllLlibres();
    Llibre getLlibre(long isbn) throws Exception;
    int getSize();
    boolean existsLlibre(long isbn);
    List<Llibre> get10Llibres();
    List<Llibre> get100Llibres(int index);
    int maxIndex100Llibres();
    List<Llibre> getRecentlyAdded();
    boolean isLargeLibrary();
    int countLlibresDB();
    List<Llibre> getLlibresPage(int offset, int pageSize);

    List<Llibre> aplicarFiltres(LlibreFilter f);
    List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f);
    List<Llibre> searchLlibresSQL(LlibreFilter f);

    List<Llista> getAllLlistes();
    int getCountInLlista(int llistaId);
    Map<Integer, Integer> getAllCountsInLlistes();
    List<Llibre> getLlibresInLlista(int llistaId);
    List<LlibreLlistaRow> getAllLlibreLlistaRows();
    List<Llista> getLlistesForLlibre(long isbn);

    List<Tag> getAllTags();
    List<Tag> getTagsForLlibre(long isbn);
    List<LlibreTagRow> getAllLlibreTagRows();
    Set<Long> getLlibresWithTag(int tagId);

    Set<Long> getLoanedISBNs();
    List<PrestecRow> getAllActiveLoans();
    List<PrestecRow> getLoansForIsbn(long isbn);
    List<Object[]> getAllOverdueLoans(int daysThreshold);
    int countLoans(long isbn);

    byte[] getLlibreBlob(long isbn);

    long getDbSizeBytes();
    List<String> getDistinctValues(String column);
    List<String> getDistinctAutorNames();

    /** Loads notes/descripcio for a book returned by a light table query. */
    default void loadHeavyFields(Llibre book) {}

    /** Read-only view for export/backup without mutation API. */
    default ExportSnapshot asExportSnapshot() {
        return new ExportSnapshot() {
            @Override public List<Llibre> books() { return getAllLlibres(); }
            @Override public List<Llista> shelves() { return getAllLlistes(); }
            @Override public List<Tag> tags() { return getAllTags(); }
            @Override public long dbSizeBytes() { return getDbSizeBytes(); }
        };
    }
}
