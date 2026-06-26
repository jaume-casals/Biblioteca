package persistencia.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import persistencia.row.PrestecEndarrerit;

import persistencia.internal.ControladorPersistencia;
import persistencia.internal.MapejadorsFiles;
import persistencia.row.PrestecRow;
public class PrestecDao {

    private final Connection con;

    public PrestecDao(Connection con) { this.con = con; }

    public void add(long isbn, String nom) throws SQLException {
        // Comprova que el llibre existeix abans d'insertar — la FK ho faria
        // igual, però el missatge d'error SQL és opac. Així donem un
        // missatge clar en català.
        try (PreparedStatement check = con.prepareStatement("SELECT 1 FROM llibre WHERE ISBN = ?")) {
            check.setLong(1, isbn);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) throw new SQLException("No existeix cap llibre amb ISBN " + isbn);
            }
        }
        MapejadorsFiles.exec(con,
            "INSERT INTO prestec (isbn, nom_persona, data_prestec, retornat) VALUES (?, ?, CURRENT_DATE, FALSE)", ps -> {
                ps.setLong(1, isbn);
                ps.setString(2, nom);
            });
    }

    public void returnLoan(long isbn) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE prestec SET retornat = TRUE WHERE isbn = ? AND retornat = FALSE")) {
            ps.setLong(1, isbn);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Llibre amb l'ISBN " + isbn + " no està en prèstec");
        }
    }

    /** Tots els prèstecs, inclosos els ja retornats. Només l'usa
     *  {@link ServeiCopiaSeguretat#copiaSegToSQL} — per a la UI i l'API, usar
     *  {@link #obtenirActiveLoans()} (exposat a través de
     *  {@code ControladorPersistencia.obtenirAllActiveLoans} i
     *  {@code ControladorDomini.obtenirAllActiveLoans}). */
    /** Consulta compartida per {@link #obtenirAll} i {@link #obtenirActiveLoans}; només varia el WHERE i el missatge. */
    private List<PrestecRow> query(String sql, String errMsg) {
        try {
            return MapejadorsFiles.queryAll(con, sql, rs ->
                PrestecRow.fromStrings(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException(errMsg + e.getMessage(), e);
        }
    }

    public List<persistencia.row.PrestecRow> obtenirAll() {
        return query("SELECT isbn, nom_persona, data_prestec, retornat FROM prestec ORDER BY id",
            "Error carregant els préstecs: ");
    }

    public List<persistencia.row.PrestecRow> obtenirActiveLoans() {
        return query("SELECT isbn, nom_persona, data_prestec, retornat FROM prestec WHERE retornat = FALSE ORDER BY id",
            "Error carregant els préstecs actius: ");
    }

    public Set<Long> obtenirLoanedISBNs() {
        try {
            return new HashSet<>(MapejadorsFiles.queryAll(con,
                "SELECT DISTINCT isbn FROM prestec WHERE retornat = FALSE", rs -> rs.getLong(1)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs: " + e.getMessage(), e);
        }
    }

    public List<persistencia.row.PrestecRow> obtenirForIsbn(long isbn) {
        try {
            return MapejadorsFiles.queryWithParams(con,
                "SELECT nom_persona, data_prestec, retornat FROM prestec WHERE isbn = ? ORDER BY id",
                ps -> ps.setLong(1, isbn),
                rs -> PrestecRow.fromStrings(isbn, rs.getString(1), rs.getString(2), rs.getBoolean(3)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs per ISBN: " + e.getMessage(), e);
        }
    }

    public int count(long isbn) {
        try {
            List<Integer> r = MapejadorsFiles.queryWithParams(con,
                "SELECT COUNT(*) FROM prestec WHERE isbn = ?",
                ps -> ps.setLong(1, isbn), rs -> rs.getInt(1));
            return r.isEmpty() ? 0 : r.get(0);
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error comptant els préstecs per ISBN: " + e.getMessage(), e);
        }
    }

    public List<PrestecEndarrerit> obtenirOverdue(int daysThreshold) {
        java.sql.Date cutoff = java.sql.Date.valueOf(
            java.time.LocalDate.now().minusDays(daysThreshold));
        try {
            return MapejadorsFiles.queryWithParams(con,
                "SELECT p.nom_persona, l.nom, p.data_prestec FROM prestec p " +
                "JOIN llibre l ON p.isbn = l.ISBN " +
                "WHERE p.retornat = FALSE AND p.data_prestec < ? " +
                "ORDER BY p.data_prestec",
                ps -> ps.setDate(1, cutoff),
                rs -> PrestecEndarrerit.fromStrings(rs.getString(1), rs.getString(2), rs.getString(3)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant préstecs vençuts: " + e.getMessage(), e);
        }
    }
}
