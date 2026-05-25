package interficie;

import domini.Llibre;
import domini.Llista;
import persistencia.LlibreLlistaRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ShelfReader {
    @Deprecated ArrayList<Llista> getAllLlistes();
    default List<Llista> listAllLlistes() { return getAllLlistes(); }
    Llista getLlistaById(int id) throws Exception;
    int getCountInLlista(int llistaId);
    Map<Integer, Integer> getAllCountsInLlistes();
    @Deprecated ArrayList<Llibre> getLlibresInLlista(int llistaId);
    default List<Llibre> listLlibresInLlista(int llistaId) { return getLlibresInLlista(llistaId); }
    List<LlibreLlistaRow> getAllLlibreLlistaRows();
    @Deprecated ArrayList<Llista> getLlistesForLlibre(long isbn);
    default List<Llista> listLlistesForLlibre(long isbn) { return getLlistesForLlibre(isbn); }
}
