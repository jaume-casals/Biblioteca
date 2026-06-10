package persistencia;

import java.sql.ResultSet;
import java.sql.SQLException;

import domini.Llibre;

/**
 * Static mappers from {@link ResultSet} rows to {@link Llibre} domain objects.
 * Shared between {@link LlibreDaoCore} (own SELECTs) and {@link LlistaDao}
 * (cross-table SELECTs that read llibre rows).  Column names must match
 * the canonical SELECT lists in {@link LlibreDaoCore}.
 */
final class LlibreMapper {
    private LlibreMapper() {}

    static Llibre buildLlibreLight(ResultSet rs) throws SQLException {
        Llibre l = Llibre.builder()
            .isbn(rs.getLong("ISBN"))
            .nom(rs.getString("nom"))
            .autor(rs.getString("autor"))
            .any(rs.getObject("any", Integer.class))
            .descripcio(null)
            .valoracio(rs.getObject("valoracio", Double.class))
            .preu(rs.getObject("preu", Double.class))
            .llegit(rs.getBoolean("llegit"))
            .imatge(rs.getString("imatge"))
            .build();
        l.setHasBlob(rs.getBoolean("has_blob"));
        fillLlibreTail(l, rs, false);
        l.setHeavyFieldsLoaded(false);
        return l;
    }

    static Llibre buildLlibre(ResultSet rs) throws SQLException {
        Llibre l = Llibre.builder()
            .isbn(rs.getLong("ISBN"))
            .nom(rs.getString("nom"))
            .autor(rs.getString("autor"))
            .any(rs.getObject("any", Integer.class))
            .descripcio(rs.getString("descripcio"))
            .valoracio(rs.getObject("valoracio", Double.class))
            .preu(rs.getObject("preu", Double.class))
            .llegit(rs.getBoolean("llegit"))
            .imatge(rs.getString("imatge"))
            .build();
        l.setHasBlob(rs.getBoolean("has_blob"));
        l.setNotes(rs.getString("notes"));
        fillLlibreTail(l, rs, true);
        return l;
    }

    private static void fillLlibreTail(Llibre l, ResultSet rs, boolean withHeavy) throws SQLException {
        if (withHeavy) {
            l.setNotes(rs.getString("notes"));
        }
        l.setPagines(rs.getInt("pagines"));
        l.setPaginesLlegides(rs.getInt("pagines_llegides"));
        l.setEditorial(rs.getString("editorial"));
        l.setSerie(rs.getString("serie"));
        l.setVolum(rs.getInt("volum"));
        l.setDataCompra(rs.getString("data_compra"));
        l.setDataLectura(rs.getString("data_lectura"));
        l.setIdioma(rs.getString("idioma"));
        l.setFormat(rs.getString("format"));
        l.setDesitjat(rs.getBoolean("desitjat"));
        l.setPaisOrigen(rs.getString("pais_origen"));
        l.setEstat(rs.getString("estat"));
        int exemplars = rs.getInt("exemplars");
        l.setExemplars(exemplars > 0 ? exemplars : 1);
        l.setLlenguaOriginal(rs.getString("llengua_original"));
        l.setNomCa(rs.getString("nom_ca"));
        l.setNomEs(rs.getString("nom_es"));
        l.setNomEn(rs.getString("nom_en"));
    }
}
