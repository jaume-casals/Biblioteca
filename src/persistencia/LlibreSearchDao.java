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
import domini.SortSpec;

/** Unified-filter search for the main library table. */
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
        if (f.getLlistaId() != null) {
            sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
            params.add(f.getLlistaId());
        }
        if (f.getTagId() != null) {
            sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
            params.add(f.getTagId());
        }
        sql.append(" WHERE 1=1");
        // addCondition(sql, params, clause, value) appends nothing when
        // value is null and the right number of placeholders otherwise.
        // The old inline branches were 18 lines of repeated
        // "if (f.getX() != null) { sql.append(" AND ..."); params.add(...); }"
        // and the place-counting bugs were easy to introduce; the helper
        // is the tot.txt MEDIUM finding on this class.
        addCondition(sql, params, " AND l.ISBN = ?", f.getIsbn());
        if (f.getNom() != null && !f.getNom().isBlank()) {
            String p = "%" + f.getNom() + "%";
            sql.append(" AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)");
            params.add(p); params.add(p); params.add(p); params.add(p);
        }
        addCondition(sql, params, " AND EXISTS (SELECT 1 FROM llibre_autor la2 JOIN autor a2 ON la2.autor_id = a2.id WHERE la2.isbn = l.ISBN AND a2.nom LIKE ?)",
            f.getAutor() != null && !f.getAutor().isBlank() ? "%" + f.getAutor() + "%" : null);
        addCondition(sql, params, " AND l.`any` >= ?", f.getAnyMin());
        addCondition(sql, params, " AND l.`any` <= ?", f.getAnyMax());
        addCondition(sql, params, " AND l.valoracio >= ?", f.getValoracioMin());
        addCondition(sql, params, " AND l.valoracio <= ?", f.getValoracioMax());
        addCondition(sql, params, " AND l.preu >= ?", f.getPreuMin());
        addCondition(sql, params, " AND l.preu <= ?", f.getPreuMax());
        addCondition(sql, params, " AND l.llegit = ?", f.getLlegit());
        addCondition(sql, params, " AND l.editorial LIKE ?",
            f.getEditorial() != null && !f.getEditorial().isBlank() ? "%" + f.getEditorial() + "%" : null);
        addCondition(sql, params, " AND l.serie LIKE ?",
            f.getSerie() != null && !f.getSerie().isBlank() ? "%" + f.getSerie() + "%" : null);
        addCondition(sql, params, " AND l.format = ?",
            f.getFormat() != null && !f.getFormat().isBlank() ? f.getFormat() : null);
        addCondition(sql, params, " AND l.idioma LIKE ?",
            f.getIdioma() != null && !f.getIdioma().isBlank() ? "%" + f.getIdioma() + "%" : null);
        SortSpec sort = f.getSort();
        if (sort == null) sort = SortSpec.defaultAsc();
        sql.append(" ORDER BY ").append(sort.toSql());
        if (pageSize > 0) sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
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
     * Append {@code clause} to {@code sql} and {@code value} to {@code params}
     * if {@code value} is non-null. No-op when {@code value} is null — the
     * caller can pass a pre-computed value (e.g. {@code "%foo%"}) and let the
     * helper decide whether to bind it. Reduces the search() body from
     * ~20 branches of {@code if (f.getX() != null) { sql.append(...); params.add(...); }}
     * to single-line per-condition calls.
     */
    private static void addCondition(StringBuilder sql, List<Object> params, String clause, Object value) {
        if (value == null) return;
        sql.append(clause);
        params.add(value);
    }

    private static void bindParam(PreparedStatement ps, int index, Object p) throws SQLException {
        switch (p) {
            case String  s -> ps.setString(index, s);
            case Long    l -> ps.setLong(index, l);
            case Integer i -> ps.setInt(index, i);
            case Double  d -> ps.setDouble(index, d);
            case Boolean b -> ps.setBoolean(index, b);
            case null      -> ps.setObject(index, null, Types.NULL);
            default        -> throw new IllegalArgumentException("Unsupported filter parameter type: " + p.getClass().getName());
        }
    }
}
