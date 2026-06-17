package persistencia;

import domini.Llibre;
import domini.Llista;
import java.sql.*;
import java.util.ArrayList;

public class LlistaDao {

    private final Connection con;

    LlistaDao(Connection con) { this.con = con; }

    // Nota sobre el doble bloqueig: tots els consumidors passen per
    // ControladorPersistencia que ja està sincronitzat, de manera que
    // els mètodes del DAO no cal que estiguin sincronitzats ells
    // mateixos.

    public ArrayList<Llista> obtenirAll() {
        ArrayList<Llista> llistes = new ArrayList<>();
        try {
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, nom, ordre, color FROM llista ORDER BY ordre, nom")) {
                while (rs.next()) {
                    Llista l = new Llista(rs.getInt(1), rs.getString(2));
                    l.posarOrdre(rs.getInt(3));
                    l.setColor(rs.getString(4));
                    llistes.add(l);
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les llistes: " + e.getMessage(), e);
        }
        return llistes;
    }

    // La subconsulta SELECT COALESCE(MAX(ordre),0)+1 no s'envolta
    // intencionadament en una transacció SERIALIZABLE: aquesta
    // aplicació s'executa en una sola JVM i el mètode del DAO està
    // sincronitzat, de manera que les curses entre processos a MariaDB
    // no són un motiu de preocupació.
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

    public void actualitzarNom(int id, String newNom) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET nom = ? WHERE id = ?")) {
            ps.setString(1, newNom);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // getCount() es podria servir dels resultats de getAllCounts() en
    // una optimització futura per evitar una anada i tornada a la BBDD
    // per prestatgeria.
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

    // Optimització futura: els consumidors que necessitin tant
    // getAllCounts() com getCount() individual haurien de reutilitzar
    // el mapa de getAllCounts() en lloc d'emetre consultes per
    // prestatgeria.
    public java.util.Map<Integer, Integer> obtenirAllCounts() {
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

    public ArrayList<Llibre> obtenirLlibres(int llistaId) {
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
                    while (rs.next()) llibres.add(LlibreMapper.buildLlibre(rs));
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres de la llista: " + e.getMessage(), e);
        }
        return llibres;
    }

    public java.util.Set<Long> obtenirISBNsInLlista(int llistaId) {
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

    public void afegirLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT IGNORE INTO llibre_llista (isbn, llista_id, valoracio, llegit) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.setDouble(3, valoracio);
            ps.setBoolean(4, llegit);
            ps.execute();
        }
    }

    public void eliminarLlibre(long isbn, int llistaId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM llibre_llista WHERE isbn = ? AND llista_id = ?")) {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
            ps.execute();
        }
    }

    public void actualitzarLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE llibre_llista SET valoracio = ?, llegit = ? WHERE isbn = ? AND llista_id = ?")) {
            ps.setDouble(1, valoracio);
            ps.setBoolean(2, llegit);
            ps.setLong(3, isbn);
            ps.setInt(4, llistaId);
            ps.execute();
        }
    }

    public ArrayList<Llista> obtenirLlistesForLlibre(long isbn) {
        ArrayList<Llista> llistes = new ArrayList<>();
        for (Object[] row : queryLlistesForLlibreRaw(isbn)) {
            Llista llista = new Llista((Integer) row[0], (String) row[1]);
            llista.posarValoracioLlibre((Double) row[2]);
            llista.posarLlegitLlibre((Boolean) row[3]);
            llista.posarOrdre((Integer) row[4]);
            llista.setColor((String) row[5]);
            llistes.add(llista);
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
    public java.util.List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) {
        java.util.List<domini.LlibreLlistaContext> out = new java.util.ArrayList<>();
        for (Object[] row : queryLlistesForLlibreRaw(isbn)) {
            out.add(domini.LlibreLlistaContext.of(
                isbn,
                (Integer) row[0],
                (String) row[1],
                (Integer) row[4],
                (String) row[5],
                (Double) row[2],
                (Boolean) row[3]
            ));
        }
        return out;
    }

    /**
     * Single SQL query shared by {@link #getLlistesForLlibre} and
     * {@link #getLlistesForLlibreContext}. Columns in the result row:
     * 0=id, 1=nom, 2=valoracio, 3=llegit, 4=ordre, 5=color.
     */
    private java.util.List<Object[]> queryLlistesForLlibreRaw(long isbn) {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT l.id, l.nom, ll.valoracio, ll.llegit, l.ordre, l.color FROM llista l " +
                    "JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.ordre, l.nom")) {
                ps.setLong(1, isbn);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[] {
                            rs.getInt(1), rs.getString(2),
                            rs.getObject(3, Double.class), rs.getBoolean(4),
                            rs.getInt(5), rs.getString(6)
                        });
                    }
                }
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les llistes del llibre: " + e.getMessage(), e);
        }
        return rows;
    }

    public void actualitzarOrdre(int id, int ordre) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET ordre = ? WHERE id = ?")) {
            ps.setInt(1, ordre);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public void actualitzarColor(int id, String color) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE llista SET color = ? WHERE id = ?")) {
            if (color == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, color);
            ps.setInt(2, id);
            ps.execute();
        }
    }

    public java.util.List<persistencia.LlibreLlistaRow> obtenirAllLlibreLlista() {
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
