package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Shared DAO helpers for read-only queries. */
final class RowMappers {
    private RowMappers() {}

    @FunctionalInterface interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
    @FunctionalInterface interface PsBinder { void bind(PreparedStatement ps) throws SQLException; }

    static <T> List<T> queryAll(Connection con, String sql, RowMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) out.add(mapper.map(rs));
        }
        return out;
    }

    static <T> List<T> queryWithParams(Connection con, String sql, PsBinder binder, RowMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
        }
        return out;
    }
}
