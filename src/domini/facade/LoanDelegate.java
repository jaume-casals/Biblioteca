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
public final class LoanDelegate {

    private final StateContext state;

    public LoanDelegate(StateContext state) {
        this.state = state;
    }

    public void prestarLlibre(long isbn, String nom) {
        try { state.persistence().addPrestec(isbn, nom); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public void retornarLlibre(long isbn) {
        try { state.persistence().returnPrestec(isbn); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }

    public Set<Long> getLoanedISBNs() { return state.persistence().getLoanedISBNs(); }
    public List<PrestecRow> getAllActiveLoans() { return state.persistence().getAllActiveLoans(); }
    public List<PrestecRow> getLoansForIsbn(long isbn) { return state.persistence().getLoansForIsbn(isbn); }
    public List<persistencia.OverdueLoan> getAllOverdueLoans(int daysThreshold) { return state.persistence().getAllOverdueLoans(daysThreshold); }
    public int countLoans(long isbn) { return state.persistence().countLoans(isbn); }
}
