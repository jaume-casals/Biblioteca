package interficie;

import persistencia.OverdueLoan;
import persistencia.PrestecRow;
import java.util.List;
import java.util.Set;

public interface LoanReader {
    Set<Long> obtenirLoanedISBNs();
    List<PrestecRow> obtenirAllActiveLoans();
    List<PrestecRow> obtenirLoansForIsbn(long isbn);
    List<OverdueLoan> obtenirAllOverdueLoans(int daysThreshold);
    int comptarLoans(long isbn);
}
