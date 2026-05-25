package interficie;

import domini.Llibre;
import domini.LlibreFilter;
import java.util.ArrayList;
import java.util.List;

public interface BookReader {
    @Deprecated ArrayList<Llibre> getAllLlibres();
    default List<Llibre> listAllLlibres() { return getAllLlibres(); }
    Llibre getLlibre(long isbn) throws Exception;
    default java.util.Optional<Llibre> findLlibre(long isbn) {
        try { return java.util.Optional.ofNullable(getLlibre(isbn)); }
        catch (Exception e) { return java.util.Optional.empty(); }
    }
    int getSize();
    boolean existsLlibre(long isbn);
    @Deprecated ArrayList<Llibre> get10Llibres();
    default List<Llibre> list10Llibres() { return get10Llibres(); }
    @Deprecated ArrayList<Llibre> get100Llibres(int index);
    default List<Llibre> list100Llibres(int index) { return get100Llibres(index); }
    int maxIndex100Llibres();
    @Deprecated ArrayList<Llibre> getRecentlyAdded();
    default List<Llibre> listRecentlyAdded() { return getRecentlyAdded(); }
    boolean isLargeLibrary();
    int countLlibresDB();
    @Deprecated ArrayList<Llibre> getLlibresPage(int offset, int pageSize);
    default List<Llibre> listLlibresPage(int offset, int pageSize) { return getLlibresPage(offset, pageSize); }
    @Deprecated ArrayList<Llibre> aplicarFiltres(LlibreFilter f);
    default List<Llibre> listAplicarFiltres(LlibreFilter f) { return aplicarFiltres(f); }
    @Deprecated ArrayList<Llibre> aplicarFiltres(ArrayList<Llibre> font, LlibreFilter f);
    default List<Llibre> listAplicarFiltres(List<Llibre> font, LlibreFilter f) { return aplicarFiltres(new ArrayList<>(font), f); }
    @Deprecated ArrayList<Llibre> searchLlibresSQL(LlibreFilter f);
    default List<Llibre> listSearchLlibresSQL(LlibreFilter f) { return searchLlibresSQL(f); }
    byte[] getLlibreBlob(long isbn);
}
