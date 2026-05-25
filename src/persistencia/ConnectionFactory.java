package persistencia;

import java.sql.Connection;

/**
 * Façade for {@link ServerConect}'s driver-loading + URL-building responsibilities, separated
 * from {@link SchemaManager}. Used by callers that want a typed entry point.
 */
public final class ConnectionFactory {
    private ConnectionFactory() {}

    public static Connection open() {
        ServerConect sc = new ServerConect();
        sc.createDatabase();
        return sc.getConnection();
    }
}
