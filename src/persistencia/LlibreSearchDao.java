package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import domini.Llibre;
import domini.LlibreFilter;
import domini.EspecificacioOrdenacio;

/** Cerca amb filtre unificat per a la taula principal de la biblioteca. */
public class LlibreSearchDao {

    private final Connection con;

    LlibreSearchDao(Connection con) { this.con = con; }

    public ArrayList<Llibre> search(LlibreFilter f) {
        return search(f, 0, 0);
    }

    public ArrayList<Llibre> search(LlibreFilter f, int offset, int pageSize) {
        if (pageSize < 0) throw new IllegalArgumentException("pageSize must be >= 0 (0 means no limit); got " + pageSize);
        ArrayList<Llibre> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT " + LlibreDaoCore.LLIBRE_COLUMNS_L + " FROM llibre l");
        List<Object> params = new ArrayList<>();
        if (f.obtenirLlistaId() != null) {
            sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
            params.add(f.obtenirLlistaId());
        }
        if (f.obtenirTagId() != null) {
            sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
            params.add(f.obtenirTagId());
        }
        sql.append(" WHERE 1=1");
        // addCondition(sql, params, clause, value) no afegeix res quan
        // el valor és null i el nombre correcte de placeholders en cas
        // contrari. Les antigues branques inline eren 18 línies repetint
        // "if (f.getX() != null) { sql.append(" AND ..."); params.add(...); }"
        // i els errors de recompte de placeholders eren fàcils
        // d'introduir; l'helper és el finding MEDIUM de tot.txt sobre
        // aquesta classe.
        afegirCondition(sql, params, " AND l.ISBN = ?", f.obtenirIsbn());
        if (f.obtenirNom() != null && !f.obtenirNom().isBlank()) {
            String p = "%" + f.obtenirNom() + "%";
            sql.append(" AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)");
            params.add(p); params.add(p); params.add(p); params.add(p);
        }
        afegirCondition(sql, params, " AND EXISTS (SELECT 1 FROM llibre_autor la2 JOIN autor a2 ON la2.autor_id = a2.id WHERE la2.isbn = l.ISBN AND a2.nom LIKE ?)",
            f.obtenirAutor() != null && !f.obtenirAutor().isBlank() ? "%" + f.obtenirAutor() + "%" : null);
        afegirCondition(sql, params, " AND l.`any` >= ?", f.obtenirAnyMin());
        afegirCondition(sql, params, " AND l.`any` <= ?", f.obtenirAnyMax());
        afegirCondition(sql, params, " AND l.valoracio >= ?", f.obtenirValoracioMin());
        afegirCondition(sql, params, " AND l.valoracio <= ?", f.obtenirValoracioMax());
        afegirCondition(sql, params, " AND l.preu >= ?", f.obtenirPreuMin());
        afegirCondition(sql, params, " AND l.preu <= ?", f.obtenirPreuMax());
        afegirCondition(sql, params, " AND l.llegit = ?", f.obtenirLlegit());
        afegirCondition(sql, params, " AND l.editorial LIKE ?",
            f.obtenirEditorial() != null && !f.obtenirEditorial().isBlank() ? "%" + f.obtenirEditorial() + "%" : null);
        afegirCondition(sql, params, " AND l.serie LIKE ?",
            f.obtenirSerie() != null && !f.obtenirSerie().isBlank() ? "%" + f.obtenirSerie() + "%" : null);
        afegirCondition(sql, params, " AND l.format = ?",
            f.obtenirFormat() != null && !f.obtenirFormat().isBlank() ? f.obtenirFormat() : null);
        afegirCondition(sql, params, " AND l.idioma LIKE ?",
            f.obtenirIdioma() != null && !f.obtenirIdioma().isBlank() ? "%" + f.obtenirIdioma() + "%" : null);
        EspecificacioOrdenacio sort = f.obtenirSort();
        if (sort == null) sort = EspecificacioOrdenacio.defaultAsc();
        sql.append(" ORDER BY ").append(sort.toSql());
        if (pageSize > 0) sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                vincularParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(LlibreMapper.buildLlibre(rs));
            }
        } catch (SQLException e) {
            throw new domini.BibliotecaException("Error en searchLlibres: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Afegeix {@code clause} a {@code sql} i {@code value} a {@code params}
     * si {@code value} no és null. No fa res quan {@code value} és null — el
     * consumidor pot passar un valor precalculat (p.ex. {@code "%foo%"}) i deixar
     * que l'ajudant decideixi si vincular-lo. Redueix el cos de search() des de
     * ~20 branques de {@code if (f.getX() != null) { sql.append(...); params.add(...); }}
     * a crides d'una sola línia per condició.
     */
    private static void afegirCondition(StringBuilder sql, List<Object> params, String clause, Object value) {
        if (value == null) return;
        sql.append(clause);
        params.add(value);
    }

    private static void vincularParam(PreparedStatement ps, int index, Object p) throws SQLException {
        switch (p) {
            case String  s -> ps.setString(index, s);
            case Long    l -> ps.setLong(index, l);
            case Integer i -> ps.setInt(index, i);
            case Double  d -> ps.setDouble(index, d);
            case Boolean b -> ps.setBoolean(index, b);
            case null      -> {
                // Avui el constructor SQL no insereix mai un null literal
                // (tota condició està protegida amb != null o amb una
                // comprovació de blanc), de manera que aquesta branca és
                // morta. Registra a FINE perquè un futur lloc de crida
                // que afegeixi un null literal es faci visible als logs
                // sense llançar excepció.
                java.util.logging.Logger.getLogger(LlibreSearchDao.class.getName())
                    .fine("bindParam: null literal a l'índex " + index + " — hauria d'estar protegit aigües amunt");
                ps.setObject(index, null, Types.NULL);
            }
            default        -> throw new IllegalArgumentException("Unsupported filter parameter type: " + p.getClass().getName());
        }
    }
}
