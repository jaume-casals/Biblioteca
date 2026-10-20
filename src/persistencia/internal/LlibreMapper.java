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
 *
 * <p><b>Accés per índex de columna.</b> Els accessors de JDBC accepten el nom de
 * la columna ({@code rs.getString("nom")}) o el seu índex ({@code rs.getString(2)}).
 * Els primers obliguen el driver a resoldre el nom <em>per fila</em>: H2
 * normalitza el nom ({@code StringUtils.toUpperEnglish}, que reserva una cadena
 * nova), en calcula el hash i consulta un {@code HashMap}. Amb 23 columnes per
 * fila, això era el 40% del temps del procés de càrrega de la biblioteca
 * (mesurat amb JFR: 492 de 1639 mostres eren
 * {@code HashMap.getNode} sota {@code JdbcResultSet.getColumnIndex}).
 * Els índexs es resolen <b>una sola vegada per result set</b> amb
 * {@link ResultSet#findColumn(String)} i després s'accedeix per índex, que és
 * un accés directe sense cap cerca.
 *
 * <p>Per això els DAO han de construir un {@link Cols} abans del bucle de
 * files i passar-lo a les versions de mapper que el reben: resoldre els
 * índexs dins del bucle tornaria a pagar la cerca per fila.
 */
public final class LlibreMapper {
    private LlibreMapper() {}

    /**
     * Índexs de les columnes d'un result set de llibre, resolts una sola
     * vegada per consulta.
     *
     * <p>Les columnes pesades ({@code descripcio}, {@code notes}) no existeixen
     * a la projecció lleugera: el seu índex val {@code -1} i el consumidor no
     * les llegeix.
     */
    public static final class Cols {
        public final int isbn, nom, autor, any, descripcio, valoracio, preu, llegit, imatge, hasBlob,
                 pagines, paginesLlegides, editorial, serie, volum, dataCompra, dataLectura,
                 idioma, format, desitjat, paisOrigen, estat, exemplars, llenguaOriginal,
                 nomCa, nomEs, nomEn, notes;

        public Cols(ResultSet rs) throws SQLException {
            isbn            = idx(rs, "ISBN");
            nom             = idx(rs, "nom");
            autor           = idx(rs, "autor");
            any             = idx(rs, "any");
            descripcio      = idx(rs, "descripcio");
            valoracio       = idx(rs, "valoracio");
            preu            = idx(rs, "preu");
            llegit          = idx(rs, "llegit");
            imatge          = idx(rs, "imatge");
            hasBlob         = idx(rs, "has_blob");
            pagines         = idx(rs, "pagines");
            paginesLlegides = idx(rs, "pagines_llegides");
            editorial       = idx(rs, "editorial");
            serie           = idx(rs, "serie");
            volum           = idx(rs, "volum");
            dataCompra      = idx(rs, "data_compra");
            dataLectura     = idx(rs, "data_lectura");
            idioma          = idx(rs, "idioma");
            format          = idx(rs, "format");
            desitjat        = idx(rs, "desitjat");
            paisOrigen      = idx(rs, "pais_origen");
            estat           = idx(rs, "estat");
            exemplars       = idx(rs, "exemplars");
            llenguaOriginal = idx(rs, "llengua_original");
            nomCa           = idx(rs, "nom_ca");
            nomEs           = idx(rs, "nom_es");
            nomEn           = idx(rs, "nom_en");
            notes           = idx(rs, "notes");
        }
    }

    /** Índex de la columna, o {@code -1} si la projecció no la conté. */
    private static int idx(ResultSet rs, String name) {
        try {
            return rs.findColumn(name);
        } catch (SQLException e) {
            return -1;
        }
    }

    /** Com {@link #buildLlibreLight(ResultSet, Cols)} però resol els índexs de columna. */
    public static Llibre buildLlibreLight(ResultSet rs) throws SQLException {
        return buildLlibreLight(rs, new Cols(rs));
    }

    public static Llibre buildLlibreLight(ResultSet rs, Cols c) throws SQLException {
        Llibre l = buildLlibreCore(rs, c, null);
        l.posarHasBlob(rs.getBoolean(c.hasBlob));
        fillLlibreTail(l, rs, c, false);
        l.posarCampsPesatsCarregats(false);
        return l;
    }

    /** Com {@link #buildLlibre(ResultSet, Cols)} però resol els índexs de columna. */
    public static Llibre buildLlibre(ResultSet rs) throws SQLException {
        return buildLlibre(rs, new Cols(rs));
    }

    public static Llibre buildLlibre(ResultSet rs, Cols c) throws SQLException {
        Llibre l = buildLlibreCore(rs, c, rs.getString(c.descripcio));
        l.posarHasBlob(rs.getBoolean(c.hasBlob));
        fillLlibreTail(l, rs, c, true);
        l.posarCampsPesatsCarregats(true);
        return l;
    }

    private static Llibre buildLlibreCore(ResultSet rs, Cols c, String descripcio) throws SQLException {
        return Llibre.builder()
            .isbn(rs.getLong(c.isbn))
            .nom(rs.getString(c.nom))
            .autor(rs.getString(c.autor))
            .any(readInteger(rs, c.any))
            .descripcio(descripcio)
            .valoracio(readDouble(rs, c.valoracio))
            .preu(readDouble(rs, c.preu))
            .llegit(rs.getBoolean(c.llegit))
            .imatge(rs.getString(c.imatge))
            .build();
    }

    /** Llegeix un enter nullable sense passar pel camí {@code getObject(String, Class)}. */
    private static Integer readInteger(ResultSet rs, int idx) throws SQLException {
        if (idx < 0) return null;
        int v = rs.getInt(idx);
        return rs.wasNull() ? null : Integer.valueOf(v);
    }

    /** Llegeix un double nullable sense passar pel camí {@code getObject(String, Class)}. */
    private static Double readDouble(ResultSet rs, int idx) throws SQLException {
        if (idx < 0) return null;
        double v = rs.getDouble(idx);
        return rs.wasNull() ? null : Double.valueOf(v);
    }

    /** @param includeNotes si és cert, llegeix la columna {@code notes} del result set */
    private static void fillLlibreTail(Llibre l, ResultSet rs, Cols c, boolean includeNotes) throws SQLException {
        if (includeNotes) {
            l.posarNotes(rs.getString(c.notes));
        }
        l.posarPagines(rs.getInt(c.pagines));
        l.posarPaginesLlegides(rs.getInt(c.paginesLlegides));
        l.posarEditorial(rs.getString(c.editorial));
        l.posarSerie(rs.getString(c.serie));
        l.posarVolum(rs.getInt(c.volum));
        l.posarDataCompra(rs.getString(c.dataCompra));
        l.posarDataLectura(rs.getString(c.dataLectura));
        l.posarIdioma(rs.getString(c.idioma));
        l.posarFormat(rs.getString(c.format));
        l.posarDesitjat(rs.getBoolean(c.desitjat));
        l.posarPaisOrigen(rs.getString(c.paisOrigen));
        l.posarEstat(rs.getString(c.estat));
        int exemplars = rs.getInt(c.exemplars);
        l.posarExemplars(exemplars > 0 ? exemplars : 1);
        l.posarLlenguaOriginal(rs.getString(c.llenguaOriginal));
        l.posarNomCa(rs.getString(c.nomCa));
        l.posarNomEs(rs.getString(c.nomEs));
        l.posarNomEn(rs.getString(c.nomEn));
    }
}
