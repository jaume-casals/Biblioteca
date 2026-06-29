package persistencia.dao;

import domini.Llibre;
import domini.Llista;
import java.sql.*;
import java.util.ArrayList;
import persistencia.internal.LlibreMapper;
import persistencia.internal.MapejadorsFiles;

import persistencia.internal.ControladorPersistencia;
import persistencia.row.LlibreLlistaRow;
public class LlistaDao {

    private final Connection con;

    public LlistaDao(Connection con) { this.con = con; }

    // Nota sobre el doble bloqueig: tots els consumidors passen per
    // ControladorPersistencia que ja està sincronitzat, de manera que
    // els mètodes del DAO no cal que estiguin sincronitzats ells
    // mateixos.

    public ArrayList<Llista> obtenirAll() {
        return new ArrayList<>(MapejadorsFiles.queryAllOrThrow(con,
            "SELECT id, nom, ordre, color FROM llista ORDER BY ordre, nom",
            "Error carregant les llistes",
            rs -> {
                Llista l = new Llista(rs.getInt(1), rs.getString(2));
                l.posarOrdre(rs.getInt(3));
                l.posarColor(rs.getString(4));
                return l;
            }));
    }

    // La subconsulta SELECT COALESCE(MAX(ordre),0)+1 no s'envolta
    // intencionadament en una transacció SERIALIZABLE: aquesta
    // aplicació s'executa en una sola JVM i el mètode del DAO està
    // sincronitzat, de manera que les curses entre processos a MariaDB
    // no són un motiu de preocupació.
    public int create(String nom) throws SQLException {
        return MapejadorsFiles.insertReturningKey(con,
            "INSERT INTO llista (nom, ordre) VALUES (?, (SELECT COALESCE(MAX(ordre),0)+1 FROM llista AS sub))",
            ps -> ps.setString(1, nom));
    }

    public void delete(int id) throws SQLException {
        MapejadorsFiles.exec(con, "DELETE FROM llista WHERE id = ?", ps -> ps.setInt(1, id));
    }

    public void actualitzarNom(int id, String newNom) {
        MapejadorsFiles.execOrThrow(con, "UPDATE llista SET nom = ? WHERE id = ?",
            "Error actualitzant el nom de la llista", ps -> {
                ps.setString(1, newNom);
                ps.setInt(2, id);
            });
    }

    // getCount() es podria servir dels resultats de getAllCounts() en
    // una optimització futura per evitar una anada i tornada a la BBDD
    // per prestatgeria.
    public int obtenirRecompte(int llistaId) {
        java.util.List<Integer> r = MapejadorsFiles.queryWithParamsOrThrow(con,
            "SELECT COUNT(*) FROM llibre_llista WHERE llista_id = ?",
            ps -> ps.setInt(1, llistaId), "Error comptant els llibres de la llista", rs -> rs.getInt(1));
        return r.isEmpty() ? 0 : r.get(0);
    }

