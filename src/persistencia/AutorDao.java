package persistencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutorDao {

    private final Connection con;

    AutorDao(Connection con) { this.con = con; }

    public synchronized List<AutorRow> getAll() {
        List<AutorRow> rows = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, nom FROM autor ORDER BY nom")) {
                while (rs.next()) rows.add(new AutorRow(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            System.err.println("Error carregant els autors: " + e.getMessage());
        }
        return rows;
    }

    public synchronized List<String> getDistinctAutorNames() {
        List<String> vals = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT nom FROM autor ORDER BY nom")) {
                while (rs.next()) vals.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Error carregant autors: " + e.getMessage());
        }
        return vals;
    }

    public synchronized List<LlibreAutorRow> getAllLlibreAutor() {
        List<LlibreAutorRow> rows = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, autor_id FROM llibre_autor ORDER BY isbn, autor_id")) {
                while (rs.next()) rows.add(new LlibreAutorRow(rs.getLong(1), rs.getInt(2)));
            }
        } catch (SQLException e) {
            System.err.println("Error carregant els autors dels llibres: " + e.getMessage());
        }
        return rows;
    }
}
