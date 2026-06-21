package persistencia.contract;

import domini.Llibre;
import domini.Llista;
import persistencia.row.LlibreLlistaRow;
import java.util.List;
import java.util.Map;

public interface LectorPrestatgeria {
    List<Llista> obtenirAllLlistes();
    Llista obtenirLlistaById(int id) throws domini.BibliotecaException.NoTrobat;
    int obtenirCountInLlista(int llistaId);
    Map<Integer, Integer> obtenirAllCountsInLlistes();
    List<Llibre> obtenirLlibresInLlista(int llistaId);
    List<LlibreLlistaRow> obtenirAllLlibreLlistaRows();
    List<Llista> obtenirLlistesForLlibre(long isbn);
    List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn);
}
