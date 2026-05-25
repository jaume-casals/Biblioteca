package domini;

import java.sql.SQLException;

/**
 * Lambda body that may throw {@link SQLException}. Used by domain-layer try/catch wrappers
 * to convert SQL errors into {@link BibliotecaException} without boilerplate at each call site.
 */
@FunctionalInterface
public interface SqlOp {
    void run() throws SQLException;

    /** Run {@code op}; rethrow any SQLException as a {@link BibliotecaException} with the cause preserved. */
    static void domain(SqlOp op) {
        try { op.run(); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }
}
