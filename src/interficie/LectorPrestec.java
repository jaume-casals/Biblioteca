package interficie;

import persistencia.PrestecEndarrerit;
import persistencia.PrestecRow;
import java.util.List;
import java.util.Set;

public interface LectorPrestec {
    Set<Long> obtenirLoanedISBNs();
    List<PrestecRow> obtenirAllActiveLoans();
    List<PrestecRow> obtenirLoansForIsbn(long isbn);
    List<PrestecEndarrerit> obtenirAllOverdueLoans(int daysThreshold);
    int comptarLoans(long isbn);
}
