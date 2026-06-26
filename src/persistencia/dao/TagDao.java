package persistencia.dao;

import domini.Tag;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import persistencia.internal.MapejadorsFiles;
import persistencia.row.LlibreTagRow;

public class TagDao {

    private final Connection con;

    /**
     * Whitelist de columnes vàlides per a {@link #obtenirDistinctValues(String)}.
     * El nom de la columna s'interpola directament a la SQL (no hi ha
     * paràmetre per a un nom de columna en JDBC), per això la llista blanca
     * és <em>obligatòria</em> — sense ella, una entrada no validada seria
     * un vector d'SQL injection. Mantenir sincronitzada amb el switch en
     * memòria de {@code domini.facade.DelegatEstadistiques.IN_MEMORY_EXTRACTORS}
     * — la verificació corre al constructor per fallar aviat si la
     * llista queda desincronitzada.
     */
    public static final Set<String> AUTOCOMPLETE_COLUMNS = Set.of(
        "editorial", "serie", "idioma", "pais_origen", "format", "llengua_original");

    /**
     * Asserció d'inici: cada columna que el camí en memòria a
     * {@code DelegatEstadistiques.IN_MEMORY_EXTRACTORS} coneix ha de ser
     * també a la llista blanca SQL. Sense aquesta comprovació els dos
     * camins es desincronitzen silenciosament — una columna afegida als
     * extractors en memòria però no a la llista blanca SQL donaria un
     * conjunt de resultats més petit quan s'exercita el camí SQL (p.ex.
     * per a una columna que no té cap extractor en memòria). La
     * referència és per cadena per mantenir aquest DAO ignorant de la
     * capa de façana (que crearia un cicle).
     */
    static {
        try {
            Class<?> statsCls = Class.forName("domini.facade.DelegatEstadistiques");
            java.lang.reflect.Field f = statsCls.getDeclaredField("IN_MEMORY_EXTRACTORS");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, ?> mem = (java.util.Map<String, ?>) f.get(null);
            java.util.Set<String> memKeys = mem.keySet();
            java.util.Set<String> missing = new java.util.HashSet<>(memKeys);
            missing.removeAll(AUTOCOMPLETE_COLUMNS);
            if (!missing.isEmpty()) {
                throw new ExceptionInInitializerError(
                    "A TagDao.AUTOCOMPLETE_COLUMNS li falten els extractors en memòria: " + missing
                    + " (la llista blanca SQL ha de ser un superconjunt del mapa en memòria)");
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            // StatsDelegate pot ser absent als classpath de test; el
            // assert és best-effort. La deriva la continua capturant la
            // revisió de codi.
        }
    }

    private volatile java.util.List<LlibreTagRow> llibreTagCache;

    private static final MapejadorsFiles.MapejadorFiles<Tag> TAG_MAPPER =
        rs -> new Tag(rs.getInt(1), rs.getString(2));

    public TagDao(Connection con) { this.con = con; }

    public void invalidateLlibreTagCache() { llibreTagCache = null; }

    public ArrayList<Tag> obtenirAll() {
        try {
            return new ArrayList<>(MapejadorsFiles.queryAll(con,
                "SELECT id, nom FROM tag ORDER BY nom", TAG_MAPPER));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les etiquetes: " + e.getMessage(), e);
        }
    }

    public int create(String nom) throws SQLException {
        return MapejadorsFiles.insertReturningKey(con,
            "INSERT INTO tag (nom) VALUES (?)", ps -> ps.setString(1, nom));
    }

    public void delete(int id) throws SQLException {
        MapejadorsFiles.exec(con, "DELETE FROM tag WHERE id = ?", ps -> ps.setInt(1, id));
        invalidateLlibreTagCache();
    }

    public void rename(int id, String newNom) throws SQLException {
        MapejadorsFiles.exec(con, "UPDATE tag SET nom = ? WHERE id = ?", ps -> {
            ps.setString(1, newNom);
            ps.setInt(2, id);
        });
    }

    public ArrayList<Tag> obtenirForLlibre(long isbn) {
        try {
            return new ArrayList<>(MapejadorsFiles.queryWithParams(con,
                "SELECT t.id, t.nom FROM tag t JOIN llibre_tag lt ON t.id = lt.tag_id WHERE lt.isbn = ? ORDER BY t.nom",
                ps -> ps.setLong(1, isbn), TAG_MAPPER));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les etiquetes del llibre: " + e.getMessage(), e);
        }
    }

    public void afegirToLlibre(long isbn, int tagId) throws SQLException {
        MapejadorsFiles.exec(con, "INSERT IGNORE INTO llibre_tag (isbn, tag_id) VALUES (?, ?)", ps -> {
            ps.setLong(1, isbn);
            ps.setInt(2, tagId);
        });
        invalidateLlibreTagCache();
    }

    public void eliminarFromLlibre(long isbn, int tagId) throws SQLException {
        MapejadorsFiles.exec(con, "DELETE FROM llibre_tag WHERE isbn = ? AND tag_id = ?", ps -> {
            ps.setLong(1, isbn);
            ps.setInt(2, tagId);
        });
        invalidateLlibreTagCache();
    }

    public Set<Long> obtenirLlibresWithTag(int tagId) {
        try {
            return new HashSet<>(MapejadorsFiles.queryWithParams(con,
                "SELECT isbn FROM llibre_tag WHERE tag_id = ?",
                ps -> ps.setInt(1, tagId), rs -> rs.getLong(1)));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant els llibres de l'etiqueta: " + e.getMessage(), e);
        }
    }

    public java.util.List<LlibreTagRow> obtenirAllLlibreTag() {
        if (llibreTagCache != null) return llibreTagCache;
        try {
            llibreTagCache = java.util.List.copyOf(MapejadorsFiles.queryAll(con,
                "SELECT isbn, tag_id FROM llibre_tag ORDER BY tag_id, isbn",
                rs -> new LlibreTagRow(rs.getLong(1), rs.getInt(2))));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant les dades d'etiquetes: " + e.getMessage(), e);
        }
        return llibreTagCache;
    }

    /**
     * Camí SQL per a {@code obtenirDistinctValues}: consulta la taula {@code llibre}
     * amb {@code SELECT DISTINCT}. La columna ha de ser a
     * {@link #AUTOCOMPLETE_COLUMNS} (llista blanca) — qualsevol altra columna
     * retorna llista buida. La capa de domini ({@code ControladorDomini}) té un
     * camí alternatiu en memòria per a columnes que ja són a {@code Llibre};
     * veure'n el Javadoc per a la justificació del doble camí.
     */
    public java.util.List<String> obtenirDistinctValues(String column) {
        if (!AUTOCOMPLETE_COLUMNS.contains(column)) return new java.util.ArrayList<>();
        try {
            return MapejadorsFiles.queryAll(con,
                "SELECT DISTINCT `" + column + "` FROM llibre WHERE `" + column +
                "` IS NOT NULL AND `" + column + "` <> '' ORDER BY `" + column + "`",
                rs -> rs.getString(1));
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error carregant valors de " + column + ": " + e.getMessage(), e);
        }
    }

}
