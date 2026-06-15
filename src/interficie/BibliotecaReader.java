package interficie;

import domini.Llibre;
import persistencia.PrestecRow;
import java.util.List;

public interface BibliotecaReader extends BookReader, ShelfReader, TagReader, LoanReader {

    int countLoans(long isbn);

    byte[] getLlibreBlob(long isbn);

    long getDbSizeBytes();
    List<String> getDistinctValues(String column);
    List<String> getDistinctAutorNames();

    /** Loads notes/descripcio for a book returned by a light table query. */
    default void loadHeavyFields(Llibre book) {}
}
