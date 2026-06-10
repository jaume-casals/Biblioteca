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
    public static final class Null {
        public final int sqlType;
        public Null(int sqlType) { this.sqlType = sqlType; }
    }

    private static Integer intOrZero(Integer n) { return n == null ? Integer.valueOf(0) : n; }
    private static Double doubleOrZero(Double n) { return n == null ? Double.valueOf(0.0) : n; }
    private static String sOrNull(String v) { return (v == null || v.isBlank()) ? null : v; }
    private static String sOrEmpty(String v) { return v == null ? "" : v; }

    /**
     * 27-value list for the INSERT (includes {@code imatge_blob} at
     * position 9 which is the only column missing from the UPDATE).
     */
    public static Object[] forInsert(Llibre ll) {
        return new Object[] {
            ll.getISBN(),
            sOrEmpty(ll.getNom()),
            intOrZero(ll.getAny()),
            sOrEmpty(ll.getDescripcio()),
            doubleOrZero(ll.getValoracio()),
            doubleOrZero(ll.getPreu()),
            Boolean.TRUE.equals(ll.getLlegit()),
            sOrEmpty(ll.getImatge()),
            ll.getImatgeBlob(),
            ll.getNotes(),
            ll.getPagines(),
            ll.getPaginesLlegides(),
            ll.getEditorial(),
            ll.getSerie(),
            ll.getVolum(),
            dateOrNull(ll.getDataCompra()),
            dateOrNull(ll.getDataLectura()),
            sOrNull(ll.getIdioma()),
            sOrNull(ll.getFormat()),
            ll.isDesitjat(),
            sOrNull(ll.getPaisOrigen()),
            sOrNull(ll.getEstat()),
            Math.max(1, ll.getExemplars()),
            sOrNull(ll.getLlenguaOriginal()),
            sOrNull(ll.getNomCa()),
            sOrNull(ll.getNomEs()),
            sOrNull(ll.getNomEn())
        };
    }

    /**
     * 25-value list for the UPDATE (no {@code imatge_blob}, no
     * {@code ISBN}).  The caller appends the WHERE-clause ISBN.
     */
    public static Object[] forUpdate(Llibre ll) {
        return new Object[] {
            sOrEmpty(ll.getNom()),
            intOrZero(ll.getAny()),
            sOrEmpty(ll.getDescripcio()),
            doubleOrZero(ll.getValoracio()),
            doubleOrZero(ll.getPreu()),
            Boolean.TRUE.equals(ll.getLlegit()),
            sOrEmpty(ll.getImatge()),
            ll.getNotes(),
            ll.getPagines(),
            ll.getPaginesLlegides(),
            ll.getEditorial(),
            ll.getSerie(),
            ll.getVolum(),
            dateOrNull(ll.getDataCompra()),
            dateOrNull(ll.getDataLectura()),
            sOrNull(ll.getIdioma()),
            sOrNull(ll.getFormat()),
            ll.isDesitjat(),
            sOrNull(ll.getPaisOrigen()),
            sOrNull(ll.getEstat()),
            Math.max(1, ll.getExemplars()),
            sOrNull(ll.getLlenguaOriginal()),
            sOrNull(ll.getNomCa()),
            sOrNull(ll.getNomEs()),
            sOrNull(ll.getNomEn())
        };
    }

    private static Object dateOrNull(String s) {
        if (s == null) return new Null(Types.DATE);
        try { return java.sql.Date.valueOf(s); }
        catch (IllegalArgumentException e) { return new Null(Types.DATE); }
    }

    /** Binds a single value to the PreparedStatement, with the type-aware
     *  null handling the old inline code used to do per call. */
    public static void bind(PreparedStatement ps, int idx, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.VARCHAR);
        } else if (value instanceof Null n) {
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
