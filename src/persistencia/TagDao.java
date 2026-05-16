package persistencia;

import domini.Tag;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TagDao {

    private final Connection con;

    private static final Set<String> AUTOCOMPLETE_COLUMNS = new HashSet<>(
        Arrays.asList("editorial", "serie", "idioma", "pais_origen", "format", "llengua_original"));

    TagDao(Connection con) { this.con = con; }

    public synchronized ArrayList<Tag> getAll() {
        ArrayList<Tag> tags = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, nom FROM tag ORDER BY nom")) {
                while (rs.next()) tags.add(new Tag(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            System.err.println("Error carregant les etiquetes: " + e.getMessage());
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
            System.err.println("Error carregant les etiquetes del llibre: " + e.getMessage());
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
    }

    public synchronized void removeFromLlibre(long isbn, int tagId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM llibre_tag WHERE isbn = ? AND tag_id = ?")) {
            ps.setLong(1, isbn);
            ps.setInt(2, tagId);
            ps.execute();
        }
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
            System.err.println("Error carregant els llibres de l'etiqueta: " + e.getMessage());
        }
        return isbns;
    }

    public synchronized java.util.List<domini.LlibreTagRow> getAllLlibreTag() {
        java.util.List<domini.LlibreTagRow> rows = new java.util.ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, tag_id FROM llibre_tag ORDER BY tag_id, isbn")) {
                while (rs.next())
                    rows.add(new domini.LlibreTagRow(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            System.err.println("Error carregant les dades d'etiquetes: " + e.getMessage());
        }
        return rows;
    }

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
            System.err.println("Error carregant valors de " + column + ": " + e.getMessage());
        }
        return vals;
    }

}
