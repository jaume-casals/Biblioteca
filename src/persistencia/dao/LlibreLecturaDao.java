package persistencia.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import persistencia.row.LecturaRow;

/** Lectures de la taula {@code lectura} (una fila per sessió de lectura). */
public class LlibreLecturaDao {

    private final Connection con;

    public LlibreLecturaDao(Connection con) { this.con = con; }

    public List<LecturaRow> obtenirAllLectures() {
        List<LecturaRow> rows = new ArrayList<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(
                "SELECT isbn, data_inici, data_fi, pagines_llegides FROM lectura ORDER BY id")) {
            while (rs.next())
                rows.add(new LecturaRow(rs.getLong(1), LecturaRow.analitzarDateOrNull(rs.getString(2)), LecturaRow.analitzarDateOrNull(rs.getString(3)), rs.getInt(4)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les lectures: " + e.getMessage(), e);
        }
        return rows;
    }
}
