package persistencia;

public record ConnectionConfig(
    String dbType, String host, String user, String password,
    String profile, String testUrl) {}
