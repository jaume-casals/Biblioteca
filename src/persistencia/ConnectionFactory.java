package persistencia;

import java.sql.Connection;

import herramienta.Config;

public final class ConnectionFactory {
    private ConnectionFactory() {}

    public static ConnectionConfig withConfig(String dbType, String host, String user,
            String password, String profile, String testUrl) {
        return new ConnectionConfig(dbType, host, user, password, profile, testUrl);
    }
}
