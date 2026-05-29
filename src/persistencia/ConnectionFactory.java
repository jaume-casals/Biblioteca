package persistencia;

import java.sql.Connection;

import herramienta.Config;

public final class ConnectionFactory {
    private ConnectionFactory() {}

    public static ConnectionConfig withConfig(String dbType, String host, String user,
            String password, String profile, String testUrl) {
        return new ConnectionConfig(dbType, host, user, password, profile, testUrl);
    }

    public static Connection open() {
        String testUrl = System.getProperty("biblioteca.h2.url");
        ConnectionConfig cfg = withConfig(
            testUrl != null ? "h2" : Config.getDbType(),
            Config.getDbHost(), Config.getDbUser(), Config.getDbPassword(),
            Config.getDbProfile(), testUrl);
        ServerConect sc = new ServerConect();
        sc.createDatabase(cfg);
        return sc.getConnection();
    }
}
