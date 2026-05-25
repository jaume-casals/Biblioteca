package interficie;

public interface LoanWriter extends LoanReader {
    void prestarLlibre(long isbn, String nom);
    void retornarLlibre(long isbn);
}
