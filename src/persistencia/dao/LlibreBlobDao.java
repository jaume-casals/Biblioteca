package persistencia.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import domini.Llibre;
import persistencia.internal.MapejadorsFiles;

/** Lectura/escriptura del {@code imatge_blob} i càrrega mandrosa de {@code descripcio}/{@code notes}. */
public class LlibreBlobDao {

    private final Connection con;

    public LlibreBlobDao(Connection con) { this.con = con; }

    public byte[] obtenirBlob(long isbn) {
        return MapejadorsFiles.queryOneWithParamsOrThrow(con,
            "SELECT imatge_blob FROM llibre WHERE ISBN = ?",
            ps -> ps.setLong(1, isbn),
            "Error carregant la imatge del llibre",
            rs -> rs.getBytes(1));
    }

    public void setBlob(long isbn, byte[] blob) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llibre SET imatge_blob = ? WHERE ISBN = ?")) {
            ps.setBytes(1, blob);
            ps.setLong(2, isbn);
            ps.execute();
        }
    }

    private static void applyHeavyFields(Llibre target, ResultSet rs) throws SQLException {
        target.posarDescripcio(rs.getString("descripcio"));
        target.posarNotes(rs.getString("notes"));
        target.posarCampsPesatsCarregats(true);
    }

    public void carregarHeavyFields(long isbn, Llibre target) {
        MapejadorsFiles.useOneRowOrThrow(con,
            "SELECT descripcio, notes FROM llibre WHERE ISBN = ?",
            ps -> ps.setLong(1, isbn),
            "Error carregant camps pesats",
            rs -> applyHeavyFields(target, rs));
    }

    /**
     * Contrapartida per lots de {@link #carregarHeavyFields(long, Llibre)}: un
     * sol viatge d'anada i tornada per a N llibres en lloc de N. Els llibres
     * no trobats a la BBDD (p.ex. eliminats entre el càlcul de la llista
     * d'ISBN pel consumidor i l'execució d'aquest SELECT) se salten
     * silenciosament — la instància en memòria conserva el seu estat
     * obsolet "no carregat". Els llibres que ja són
     * {@code teCampsPesatsCarregats()} s'haurien de filtrar pel consumidor
     * (té la referència en memòria) per evitar un {@code target.posar*}
     * no-op innecessari.
     */
    public void carregarHeavyFieldsBatched(java.util.List<Long> isbns,
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
            String sql = "SELECT ISBN, descripcio, notes FROM llibre WHERE ISBN IN (" +
                MapejadorsFiles.placeholders(chunk.size()) + ")";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < chunk.size(); i++) ps.setLong(i + 1, chunk.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long isbn = rs.getLong(1);
                        Llibre target = targets.get(isbn);
                        if (target == null) continue;
                        applyHeavyFields(target, rs);
                    }
                }
            } catch (SQLException e) {
                throw new domini.BibliotecaException("Error carregant camps pesats (batch): " + e.getMessage(), e);
            }
        }
    }
}
