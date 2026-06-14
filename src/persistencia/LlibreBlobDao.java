package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import domini.Llibre;

/** Read/write of the {@code imatge_blob} and lazy {@code descripcio}/{@code notes} load. */
public class LlibreBlobDao {

    private final Connection con;

    LlibreBlobDao(Connection con) { this.con = con; }

    public synchronized byte[] getBlob(long isbn) {
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT imatge_blob FROM llibre WHERE ISBN = ?")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getBytes(1);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant la imatge del llibre: " + e.getMessage(), e);
        }
        return null;
    }

    public synchronized void setBlob(long isbn, byte[] blob) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llibre SET imatge_blob = ? WHERE ISBN = ?")) {
            ps.setBytes(1, blob);
            ps.setLong(2, isbn);
            ps.execute();
        }
    }

    public synchronized void loadHeavyFields(long isbn, Llibre target) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT descripcio, notes FROM llibre WHERE ISBN = ?")) {
            ps.setLong(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    target.setDescripcio(rs.getString("descripcio"));
                    target.setNotes(rs.getString("notes"));
                    target.setHeavyFieldsLoaded(true);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant camps pesats: " + e.getMessage(), e);
        }
    }

    /**
     * Batched counterpart to {@link #loadHeavyFields(long, Llibre)}: one
     * round-trip for N books instead of N. Books not found in the DB
     * (e.g. deleted between the caller computing the ISBN list and this
     * SELECT running) are silently skipped — the in-memory instance
     * keeps its stale "not loaded" state. Books that are already
     * {@code isHeavyFieldsLoaded()} should be filtered out by the caller
     * (it has the in-memory reference) to avoid an unnecessary
     * {@code target.set*} no-op.
     */
    public synchronized void loadHeavyFieldsBatched(java.util.List<Long> isbns,
                                                    java.util.Map<Long, Llibre> targets) {
        if (isbns == null || isbns.isEmpty()) return;
        // Build a single "IN (?, ?, …)" query. Placeholder count capped
        // to keep the SQL string manageable — chunks of 500 are well
        // within H2 / MariaDB IN-list limits and keep the prepared
        // statement's parameter array from blowing up the JDBC driver
        // memory budget.
        final int CHUNK = 500;
        for (int from = 0; from < isbns.size(); from += CHUNK) {
            int to = Math.min(from + CHUNK, isbns.size());
            java.util.List<Long> chunk = isbns.subList(from, to);
            StringBuilder sql = new StringBuilder("SELECT ISBN, descripcio, notes FROM llibre WHERE ISBN IN (");
            for (int i = 0; i < chunk.size(); i++) sql.append(i == 0 ? "?" : ",?");
            sql.append(")");
            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                for (int i = 0; i < chunk.size(); i++) ps.setLong(i + 1, chunk.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long isbn = rs.getLong(1);
                        Llibre target = targets.get(isbn);
                        if (target == null) continue;
                        target.setDescripcio(rs.getString("descripcio"));
                        target.setNotes(rs.getString("notes"));
                        target.setHeavyFieldsLoaded(true);
                    }
                }
            } catch (SQLException e) {
                throw new domini.BibliotecaException("Error carregant camps pesats (batch): " + e.getMessage(), e);
            }
        }
    }
}
