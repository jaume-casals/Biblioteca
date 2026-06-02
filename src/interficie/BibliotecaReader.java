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

    /** Read-only view for export/backup without mutation API. */
    default ExportSnapshot asExportSnapshot() {
        return new ExportSnapshot() {
            @Override public List<Llibre> books() { return getAllLlibres(); }
            @Override public List<domini.Llista> shelves() { return getAllLlistes(); }
            @Override public List<domini.Tag> tags() { return getAllTags(); }
            @Override public long dbSizeBytes() { return getDbSizeBytes(); }
        };
    }
}
