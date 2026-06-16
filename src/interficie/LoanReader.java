package interficie;

import persistencia.OverdueLoan;
import persistencia.PrestecRow;
import java.util.List;
import java.util.Set;

public interface LoanReader {
    Set<Long> getLoanedISBNs();
    List<PrestecRow> getAllActiveLoans();
    List<PrestecRow> getLoansForIsbn(long isbn);
    List<OverdueLoan> getAllOverdueLoans(int daysThreshold);
    int countLoans(long isbn);
}
