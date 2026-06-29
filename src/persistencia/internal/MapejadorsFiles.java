package persistencia.internal;

import domini.BibliotecaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Utilitats compartides de DAO per a consultes de només lectura. */
public final class MapejadorsFiles {
    private MapejadorsFiles() {}

    @FunctionalInterface public interface MapejadorFiles<T> { T map(ResultSet rs) throws SQLException; }
    @FunctionalInterface public interface LligadorPs { void bind(PreparedStatement ps) throws SQLException; }
    @FunctionalInterface public interface UsuariResultSet { void use(ResultSet rs) throws SQLException; }

    public static <T> List<T> queryAll(Connection con, String sql, MapejadorFiles<T> mapper) throws SQLException {
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return executeQuery(rs, mapper);
        }
    }

    public static <T> List<T> queryWithParams(Connection con, String sql, LligadorPs binder, MapejadorFiles<T> mapper) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return executeQuery(rs, mapper);
            }
        }
    }

    /** Executa una sentència d'escriptura (INSERT/UPDATE/DELETE) amb el binder donat. */
    public static void exec(Connection con, String sql, LligadorPs binder) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            ps.execute();
        }
    }

    /** Com {@link #queryAll} però re-llança {@link SQLException} com {@link BibliotecaException} amb el missatge donat. */
    public static <T> List<T> queryAllOrThrow(Connection con, String sql, String errMsg, MapejadorFiles<T> mapper) {
        try {
            return queryAll(con, sql, mapper);
        } catch (SQLException e) {
            throw new BibliotecaException(errMsg + ": " + e.getMessage(), e);
        }
    }

    /** Com {@link #queryWithParams} però re-llança {@link SQLException} com {@link BibliotecaException} amb el missatge donat. */
    public static <T> List<T> queryWithParamsOrThrow(Connection con, String sql, LligadorPs binder, String errMsg, MapejadorFiles<T> mapper) {
        try {
            return queryWithParams(con, sql, binder, mapper);
        } catch (SQLException e) {
            throw new BibliotecaException(errMsg + ": " + e.getMessage(), e);
        }
    }

    /** Com {@link #exec} però re-llança {@link SQLException} com {@link BibliotecaException} amb el missatge donat. */
    public static void execOrThrow(Connection con, String sql, String errMsg, LligadorPs binder) {
        try {
            exec(con, sql, binder);
        } catch (SQLException e) {
            throw new BibliotecaException(errMsg + ": " + e.getMessage(), e);
        }
    }

    /** Com {@link #queryWithParams} però retorna només la primera fila mapejada o {@code null} si no n'hi ha cap. */
    public static <T> T queryOneWithParamsOrThrow(Connection con, String sql, LligadorPs binder, String errMsg, MapejadorFiles<T> mapper) {
        List<T> all = queryWithParamsOrThrow(con, sql, binder, errMsg, mapper);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Com {@link #queryWithParams} però invoca {@code use} sobre la primera fila (si n'hi ha) i no retorna res. */
    public static void useOneRowOrThrow(Connection con, String sql, LligadorPs binder, String errMsg, UsuariResultSet use) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) use.use(rs);
            }
        } catch (SQLException e) {
            throw new BibliotecaException(errMsg + ": " + e.getMessage(), e);
        }
    }

    /** Executa un INSERT amb {@code RETURN_GENERATED_KEYS} i retorna la primera clau int generada. */
    public static int insertReturningKey(Connection con, String sql, LligadorPs binder) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.execute();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("insertReturningKey: no s'ha retornat cap clau generada");
                return rs.getInt(1);
            }
        }
    }

    /** Construeix una llista de marcadors de posició {@code "?,?,...,?"} de mida {@code n}. */
    public static String placeholders(int n) {
        StringBuilder sb = new StringBuilder(Math.max(0, n * 2));
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    private static <T> List<T> executeQuery(ResultSet rs, MapejadorFiles<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        while (rs.next()) out.add(mapper.map(rs));
        return out;
    }
}
