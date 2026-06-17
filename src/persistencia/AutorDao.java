package persistencia;

import java.sql.*;
import java.util.List;

public class AutorDao {

    private final Connection con;

    AutorDao(Connection con) { this.con = con; }

    private <T> List<T> queryAll(String sql, MapejadorsFiles.MapejadorFiles<T> mapper) {
        try { return MapejadorsFiles.queryAll(con, sql, mapper); }
        catch (SQLException e) { throw new domini.BibliotecaException("Error executant consulta: " + e.getMessage(), e); }
    }

    // Nota sobre el doble bloqueig: els consumidors passen per
    // ControladorPersistencia que ja està sincronitzat, de manera que
    // els mètodes del DAO no cal que estiguin sincronitzats ells
    // mateixos.

    public List<AutorRow> obtenirAll() {
        return queryAll("SELECT id, nom FROM autor ORDER BY nom",
                rs -> new AutorRow(rs.getInt(1), rs.getString(2)));
    }

    public List<String> obtenirDistinctAutorNames() {
        return queryAll("SELECT nom FROM autor ORDER BY nom", rs -> rs.getString(1));
    }

    public List<LlibreAutorRow> obtenirAllLlibreAutor() {
        return queryAll("SELECT isbn, autor_id FROM llibre_autor ORDER BY isbn, autor_id",
                rs -> new LlibreAutorRow(rs.getLong(1), rs.getInt(2)));
    }

    public int crearAutor(String nom) {
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

    public void actualitzarAutor(int id, String nom) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE autor SET nom = ? WHERE id = ?")) {
            ps.setString(1, nom);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error actualitzant autor: " + e.getMessage(), e);
        }
    }

    public void eliminarAutor(int id) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM autor WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error eliminant autor: " + e.getMessage(), e);
        }
    }
}
