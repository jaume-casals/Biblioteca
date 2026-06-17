package persistencia;

import domini.Llibre;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Single source of truth for the 25+ shared fields between {@code Llibre}
 * {@code INSERT} and {@code UPDATE} statements.  The DAO used to spell
 * out the same 25 line set of {@code ps.setXxx(N, ll.getYyy())} calls in
 * three places (insert, update, plus a slightly-different shape in
 * {@code BackupService.writeLlibreINSERT}).  This class centralises the
 * <i>value list</i> per SQL so all callers stay in lockstep when a new
 * column is added.
 *
 * <p>Each value can be:
 * <ul>
 *   <li>A boxed primitive / String / byte[] / java.sql.Date — bound as-is.</li>
 *   <li>{@code null} — bound as SQL NULL of the inferred type.</li>
 *   <li>{@link Null} with a specific SQL type — bound as typed NULL.</li>
 * </ul>
 * Type coercion mirrors the previous inline behaviour (empty string
 * instead of NULL for short text fields; boxed-default for null
 * numerics).
 */
public final class LlibreFieldBindings {

    private LlibreFieldBindings() {}

    /** Marker for a SQL NULL of a specific JDBC type. */
    public static final class Nul {
        public final int sqlType;
        public Nul(int sqlType) { this.sqlType = sqlType; }
    }

    private static Integer intOrZero(Integer n) { return n == null ? 0 : n; }
    private static Double doubleOrZero(Double n) { return n == null ? 0.0 : n; }
    private static String sOrNull(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static String sOrEmpty(String v) { return v == null ? "" : v; }

    /**
     * Single value-list builder used by both {@link #forInsert(Llibre)}
     * and {@link #forUpdate(Llibre)}.  The two SQLs differ only in:
     * <ul>
     *   <li>Whether the {@code ISBN} column is included (INSERT only).</li>
     *   <li>Whether the {@code imatge_blob} column is included
     *       (INSERT only).</li>
     * </ul>
     * This is the entire delta — same order, same value coercion
     * helpers, same bind contract. The previous implementation had
     * two near-identical 25/27-line arrays that drifted out of sync
     * the moment a column was added (the tot.txt HIGH finding on
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
        vals.add(sOrNull(ll.getFormat()));
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

    /** 27-value list for the INSERT (includes {@code ISBN} and
     *  {@code imatge_blob}). */
    public static Object[] forInsert(Llibre ll) { return forLlibre(ll, true, true); }

    /** 25-value list for the UPDATE (no {@code imatge_blob}, no
     *  {@code ISBN}). The caller appends the WHERE-clause ISBN. */
    public static Object[] forUpdate(Llibre ll) { return forLlibre(ll, false, false); }

    /** 27-column list for the INSERT, in the same order as
     *  {@link #forInsert(Llibre)}. Used by {@code BackupService}
     *  to generate the SQL column list without re-typing the names
     *  (per the tot.txt MEDIUM finding on INSERT column-list drift). */
    public static final String[] COLUMNS_INSERT = {
        "ISBN", "nom", "any", "descripcio", "valoracio", "preu", "llegit", "imatge",
        "imatge_blob", "notes", "pagines", "pagines_llegides", "editorial", "serie",
        "volum", "data_compra", "data_lectura", "idioma", "format", "desitjat",
        "pais_origen", "estat", "exemplars", "llengua_original", "nom_ca", "nom_es", "nom_en"
    };

    /** Render the column list as a single comma-separated SQL fragment
     *  (e.g. {@code "`ISBN`,`nom`,`any`,..."}) for use in an INSERT. */
    public static String inserirColumnsSql() {
        StringBuilder sb = new StringBuilder(COLUMNS_INSERT.length * 12);
        for (int i = 0; i < COLUMNS_INSERT.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('`').append(COLUMNS_INSERT[i]).append('`');
        }
        return sb.toString();
    }

    /** Render the placeholder list for a 27-value INSERT
     *  (e.g. {@code "?,?,?,...,?"}). */
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

    /** Binds a single value to the PreparedStatement, with the type-aware
     *  null handling the old inline code used to do per call. */
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
