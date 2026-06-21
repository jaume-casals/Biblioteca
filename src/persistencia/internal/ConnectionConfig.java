package persistencia.internal;

public record ConnectionConfig(
    String dbType, String host, String user, String password,
    String profile, String testUrl) {

    public ConnectionConfig {
        java.util.Objects.requireNonNull(dbType, "dbType");
        java.util.Objects.requireNonNull(host, "host");
        java.util.Objects.requireNonNull(user, "user");
        java.util.Objects.requireNonNull(password, "password");
    }

    @Override
    public String toString() {
        return "ConnectionConfig[dbType=" + dbType + ", host=" + host + ", user=" + user
            + ", password=***, profile=" + profile + "]";
    }
}
