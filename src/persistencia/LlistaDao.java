package persistencia;

import domini.Llibre;
import domini.Llista;
import java.sql.*;
import java.util.ArrayList;

public class LlistaDao {

    private final Connection con;

    LlistaDao(Connection con) { this.con = con; }

    public synchronized ArrayList<Llista> getAll() {
        ArrayList<Llista> llistes = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, nom, ordre, color FROM llista ORDER BY ordre, nom")) {
                while (rs.next()) {
                    Llista l = new Llista(rs.getInt(1), rs.getString(2));
                    l.setOrdre(rs.getInt(3));
                    l.setColor(rs.getString(4));
                    llistes.add(l);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error carregant les llistes: " + e.getMessage());
        }
        return llistes;
    }

    public synchronized int create(String nom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO llista (nom) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("createLlista: no generated key returned");
                return rs.getInt(1);
            }
        }
    }

    public synchronized void delete(int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM llista WHERE id = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }
    }

    public synchronized int getCount(int llistaId) {
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM llibre_llista WHERE llista_id = ?")) {
                ps.setInt(1, llistaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error comptant els llibres de la llista: " + e.getMessage());
        }
        return 0;
    }

    public synchronized ArrayList<Llibre> getLlibres(int llistaId) {
        ArrayList<Llibre> llibres = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.ISBN, l.nom, l.autor, l.`any`, l.descripcio, ll.valoracio, l.preu, ll.llegit, l.imatge, " +
                    "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
                    "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original " +
                    "FROM llibre l JOIN llibre_llista ll ON l.ISBN = ll.isbn WHERE ll.llista_id = ?")) {
                ps.setInt(1, llistaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) llibres.add(LlibreDao.buildLlibre(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error carregant els llibres de la llista: " + e.getMessage());
        }
        return llibres;
    }

    public synchronized void addLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO llibre_llista (isbn, llista_id, valoracio, llegit) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.setDouble(3, valoracio);
            ps.setBoolean(4, llegit);
            ps.execute();
        }
    }

    public synchronized void removeLlibre(long isbn, int llistaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM llibre_llista WHERE isbn = ? AND llista_id = ?")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.execute();
        }
    }

    public synchronized void updateLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE llibre_llista SET valoracio = ?, llegit = ? WHERE isbn = ? AND llista_id = ?")) {
            ps.setDouble(1, valoracio);
            ps.setBoolean(2, llegit);
            ps.setLong(3, isbn);
            ps.setInt(4, llistaId);
            ps.execute();
        }
    }

    public synchronized ArrayList<Llista> getLlistesForLlibre(long isbn) {
        ArrayList<Llista> llistes = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.id, l.nom, ll.valoracio, ll.llegit FROM llista l " +
                    "JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.nom")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Llista llista = new Llista(rs.getInt(1), rs.getString(2));
                        llista.setValoracioLlibre(rs.getDouble(3));
                        llista.setLlegitLlibre(rs.getBoolean(4));
                        llistes.add(llista);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error carregant les llistes del llibre: " + e.getMessage());
        }
        return llistes;
    }

    public synchronized void updateOrdre(int id, int ordre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET ordre = ? WHERE id = ?")) {
            ps.setInt(1, ordre);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public synchronized void updateColor(int id, String color) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET color = ? WHERE id = ?")) {
            if (color == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, color);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public synchronized java.util.List<Object[]> getAllLlibreLlista() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, llista_id, valoracio, llegit FROM llibre_llista ORDER BY llista_id, isbn")) {
                while (rs.next())
                    rows.add(new Object[]{ rs.getLong(1), rs.getInt(2), rs.getDouble(3), rs.getBoolean(4) });
            }
        } catch (SQLException e) {
            System.err.println("Error carregant les dades de llista: " + e.getMessage());
        }
        return rows;
    }
}
