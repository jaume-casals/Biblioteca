package persistencia;

import domini.Llibre;
import domini.Llista;
import java.sql.*;
import java.util.ArrayList;

public class LlistaDao {

    private final Connection con;

    LlistaDao(Connection con) { this.con = con; }

    // Double-locking note: all callers go through ControladorPersistencia which
    // is already synchronized, so DAO methods need not be synchronized themselves.

    public ArrayList<Llista> getAll() {
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
            throw new domini.BibliotecaException("Error carregant les llistes: " + e.getMessage(), e);
        }
        return llistes;
    }

    // The subquery SELECT COALESCE(MAX(ordre),0)+1 is intentionally not wrapped in a
    // SERIALIZABLE transaction: this application runs on a single JVM and the DAO
    // method is synchronized, so cross-process races in MariaDB are not a concern.
    public int create(String nom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO llista (nom, ordre) VALUES (?, (SELECT COALESCE(MAX(ordre),0)+1 FROM llista AS sub))", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("createLlista: no generated key returned");
                return rs.getInt(1);
            }
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM llista WHERE id = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }
    }

    public void updateNom(int id, String newNom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET nom = ? WHERE id = ?")) {
            ps.setString(1, newNom);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // getCount() could be served from getAllCounts() results in a future optimization
    // to avoid a separate DB round-trip per shelf.
    public int getCount(int llistaId) {
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM llibre_llista WHERE llista_id = ?")) {
                ps.setInt(1, llistaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error comptant els llibres de la llista: " + e.getMessage(), e);
        }
        return 0;
    }

    // Future optimization: callers that need both getAllCounts() and individual getCount()
    // should reuse the getAllCounts() map instead of issuing per-shelf queries.
    public java.util.Map<Integer, Integer> getAllCounts() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT llista_id, COUNT(*) FROM llibre_llista GROUP BY llista_id")) {
                while (rs.next()) counts.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error comptant els llibres de les llistes: " + e.getMessage(), e);
        }
        return counts;
    }

    public ArrayList<Llibre> getLlibres(int llistaId) {
        ArrayList<Llibre> llibres = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.ISBN, l.nom, " +
                    "(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor, " +
                    "l.`any`, l.descripcio, ll.valoracio, l.preu, ll.llegit, l.imatge, " +
                    "(l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, " +
                    "l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen, l.estat, l.exemplars, l.llengua_original, " +
                    "l.nom_ca, l.nom_es, l.nom_en " +
                    "FROM llibre l JOIN llibre_llista ll ON l.ISBN = ll.isbn WHERE ll.llista_id = ? ORDER BY l.ISBN")) {
                ps.setInt(1, llistaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) llibres.add(LlibreDao.buildLlibre(rs));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres de la llista: " + e.getMessage(), e);
        }
        return llibres;
    }

    public java.util.Set<Long> getISBNsInLlista(int llistaId) {
        java.util.Set<Long> set = new java.util.HashSet<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT isbn FROM llibre_llista WHERE llista_id = ?")) {
                ps.setInt(1, llistaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) set.add(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant ISBNs de llista: " + e.getMessage(), e);
        }
        return set;
    }

    public void addLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO llibre_llista (isbn, llista_id, valoracio, llegit) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.setDouble(3, valoracio);
            ps.setBoolean(4, llegit);
            ps.execute();
        }
    }

    public void removeLlibre(long isbn, int llistaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM llibre_llista WHERE isbn = ? AND llista_id = ?")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.execute();
        }
    }

    public void updateLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE llibre_llista SET valoracio = ?, llegit = ? WHERE isbn = ? AND llista_id = ?")) {
            ps.setDouble(1, valoracio);
            ps.setBoolean(2, llegit);
            ps.setLong(3, isbn);
            ps.setInt(4, llistaId);
            ps.execute();
        }
    }

    public ArrayList<Llista> getLlistesForLlibre(long isbn) {
        ArrayList<Llista> llistes = new ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.id, l.nom, ll.valoracio, ll.llegit, l.ordre, l.color FROM llista l " +
                    "JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.ordre, l.nom")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Llista llista = new Llista(rs.getInt(1), rs.getString(2));
                        llista.setValoracioLlibre(rs.getDouble(3));
                        llista.setLlegitLlibre(rs.getBoolean(4));
                        llista.setOrdre(rs.getInt(5));
                        llista.setColor(rs.getString(6));
                        llistes.add(llista);
                    }
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les llistes del llibre: " + e.getMessage(), e);
        }
        return llistes;
    }

    /**
     * Com a {@link #getLlistesForLlibre}, però retorna cada fila com a
     * {@link domini.LlibreLlistaContext} — el valoració/llegit per llibre
     * viatja al context, no pas al {@link Llista}. Preferit sobre
     * {@code getLlistesForLlibre} per a noves crides; l'API anterior
     * existeix per compat.
     */
    public java.util.List<domini.LlibreLlistaContext> getLlistesForLlibreContext(long isbn) {
        java.util.List<domini.LlibreLlistaContext> out = new java.util.ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.id, l.nom, ll.valoracio, ll.llegit, l.ordre, l.color FROM llista l " +
                    "JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.ordre, l.nom")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(domini.LlibreLlistaContext.of(
                            (int) isbn,
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(5),
                            rs.getString(6),
                            rs.getDouble(3),
                            rs.getBoolean(4)
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els contextos de llista del llibre: " + e.getMessage(), e);
        }
        return out;
    }

    public void updateOrdre(int id, int ordre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET ordre = ? WHERE id = ?")) {
            ps.setInt(1, ordre);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public void updateColor(int id, String color) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET color = ? WHERE id = ?")) {
            if (color == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, color);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public java.util.List<persistencia.LlibreLlistaRow> getAllLlibreLlista() {
        java.util.List<persistencia.LlibreLlistaRow> rows = new java.util.ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery(
                    "SELECT isbn, llista_id, valoracio, llegit FROM llibre_llista ORDER BY llista_id, isbn")) {
                while (rs.next())
                    rows.add(new persistencia.LlibreLlistaRow(rs.getLong(1), rs.getInt(2), rs.getDouble(3), rs.getBoolean(4)));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les dades de llista: " + e.getMessage(), e);
        }
        return rows;
    }
}
