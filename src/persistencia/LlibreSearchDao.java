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
        if (f.getLlistaId() != null) sql.append(" JOIN llibre_llista ll ON l.ISBN = ll.isbn AND ll.llista_id = ?");
        if (f.getTagId()    != null) sql.append(" JOIN llibre_tag lt ON l.ISBN = lt.isbn AND lt.tag_id = ?");
        sql.append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (f.getLlistaId() != null) params.add(f.getLlistaId());
        if (f.getTagId()    != null) params.add(f.getTagId());
        if (f.getNom()          != null && !f.getNom().isBlank()) {
            sql.append(" AND (l.nom LIKE ? OR l.nom_ca LIKE ? OR l.nom_es LIKE ? OR l.nom_en LIKE ?)");
            String p = "%" + f.getNom() + "%";
            params.add(p); params.add(p); params.add(p); params.add(p);
        }
        if (f.getAutor()        != null && !f.getAutor().isBlank()) { sql.append(" AND EXISTS (SELECT 1 FROM llibre_autor la2 JOIN autor a2 ON la2.autor_id = a2.id WHERE la2.isbn = l.ISBN AND a2.nom LIKE ?)"); params.add("%" + f.getAutor() + "%"); }
        if (f.getIsbn()         != null) { sql.append(" AND l.ISBN = ?");          params.add(f.getIsbn()); }
        if (f.getAnyMin()       != null) { sql.append(" AND l.`any` >= ?");        params.add(f.getAnyMin()); }
        if (f.getAnyMax()       != null) { sql.append(" AND l.`any` <= ?");        params.add(f.getAnyMax()); }
        if (f.getValoracioMin() != null) { sql.append(" AND l.valoracio >= ?");    params.add(f.getValoracioMin()); }
        if (f.getValoracioMax() != null) { sql.append(" AND l.valoracio <= ?");    params.add(f.getValoracioMax()); }
        if (f.getPreuMin()      != null) { sql.append(" AND l.preu >= ?");         params.add(f.getPreuMin()); }
        if (f.getPreuMax()      != null) { sql.append(" AND l.preu <= ?");         params.add(f.getPreuMax()); }
        if (f.getLlegit()       != null) { sql.append(" AND l.llegit = ?");        params.add(f.getLlegit()); }
        if (f.getEditorial()    != null && !f.getEditorial().isBlank()) { sql.append(" AND l.editorial LIKE ?");  params.add("%" + f.getEditorial() + "%"); }
        if (f.getSerie()        != null && !f.getSerie().isBlank()) { sql.append(" AND l.serie LIKE ?");      params.add("%" + f.getSerie() + "%"); }
        if (f.getFormat()       != null && !f.getFormat().isBlank()) { sql.append(" AND l.format = ?");        params.add(f.getFormat()); }
        if (f.getIdioma()       != null && !f.getIdioma().isBlank()) { sql.append(" AND l.idioma LIKE ?");     params.add("%" + f.getIdioma() + "%"); }
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