    // Optimització futura: els consumidors que necessitin tant
    // getAllCounts() com getCount() individual haurien de reutilitzar
    // el mapa de getAllCounts() en lloc d'emetre consultes per
    // prestatgeria.
    public java.util.Map<Integer, Integer> obtenirAllCounts() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        for (int[] row : MapejadorsFiles.queryAllOrThrow(con,
                "SELECT llista_id, COUNT(*) FROM llibre_llista GROUP BY llista_id",
                "Error comptant els llibres de les llistes",
                rs -> new int[]{rs.getInt(1), rs.getInt(2)}))
            counts.put(row[0], row[1]);
        return counts;
    }

    public ArrayList<Llibre> obtenirLlibres(int llistaId) {
        return new ArrayList<>(MapejadorsFiles.queryWithParamsOrThrow(con,
            "SELECT " + LlibreDaoCore.LLIBRE_COLUMNS_L_SHELF +
            " FROM llibre l JOIN llibre_llista ll ON l.ISBN = ll.isbn WHERE ll.llista_id = ? ORDER BY l.ISBN",
            ps -> ps.setInt(1, llistaId), "Error carregant els llibres de la llista", LlibreMapper::buildLlibre));
    }

    public java.util.Set<Long> obtenirISBNsInLlista(int llistaId) {
        return new java.util.HashSet<>(MapejadorsFiles.queryWithParamsOrThrow(con,
            "SELECT isbn FROM llibre_llista WHERE llista_id = ?",
            ps -> ps.setInt(1, llistaId), "Error carregant ISBNs de llista", rs -> rs.getLong(1)));
    }

    public void afegirLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        MapejadorsFiles.exec(con,
            "INSERT IGNORE INTO llibre_llista (isbn, llista_id, valoracio, llegit) VALUES (?, ?, ?, ?)", ps -> {
                ps.setLong(1, isbn);
                ps.setInt(2, llistaId);
                ps.setDouble(3, valoracio);
                ps.setBoolean(4, llegit);
            });
    }

    public void eliminarLlibre(long isbn, int llistaId) throws SQLException {
        MapejadorsFiles.exec(con, "DELETE FROM llibre_llista WHERE isbn = ? AND llista_id = ?", ps -> {
            ps.setLong(1, isbn);
            ps.setInt(2, llistaId);
        });
    }

    public void actualitzarLlibre(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
        MapejadorsFiles.exec(con,
            "UPDATE llibre_llista SET valoracio = ?, llegit = ? WHERE isbn = ? AND llista_id = ?", ps -> {
                ps.setDouble(1, valoracio);
                ps.setBoolean(2, llegit);
                ps.setLong(3, isbn);
                ps.setInt(4, llistaId);
            });
    }

    public ArrayList<Llista> obtenirLlistesForLlibre(long isbn) {
        ArrayList<Llista> llistes = new ArrayList<>();
        for (Object[] row : queryLlistesForLlibreRaw(isbn)) {
            Llista llista = new Llista((Integer) row[0], (String) row[1]);
            llista.posarValoracioLlibre((Double) row[2]);
            llista.posarLlegitLlibre((Boolean) row[3]);
            llista.posarOrdre((Integer) row[4]);
            llista.posarColor((String) row[5]);
            llistes.add(llista);
        }
        return llistes;
    }

    /**
     * Com a {@link #obtenirLlistesForLlibre}, però retorna cada fila com a
     * {@link domini.LlibreLlistaContext} — la valoració/llegit per llibre
     * viatja al context, no pas al {@link Llista}. Preferit sobre
     * {@code obtenirLlistesForLlibre} per a noves crides; l'API anterior
     * existeix per compat.
     */
    public java.util.List<domini.LlibreLlistaContext> obtenirLlistesForLlibreContext(long isbn) {
        java.util.List<domini.LlibreLlistaContext> out = new java.util.ArrayList<>();
        for (Object[] row : queryLlistesForLlibreRaw(isbn)) {
            out.add(new domini.LlibreLlistaContext(
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
     * Consulta SQL única compartida per {@link #obtenirLlistesForLlibre} i
     * {@link #obtenirLlistesForLlibreContext}. Columnes a la fila de resultat:
     * 0=id, 1=nom, 2=valoracio, 3=llegit, 4=ordre, 5=color.
     */
    private java.util.List<Object[]> queryLlistesForLlibreRaw(long isbn) {
        return MapejadorsFiles.queryWithParamsOrThrow(con,
            "SELECT l.id, l.nom, ll.valoracio, ll.llegit, l.ordre, l.color FROM llista l " +
            "JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.ordre, l.nom",
            ps -> ps.setLong(1, isbn),
            "Error carregant les llistes del llibre",
            rs -> new Object[] {
                rs.getInt(1), rs.getString(2),
                rs.getObject(3, Double.class), rs.getBoolean(4),
                rs.getInt(5), rs.getString(6)
            });
    }

    public void actualitzarOrdre(int id, int ordre) {
        MapejadorsFiles.execOrThrow(con, "UPDATE llista SET ordre = ? WHERE id = ?",
            "Error actualitzant l'ordre de la llista", ps -> {
                ps.setInt(1, ordre);
                ps.setInt(2, id);
            });
    }

    public void actualitzarColor(int id, String color) {
        MapejadorsFiles.execOrThrow(con, "UPDATE llista SET color = ? WHERE id = ?",
            "Error actualitzant el color de la llista", ps -> {
                if (color == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, color);
                ps.setInt(2, id);
            });
    }

    public java.util.List<persistencia.row.LlibreLlistaRow> obtenirAllLlibreLlista() {
        return MapejadorsFiles.queryAllOrThrow(con,
            "SELECT isbn, llista_id, valoracio, llegit FROM llibre_llista ORDER BY llista_id, isbn",
            "Error carregant les dades de llista",
            rs -> new persistencia.row.LlibreLlistaRow(rs.getLong(1), rs.getInt(2), rs.getDouble(3), rs.getBoolean(4)));
    }
}
