package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import java.util.List;

public interface BookReader {
    List<Llibre> getAllLlibres();
    Llibre getLlibre(long isbn) throws Exception;
    default java.util.Optional<Llibre> findLlibre(long isbn) {
        try { return java.util.Optional.ofNullable(getLlibre(isbn)); }
        catch (Exception e) { return java.util.Optional.empty(); }
    }
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
    byte[] getLlibreBlob(long isbn);
}
