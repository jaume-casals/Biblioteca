package interficie;

import domini.Llibre;
import domini.Llista;
import persistencia.LlibreLlistaRow;
import java.util.List;
import java.util.Map;

public interface ShelfReader {
    List<Llista> obtenirAllLlistes();
    Llista obtenirLlistaById(int id) throws Exception;
    int obtenirCountInLlista(int llistaId);
    Map<Integer, Integer> obtenirAllCountsInLlistes();
    List<Llibre> obtenirLlibresInLlista(int llistaId);
    List<LlibreLlistaRow> obtenirAllLlibreLlistaRows();
    List<Llista> obtenirLlistesForLlibre(long isbn);
    List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn);
}
