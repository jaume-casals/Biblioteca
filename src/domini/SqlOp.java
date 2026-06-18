package domini;

import java.sql.SQLException;

/**
 * Cos de lambda que pot llançar {@link SQLException}. Usat pels embolcalls try/catch de la capa
 * de domini per convertir errors de SQL en {@link BibliotecaException} sense boilerplate a cada
 * punt de crida.
 */
@FunctionalInterface
public interface SqlOp {
    void run() throws SQLException;

    /** Executa {@code op}; relança qualsevol SQLException com a {@link BibliotecaException} preservant la causa. */
    static void domain(SqlOp op) {
        try { op.run(); }
        catch (SQLException e) { throw new BibliotecaException(e.getMessage(), e); }
    }
}
