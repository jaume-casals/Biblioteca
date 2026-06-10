package persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link RowMappers}.
 * Uses a private constructor — exercises the public static helpers against
 * an in-memory H2 connection.
 */
class RowMappersTest {

    static {
        try { Class.forName("org.h2.Driver"); } catch (ClassNotFoundException ignored) {}
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:rowmappers_" + System.nanoTime()
            + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Test
    @DisplayName("queryAll: streams all rows through the mapper")
    void queryAllStreams() throws SQLException {
        try (Connection c = open();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE t (id INT, name VARCHAR(50))");
            s.executeUpdate("INSERT INTO t VALUES (1, 'A'), (2, 'B'), (3, 'C')");
            List<String> names = RowMappers.queryAll(c, "SELECT name FROM t ORDER BY id",
                rs -> rs.getString(1));
            assertThat(names).containsExactly("A", "B", "C");
        }
    }

    @Test
    @DisplayName("queryAll: empty result yields empty list")
    void queryAllEmpty() throws SQLException {
        try (Connection c = open();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE t (id INT)");
            List<String> r = RowMappers.queryAll(c, "SELECT id FROM t",
                rs -> rs.getString(1));
            assertThat(r).isEmpty();
        }
    }

    @Test
    @DisplayName("queryWithParams: binds parameter and runs mapper")
    void queryWithParamsBinds() throws SQLException {
        try (Connection c = open();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE t (id INT, name VARCHAR(50))");
            s.executeUpdate("INSERT INTO t VALUES (1, 'A'), (2, 'B'), (3, 'C')");
            List<String> r = RowMappers.queryWithParams(c,
                "SELECT name FROM t WHERE id >= ? ORDER BY id",
                ps -> ps.setInt(1, 2),
                rs -> rs.getString(1));
            assertThat(r).containsExactly("B", "C");
        }
    }

    @Test
    @DisplayName("queryWithParams: parameter binding is invoked exactly once")
    void queryWithParamsBinderCalledOnce() throws SQLException {
        try (Connection c = open();
             Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE t (id INT)");
            s.executeUpdate("INSERT INTO t VALUES (1), (2), (3)");
            AtomicInteger callCount = new AtomicInteger();
            List<Integer> r = RowMappers.queryWithParams(c,
                "SELECT id FROM t",
                ps -> { ps.setInt(1, 0); callCount.incrementAndGet(); },
                rs -> rs.getInt(1));
            assertThat(r).containsExactly(1, 2, 3);
            assertThat(callCount).hasValue(1);
        }
    }
}
