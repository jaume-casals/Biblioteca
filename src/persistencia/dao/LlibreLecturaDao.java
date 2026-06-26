package persistencia.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import persistencia.internal.MapejadorsFiles;
import persistencia.row.LecturaRow;

/** Lectures de la taula {@code lectura} (una fila per sessió de lectura). */
public class LlibreLecturaDao {

    private final Connection con;

    public LlibreLecturaDao(Connection con) { this.con = con; }

    public List<LecturaRow> obtenirAllLectures() {
        try {
            return MapejadorsFiles.queryAll(con,
                "SELECT isbn, data_inici, data_fi, pagines_llegides FROM lectura ORDER BY id",
                rs -> new LecturaRow(rs.getLong(1), LecturaRow.analitzarDateOrNull(rs.getString(2)),
                    LecturaRow.analitzarDateOrNull(rs.getString(3)), rs.getInt(4)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les lectures: " + e.getMessage(), e);
        }
    }
}
