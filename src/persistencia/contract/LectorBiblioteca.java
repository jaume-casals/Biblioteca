package persistencia.contract;

import domini.Llibre;
import persistencia.row.PrestecRow;
import java.util.List;

public interface LectorBiblioteca extends LectorLlibre, LectorPrestatgeria, LectorEtiqueta, LectorPrestec {

    long obtenirDbSizeBytes();
    List<String> obtenirDistinctValues(String column);
    List<String> obtenirDistinctAutorNames();

    /** Carrega les notes/descripció d'un llibre retornat per una consulta de taula lleugera. */
    default void carregarHeavyFields(Llibre book) {}

    /**
     * Carrega els camps pesats de molts llibres d'una sola vegada.
     *
     * <p>Les implementacions que ho poden fer en una consulta agrupada
     * sobreescriuen aquest mètode; la implementació per defecte recorre
     * llibre a llibre.
     *
     * @param books llibres els camps pesats dels quals s'han de carregar
     */
    default void carregarHeavyFields(java.util.List<Llibre> books) {
        for (Llibre book : books) carregarHeavyFields(book);
    }
}
