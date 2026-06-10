package persistencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrestecDao {

    private final Connection con;

    PrestecDao(Connection con) { this.con = con; }

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
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO prestec (isbn, nom_persona, data_prestec, retornat) VALUES (?, ?, CURRENT_DATE, FALSE)")) {
            ps.setLong(1, isbn);
            ps.setString(2, nom);
            ps.execute();
        }
    }

    public void returnLoan(long isbn) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE prestec SET retornat = TRUE WHERE isbn = ? AND retornat = FALSE")) {
            ps.setLong(1, isbn);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Book with ISBN " + isbn + " is not on loan");
        }
    }

    /** Tots els prèstecs, inclosos els ja retornats. Només l'usa
     *  {@link BackupService#backupToSQL} — per a la UI i l'API, usar
     *  {@link #getAllActiveLoans()} (exposat a través de
     *  {@code ControladorPersistencia.getAllActiveLoans} i
     *  {@code ControladorDomini.getAllActiveLoans}). */
    public List<persistencia.PrestecRow> getAll() {
        List<persistencia.PrestecRow> rows = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, nom_persona, data_prestec, retornat FROM prestec ORDER BY id")) {
                while (rs.next())
                    rows.add(persistencia.PrestecRow.fromStrings(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs: " + e.getMessage(), e);
        }
        return rows;
    }

    public List<persistencia.PrestecRow> getActiveLoans() {
        List<persistencia.PrestecRow> rows = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, nom_persona, data_prestec, retornat FROM prestec WHERE retornat = FALSE ORDER BY id")) {
                while (rs.next())
                    rows.add(persistencia.PrestecRow.fromStrings(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs actius: " + e.getMessage(), e);
        }
        return rows;
    }

    public Set<Long> getLoanedISBNs() {
        Set<Long> set = new HashSet<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT DISTINCT isbn FROM prestec WHERE retornat = FALSE")) {
                while (rs.next()) set.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs: " + e.getMessage(), e);
        }
        return set;
    }

    public List<persistencia.PrestecRow> getForIsbn(long isbn) {
        List<persistencia.PrestecRow> rows = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT nom_persona, data_prestec, retornat FROM prestec WHERE isbn = ? ORDER BY id")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        rows.add(persistencia.PrestecRow.fromStrings(isbn, rs.getString(1), rs.getString(2), rs.getBoolean(3)));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els préstecs per ISBN: " + e.getMessage(), e);
        }
        return rows;
    }

    public int count(long isbn) {
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM prestec WHERE isbn = ?")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error comptant els préstecs per ISBN: " + e.getMessage(), e);
        }
        return 0;
    }

    public List<Object[]> getOverdue(int daysThreshold) {
        List<Object[]> rows = new ArrayList<>();
        try {
            java.sql.Date cutoff = java.sql.Date.valueOf(
                java.time.LocalDate.now().minusDays(daysThreshold));
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT p.nom_persona, l.nom, p.data_prestec FROM prestec p " +
                    "JOIN llibre l ON p.isbn = l.ISBN " +
                    "WHERE p.retornat = FALSE AND p.data_prestec < ? " +
                    "ORDER BY p.data_prestec")) {
                ps.setDate(1, cutoff);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next())
                        rows.add(new Object[]{ rs.getString(1), rs.getString(2), rs.getString(3) });
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant préstecs vençuts: " + e.getMessage(), e);
        }
        return rows;
    }
}
