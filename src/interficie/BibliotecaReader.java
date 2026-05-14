package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import domini.Llista;
import domini.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    ArrayList<Llibre> getLlibresInLlista(int llistaId);
    ArrayList<Llista> getLlistesForLlibre(long isbn);

    ArrayList<Tag> getAllTags();
    ArrayList<Tag> getTagsForLlibre(long isbn);

    Set<Long> getLoanedISBNs();
    List<Object[]> getLoansForIsbn(long isbn);
    List<Object[]> getAllOverdueLoans(int daysThreshold);

    byte[] getLlibreBlob(long isbn);

    long getDbSizeBytes();
    List<String> getDistinctValues(String column);
    List<String> getDistinctAutorNames();
}
