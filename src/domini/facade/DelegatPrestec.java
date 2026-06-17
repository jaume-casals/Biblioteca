package domini.facade;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import domini.BibliotecaException;
import persistencia.PrestecRow;

/**
 * Loan ({@link PrestecRow}) management and loan queries.
 *
 * <p>All operations delegate to the persistence layer; loan state is
 * not held in memory (it lives entirely in the DB). Operations translate
 * {@link SQLException} into {@link BibliotecaException} for callers.
 */
public final class DelegatPrestec {

    private final StateContext state;

    public DelegatPrestec(StateContext state) {
        this.state = state;
    }

    public void prestarLlibre(long isbn, String nom) {
        try { state.persistence().afegirPrestec(isbn, nom); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void retornarLlibre(long isbn) {
        try { state.persistence().returnPrestec(isbn); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public Set<Long> obtenirLoanedISBNs() { return state.persistence().obtenirLoanedISBNs(); }
    public List<PrestecRow> obtenirAllActiveLoans() { return state.persistence().obtenirAllActiveLoans(); }
    public List<PrestecRow> obtenirLoansForIsbn(long isbn) { return state.persistence().obtenirLoansForIsbn(isbn); }
    public List<persistencia.OverdueLoan> obtenirAllOverdueLoans(int daysThreshold) { return state.persistence().obtenirAllOverdueLoans(daysThreshold); }
    public int comptarLoans(long isbn) { return state.persistence().comptarLoans(isbn); }
}
