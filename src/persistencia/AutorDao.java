package persistencia;

import java.sql.*;
import java.util.List;

public class AutorDao {

    private final Connection con;

    AutorDao(Connection con) { this.con = con; }

    private <T> List<T> queryAll(String sql, RowMappers.RowMapper<T> mapper) {
        try { return RowMappers.queryAll(con, sql, mapper); }
        catch (SQLException e) { throw new domini.BibliotecaException("Error executant consulta: " + e.getMessage(), e); }
    }

    // Double-locking note: callers go through ControladorPersistencia which is
    // already synchronized, so DAO methods need not be synchronized themselves.

    public List<AutorRow> getAll() {
        return queryAll("SELECT id, nom FROM autor ORDER BY nom",
                rs -> new AutorRow(rs.getInt(1), rs.getString(2)));
    }

    public List<String> getDistinctAutorNames() {
        return queryAll("SELECT nom FROM autor ORDER BY nom", rs -> rs.getString(1));
    }

    public List<LlibreAutorRow> getAllLlibreAutor() {
        return queryAll("SELECT isbn, autor_id FROM llibre_autor ORDER BY isbn, autor_id",
                rs -> new LlibreAutorRow(rs.getLong(1), rs.getInt(2)));
    }

    public int createAutor(String nom) {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO autor (nom) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error creant autor: " + e.getMessage(), e);
        }
        throw new domini.BibliotecaException("No s'ha obtingut id d'autor nou");
    }

    public void updateAutor(int id, String nom) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE autor SET nom = ? WHERE id = ?")) {
            ps.setString(1, nom);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error actualitzant autor: " + e.getMessage(), e);
        }
    }

    public void deleteAutor(int id) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM autor WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error eliminant autor: " + e.getMessage(), e);
        }
    }
}
