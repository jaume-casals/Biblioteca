package interficie;

import domini.Llibre;
import persistencia.PrestecRow;
import java.util.List;

public interface LectorBiblioteca extends LectorLlibre, LectorPrestatgeria, LectorEtiqueta, LectorPrestec {

    long obtenirDbSizeBytes();
    List<String> obtenirDistinctValues(String column);
    List<String> obtenirDistinctAutorNames();

    /** Carrega les notes/descripció d'un llibre retornat per una consulta de taula lleugera. */
    default void carregarHeavyFields(Llibre book) {}
}
