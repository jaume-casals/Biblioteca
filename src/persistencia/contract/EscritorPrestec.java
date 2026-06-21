package persistencia.contract;

public interface EscritorPrestec extends LectorPrestec {
    void prestarLlibre(long isbn, String nom);
    void retornarLlibre(long isbn);
}
