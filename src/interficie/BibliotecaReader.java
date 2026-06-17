package interficie;

import domini.Llibre;
import persistencia.PrestecRow;
import java.util.List;

public interface BibliotecaReader extends BookReader, ShelfReader, TagReader, LoanReader {

    byte[] obtenirLlibreBlob(long isbn);

    long obtenirDbSizeBytes();
    List<String> obtenirDistinctValues(String column);
    List<String> obtenirDistinctAutorNames();

    /** Loads notes/descripcio for a book returned by a light table query. */
    default void carregarHeavyFields(Llibre book) {}
}
