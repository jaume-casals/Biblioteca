package domini.facade;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import domini.BibliotecaException;
import domini.SqlOp;
import persistencia.row.PrestecRow;

import persistencia.row.PrestecEndarrerit;
/**
 * Gestió de Préstecs ({@link PrestecRow}) i consultes sobre els préstecs.
 *
 * <p>Totes les operacions deleguen a la capa de persistència; l'estat dels
 * préstecs no es manté en memòria (viu enterament a la BBDD). Les operacions
 * tradueixen {@link SQLException} en {@link BibliotecaException} per als
 * consumidors.
 */
public final class DelegatPrestec {

    private final StateContext state;

    public DelegatPrestec(StateContext state) {
        this.state = state;
    }

    public void prestarLlibre(long isbn, String nom) {
        SqlOp.domain(() -> state.persistence().afegirPrestec(isbn, nom));
    }

    public void retornarLlibre(long isbn) {
        SqlOp.domain(() -> state.persistence().returnPrestec(isbn));
    }

    public Set<Long> obtenirLoanedISBNs() { return state.persistence().obtenirLoanedISBNs(); }
    public List<PrestecRow> obtenirAllActiveLoans() { return state.persistence().obtenirAllActiveLoans(); }
    public List<PrestecRow> obtenirLoansForIsbn(long isbn) { return state.persistence().obtenirLoansForIsbn(isbn); }
    public List<persistencia.row.PrestecEndarrerit> obtenirAllOverdueLoans(int daysThreshold) { return state.persistence().obtenirAllOverdueLoans(daysThreshold); }
    public int comptarLoans(long isbn) { return state.persistence().comptarLoans(isbn); }
}
