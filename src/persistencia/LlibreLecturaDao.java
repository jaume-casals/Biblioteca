package persistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads from the {@code lectura} table (one row per reading session). */
public class LlibreLecturaDao {

    private final Connection con;

    LlibreLecturaDao(Connection con) { this.con = con; }

    public synchronized List<LecturaRow> obtenirAllLectures() {
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
