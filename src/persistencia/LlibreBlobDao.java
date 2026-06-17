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

    public synchronized byte[] obtenirBlob(long isbn) {
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

    public synchronized void carregarHeavyFields(long isbn, Llibre target) {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT descripcio, notes FROM llibre WHERE ISBN = ?")) {
            ps.setLong(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    target.posarDescripcio(rs.getString("descripcio"));
                    target.posarNotes(rs.getString("notes"));
                    target.posarHeavyFieldsLoaded(true);
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
    public synchronized void carregarHeavyFieldsBatched(java.util.List<Long> isbns,
                                                    java.util.Map<Long, Llibre> targets) {
        if (isbns == null || isbns.isEmpty()) return;
        // Construeix una sola consulta "IN (?, ?, …)". El recompte de
        // placeholders està limitat per mantenar la cadena SQL manejable
        // — els trossos de 500 estan dins dels límits de les llistes
        // IN d'H2 / MariaDB i eviten que l'array de paràmetres del
        // prepared statement faci explotar el pressupost de memòria
        // del driver JDBC.
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
                        target.posarDescripcio(rs.getString("descripcio"));
                        target.posarNotes(rs.getString("notes"));
                        target.posarHeavyFieldsLoaded(true);
                    }
                }
            } catch (SQLException e) {
                throw new domini.BibliotecaException("Error carregant camps pesats (batch): " + e.getMessage(), e);
            }
        }
    }
}
