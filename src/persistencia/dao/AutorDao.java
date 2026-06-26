package persistencia.dao;

import java.sql.*;
import java.util.List;

import persistencia.internal.MapejadorsFiles;
import persistencia.row.AutorRow;
import persistencia.row.LlibreAutorRow;

import persistencia.internal.ControladorPersistencia;
public class AutorDao {

    private final Connection con;

    public AutorDao(Connection con) { this.con = con; }

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
}
