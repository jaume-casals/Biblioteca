package persistencia;

public record ConnectionConfig(
    String dbType, String host, String user, String password,
    String profile, String testUrl) {

    public ConnectionConfig {
        java.util.Objects.requireNonNull(dbType, "dbType");
    }

    @Override
    public String toString() {
        return "ConnectionConfig[dbType=" + dbType + ", host=" + host + ", user=" + user
            + ", password=***, profile=" + profile + "]";
    }
}
