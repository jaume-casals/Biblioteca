package persistencia.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import domini.Llibre;

import persistencia.dao.LlibreDaoCore;
import persistencia.dao.LlistaDao;
/**
 * Mapejadors estàtics de files {@link ResultSet} a objectes de domini {@link Llibre}.
 * Compartits entre {@link LlibreDaoCore} (els seus propis SELECTs) i {@link LlistaDao}
 * (SELECTs entre taules que llegeixen files de llibre). Els noms de columna han de
 * coincidir amb les llistes canòniques de SELECT a {@link LlibreDaoCore}.
 */
public final class LlibreMapper {
    private LlibreMapper() {}

    public static Llibre buildLlibreLight(ResultSet rs) throws SQLException {
        Llibre l = buildLlibreCore(rs, null);
        l.posarHasBlob(rs.getBoolean("has_blob"));
        fillLlibreTail(l, rs, false);
        l.posarCampsPesatsCarregats(false);
        return l;
    }

    public static Llibre buildLlibre(ResultSet rs) throws SQLException {
        Llibre l = buildLlibreCore(rs, rs.getString("descripcio"));
        l.posarHasBlob(rs.getBoolean("has_blob"));
        fillLlibreTail(l, rs, true);
        return l;
    }

    private static Llibre buildLlibreCore(ResultSet rs, String descripcio) throws SQLException {
        return Llibre.builder()
            .isbn(rs.getLong("ISBN"))
            .nom(rs.getString("nom"))
            .autor(rs.getString("autor"))
            .any(rs.getObject("any", Integer.class))
            .descripcio(descripcio)
            .valoracio(rs.getObject("valoracio", Double.class))
            .preu(rs.getObject("preu", Double.class))
            .llegit(rs.getBoolean("llegit"))
            .imatge(rs.getString("imatge"))
            .build();
    }

    /** @param includeNotes si és cert, llegeix la columna {@code notes} del result set */
    private static void fillLlibreTail(Llibre l, ResultSet rs, boolean includeNotes) throws SQLException {
        if (includeNotes) {
            l.posarNotes(rs.getString("notes"));
        }
        l.posarPagines(rs.getInt("pagines"));
        l.posarPaginesLlegides(rs.getInt("pagines_llegides"));
        l.posarEditorial(rs.getString("editorial"));
        l.posarSerie(rs.getString("serie"));
        l.posarVolum(rs.getInt("volum"));
        l.posarDataCompra(rs.getString("data_compra"));
        l.posarDataLectura(rs.getString("data_lectura"));
        l.posarIdioma(rs.getString("idioma"));
        l.posarFormat(rs.getString("format"));
        l.posarDesitjat(rs.getBoolean("desitjat"));
        l.posarPaisOrigen(rs.getString("pais_origen"));
        l.posarEstat(rs.getString("estat"));
        int exemplars = rs.getInt("exemplars");
        l.posarExemplars(exemplars > 0 ? exemplars : 1);
        l.posarLlenguaOriginal(rs.getString("llengua_original"));
        l.posarNomCa(rs.getString("nom_ca"));
        l.posarNomEs(rs.getString("nom_es"));
        l.posarNomEn(rs.getString("nom_en"));
    }
}
