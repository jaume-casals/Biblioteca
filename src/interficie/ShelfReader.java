package interficie;

import domini.Llibre;
import domini.Llista;
import persistencia.LlibreLlistaRow;
import java.util.List;
import java.util.Map;

public interface ShelfReader {
    List<Llista> getAllLlistes();
    Llista getLlistaById(int id) throws Exception;
    int getCountInLlista(int llistaId);
    Map<Integer, Integer> getAllCountsInLlistes();
    List<Llibre> getLlibresInLlista(int llistaId);
    List<LlibreLlistaRow> getAllLlibreLlistaRows();
    List<Llista> getLlistesForLlibre(long isbn);
    List<domini.LlibreLlistaContext> getLlistesForLlibreContext(long isbn);
}
