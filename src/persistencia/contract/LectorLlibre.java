package persistencia.contract;

import domini.Llibre;
import domini.LlibreFilter;
import java.util.List;

public interface LectorLlibre {
    List<Llibre> obtenirAllLlibres();
    Llibre obtenirLlibre(long isbn) throws domini.BibliotecaException.NoTrobat;
    default java.util.Optional<Llibre> cercarLlibre(long isbn) {
        try { return java.util.Optional.ofNullable(obtenirLlibre(isbn)); }
        catch (domini.BibliotecaException.NoTrobat e) { return java.util.Optional.empty(); }
    }
    int getSize();
    boolean existsLlibre(long isbn);
    List<Llibre> get10Llibres();
    List<Llibre> get100Llibres(int index);
    int maxIndex100Llibres();
    List<Llibre> obtenirRecentlyAdded();
    boolean esLargeLibrary();
    int comptarLlibresDB();
    List<Llibre> obtenirLlibresPage(int offset, int pageSize);
    List<Llibre> aplicarFiltres(LlibreFilter f);
    List<Llibre> aplicarFiltres(List<Llibre> font, LlibreFilter f);
    List<Llibre> cercarLlibresSQL(LlibreFilter f);
    byte[] obtenirLlibreBlob(long isbn);

    /**
     * Lean variant of {@link #getAllLlibres()}: same result, but explicitly
     * documented to skip heavy text/blob columns (descripcio, notes, cover).
     * Default delegates to {@code getAllLlibres} for backends that already
     * return a light view (in-memory tests) or where the cost is acceptable.
     */
    default List<Llibre> obtenirAllLlibresSummary() { return obtenirAllLlibres(); }
}
