package persistencia;

import domini.Llibre;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Única font de veritat per als més de 25 camps compartits entre les sentències
 * {@code INSERT} i {@code UPDATE} de {@code Llibre}. El DAO abans escrivia el
 * mateix conjunt de 25 línies de crides {@code ps.setXxx(N, ll.getYyy())} a
 * tres llocs (insert, update, més una forma lleugerament diferent a
 * {@code ServeiCopiaSeguretat.escriureLlibreINSERT}). Aquesta classe centralitza la
 * <i>llista de valors</i> per SQL de manera que tots els consumidors es mantinguin
 * sincronitzats quan s'afegeix una columna nova.
 *
 * <p>Cada valor pot ser:
 * <ul>
 *   <li>Un primitiu encaixat / String / byte[] / java.sql.Date — vinculat tal qual.</li>
 *   <li>{@code null} — vinculat com a SQL NULL del tipus inferit.</li>
 *   <li>{@link Nul} amb un tipus SQL concret — vinculat com a NULL tipat.</li>
 * </ul>
 * La coerció de tipus reflecteix el comportament anterior (string buit en lloc
 * de NULL per a camps de text curts; valor per defecte encaixat per a numèrics nuls).
 */
public final class LlibreFieldBindings {

    private LlibreFieldBindings() {}

    /** Marcador per a un SQL NULL d'un tipus JDBC concret. */
    public static final class Nul {
        public final int sqlType;
        public Nul(int sqlType) { this.sqlType = sqlType; }
    }

    private static Integer intOrZero(Integer n) { return n == null ? 0 : n; }
    private static Double doubleOrZero(Double n) { return n == null ? 0.0 : n; }
    private static String sOrNull(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static String sOrEmpty(String v) { return v == null ? "" : v; }

    /**
     * Constructor de llista de valors únic usat tant per {@link #forInsert(Llibre)}
     * com per {@link #forUpdate(Llibre)}. Les dues SQL difereixen només en:
     * <ul>
     *   <li>Si la columna {@code ISBN} s'inclou (només INSERT).</li>
     *   <li>Si la columna {@code imatge_blob} s'inclou
     *       (només INSERT).</li>
     * </ul>
     * Aquest és tot el delta — mateix ordre, mateixos ajudants de coerció de valors,
     * mateix contracte de vinculació. La implementació anterior tenia
     * dos arrays gairebé idèntics de 25/27 línies que es desvanllaven quan
     * s'afegia una columna (el finding HIGH de tot.txt sobre
     * {@code LlibreDaoCore}).
     */
    public static Object[] forLlibre(Llibre ll, boolean includeIsbn, boolean includeBlob) {
        java.util.List<Object> vals = new java.util.ArrayList<>(27);
        if (includeIsbn)             vals.add(ll.obtenirISBN());
        vals.add(sOrEmpty(ll.obtenirNom()));
        vals.add(intOrZero(ll.obtenirAny()));
        vals.add(sOrEmpty(ll.obtenirDescripcio()));
        vals.add(doubleOrZero(ll.obtenirValoracio()));
        vals.add(doubleOrZero(ll.obtenirPreu()));
        vals.add(Boolean.TRUE.equals(ll.obtenirLlegit()));
        vals.add(sOrEmpty(ll.obtenirImatge()));
        if (includeBlob)             vals.add(ll.obtenirImatgeBlob());
        vals.add(ll.obtenirNotes());
        vals.add(ll.obtenirPagines());
        vals.add(ll.obtenirPaginesLlegides());
        vals.add(ll.obtenirEditorial());
        vals.add(ll.obtenirSerie());
        vals.add(ll.obtenirVolum());
        vals.add(dateOrNull(ll.obtenirDataCompra()));
        vals.add(dateOrNull(ll.obtenirDataLectura()));
        vals.add(sOrNull(ll.obtenirIdioma()));
        vals.add(sOrNull(ll.obtenirFormat()));
        vals.add(ll.esDesitjat());
        vals.add(sOrNull(ll.obtenirPaisOrigen()));
        vals.add(sOrNull(ll.obtenirEstat()));
        vals.add(Math.max(1, ll.obtenirExemplars()));
        vals.add(sOrNull(ll.obtenirLlenguaOriginal()));
        vals.add(sOrNull(ll.obtenirNomCa()));
        vals.add(sOrNull(ll.obtenirNomEs()));
        vals.add(sOrNull(ll.obtenirNomEn()));
        return vals.toArray();
    }

    /** Llista de 27 valors per a l'INSERT (inclou {@code ISBN} i
     *  {@code imatge_blob}). */
    public static Object[] forInsert(Llibre ll) { return forLlibre(ll, true, true); }

    /** Llista de 25 valors per a l'UPDATE (sense {@code imatge_blob}, sense
     *  {@code ISBN}). El consumidor afegeix l'ISBN de la clàusula WHERE. */
    public static Object[] forUpdate(Llibre ll) { return forLlibre(ll, false, false); }

    /** Llista de 27 columnes per a l'INSERT, en el mateix ordre que
     *  {@link #forInsert(Llibre)}. Usada per {@code ServeiCopiaSeguretat}
     *  per generar la llista de columnes SQL sense reescriure els noms
     *  (segons el finding MEDIUM de tot.txt sobre la deriva de la llista de columnes d'INSERT). */
    public static final String[] COLUMNS_INSERT = {
        "ISBN", "nom", "any", "descripcio", "valoracio", "preu", "llegit", "imatge",
        "imatge_blob", "notes", "pagines", "pagines_llegides", "editorial", "serie",
        "volum", "data_compra", "data_lectura", "idioma", "format", "desitjat",
        "pais_origen", "estat", "exemplars", "llengua_original", "nom_ca", "nom_es", "nom_en"
    };

    /** Genera la llista de columnes com un únic fragment SQL separat per comes
     *  (p.ex. {@code "`ISBN`,`nom`,`any`,..."}) per usar en un INSERT. */
    public static String inserirColumnsSql() {
        StringBuilder sb = new StringBuilder(COLUMNS_INSERT.length * 12);
        for (int i = 0; i < COLUMNS_INSERT.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('`').append(COLUMNS_INSERT[i]).append('`');
        }
        return sb.toString();
    }

    /** Genera la llista de placeholders per a un INSERT de 27 valors
     *  (p.ex. {@code "?,?,?,...,?"}). */
    public static String inserirPlaceholders() {
        StringBuilder sb = new StringBuilder(COLUMNS_INSERT.length * 2);
        for (int i = 0; i < COLUMNS_INSERT.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private static Object dateOrNull(String s) {
        if (s == null) return new Nul(Types.DATE);
        try { return java.sql.Date.valueOf(s); }
        catch (IllegalArgumentException e) { return new Nul(Types.DATE); }
    }

    /** Vincula un sol valor al PreparedStatement, amb el tractament de null
     *  conscient del tipus que l'antic codi inline feia a cada crida. */
    public static void bind(PreparedStatement ps, int idx, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.VARCHAR);
        } else if (value instanceof Nul n) {
            ps.setNull(idx, n.sqlType);
        } else if (value instanceof Integer i) {
            ps.setInt(idx, i);
        } else if (value instanceof Long l) {
            ps.setLong(idx, l);
        } else if (value instanceof Double d) {
            ps.setDouble(idx, d);
        } else if (value instanceof Boolean b) {
            ps.setBoolean(idx, b);
        } else if (value instanceof java.sql.Date d) {
            ps.setDate(idx, d);
        } else if (value instanceof byte[] b) {
            ps.setBytes(idx, b);
        } else {
            ps.setString(idx, String.valueOf(value));
        }
    }
}
