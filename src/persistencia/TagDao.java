package persistencia;

import domini.Tag;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TagDao {

    private final Connection con;

    /**
     * Whitelist de columnes vàlides per a {@link #getDistinctValues(String)}.
     * El nom de la columna s'interpola directament a la SQL (no hi ha
     * paràmetre per a un nom de columna en JDBC), per això la llista blanca
     * és <em>obligatòria</em> — sense ella, una entrada no validada seria
     * un vector d'SQL injection. Mantenir sincronitzada amb el switch en
     * memòria de {@code domini.facade.StatsDelegate.IN_MEMORY_EXTRACTORS}
     * — la verificació a {@link #verifyColumnWhitelistSync()} corre al
     * constructor per fallar aviat si la llista queda desincronitzada.
     */
    public static final Set<String> AUTOCOMPLETE_COLUMNS = Set.of(
        "editorial", "serie", "idioma", "pais_origen", "format", "llengua_original");

    /**
     * Startup assertion: every column the in-memory path in
     * {@code StatsDelegate.IN_MEMORY_EXTRACTORS} knows about must also be
     * in the SQL whitelist. Without this check the two paths drift
     * silently — a column added to the in-memory extractors but not the
     * SQL whitelist would yield a smaller result set when the SQL path
     * is exercised (e.g. for a column that doesn't have an in-memory
     * extractor at all). Reference is by string to keep this DAO
     * ignorant of the facade layer (which would create a cycle).
     */
    static {
        try {
            Class<?> statsCls = Class.forName("domini.facade.StatsDelegate");
            java.lang.reflect.Field f = statsCls.getDeclaredField("IN_MEMORY_EXTRACTORS");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, ?> mem = (java.util.Map<String, ?>) f.get(null);
            java.util.Set<String> memKeys = mem.keySet();
            java.util.Set<String> missing = new java.util.HashSet<>(memKeys);
            missing.removeAll(AUTOCOMPLETE_COLUMNS);
            if (!missing.isEmpty()) {
                throw new ExceptionInInitializerError(
                    "TagDao.AUTOCOMPLETE_COLUMNS is missing the in-memory extractors: " + missing
                    + " (the SQL whitelist must be a superset of the in-memory map)");
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // StatsDelegate may be absent in test classpaths; the assert
            // is best-effort. The drift is still caught by code review.
        }
    }

    private volatile java.util.List<LlibreTagRow> llibreTagCache;

    TagDao(Connection con) { this.con = con; }

    void invalidateLlibreTagCache() { llibreTagCache = null; }

    public synchronized ArrayList<Tag> getAll() {
        ArrayList<Tag> tags = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, nom FROM tag ORDER BY nom")) {
                while (rs.next()) tags.add(new Tag(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les etiquetes: " + e.getMessage(), e);
        }
        return tags;
    }

    public synchronized int create(String nom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tag (nom) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("createTag: no generated key returned");
                return rs.getInt(1);
            }
        }
    }

    public synchronized void delete(int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tag WHERE id = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }
        invalidateLlibreTagCache();
    }

    public synchronized void rename(int id, String newNom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE tag SET nom = ? WHERE id = ?")) {
            ps.setString(1, newNom);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public synchronized ArrayList<Tag> getForLlibre(long isbn) {
        ArrayList<Tag> tags = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT t.id, t.nom FROM tag t JOIN llibre_tag lt ON t.id = lt.tag_id WHERE lt.isbn = ? ORDER BY t.nom")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) tags.add(new Tag(rs.getInt(1), rs.getString(2)));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les etiquetes del llibre: " + e.getMessage(), e);
        }
        return tags;
    }

    public synchronized void addToLlibre(long isbn, int tagId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO llibre_tag (isbn, tag_id) VALUES (?, ?)")) {
            ps.setLong(1, isbn);
            ps.setInt(2, tagId);
            ps.execute();
        }
        invalidateLlibreTagCache();
    }

    public synchronized void removeFromLlibre(long isbn, int tagId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM llibre_tag WHERE isbn = ? AND tag_id = ?")) {
            ps.setLong(1, isbn);
            ps.setInt(2, tagId);
            ps.execute();
        }
        invalidateLlibreTagCache();
    }

    public synchronized Set<Long> getLlibresWithTag(int tagId) {
        Set<Long> isbns = new HashSet<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT isbn FROM llibre_tag WHERE tag_id = ?")) {
                ps.setInt(1, tagId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) isbns.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres de l'etiqueta: " + e.getMessage(), e);
        }
        return isbns;
    }

    public synchronized java.util.List<LlibreTagRow> getAllLlibreTag() {
        if (llibreTagCache != null) return llibreTagCache;
        java.util.List<LlibreTagRow> rows = new java.util.ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, tag_id FROM llibre_tag ORDER BY tag_id, isbn")) {
                while (rs.next())
                    rows.add(new LlibreTagRow(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les dades d'etiquetes: " + e.getMessage(), e);
        }
        llibreTagCache = java.util.List.copyOf(rows);
        return llibreTagCache;
    }

    /**
     * Camí SQL per a {@code getDistinctValues}: consulta la taula {@code llibre}
     * amb {@code SELECT DISTINCT}. La columna ha de ser a
     * {@link #AUTOCOMPLETE_COLUMNS} (llista blanca) — qualsevol altra columna
     * retorna llista buida. La capa de domini ({@code ControladorDomini}) té un
     * camí alternatiu en memòria per a columnes que ja són a {@code Llibre};
     * veure'n el Javadoc per a la justificació del doble camí.
     */
    public synchronized java.util.List<String> getDistinctValues(String column) {
        java.util.List<String> vals = new java.util.ArrayList<>();
        if (!AUTOCOMPLETE_COLUMNS.contains(column)) return vals;
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT DISTINCT `" + column + "` FROM llibre WHERE `" + column +
                    "` IS NOT NULL AND `" + column + "` <> '' ORDER BY `" + column + "`")) {
                while (rs.next()) vals.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant valors de " + column + ": " + e.getMessage(), e);
        }
        return vals;
    }

}
