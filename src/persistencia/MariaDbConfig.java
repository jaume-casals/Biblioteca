package persistencia;

public record MariaDbConfig(String host, String user, String password) implements DbProfile {
    @Override public String dbType() { return "mariadb"; }
    @Override public String toString() {
        return "MariaDbConfig[host=" + host + ", user=" + user + ", password=***]";
    }
}
