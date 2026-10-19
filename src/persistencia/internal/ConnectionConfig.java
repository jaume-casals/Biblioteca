package persistencia.internal;

public record ConnectionConfig(
    String dbType, String host, String user, String password,
    String profile, String testUrl, String database) {

    /**
     * Nom de l'esquema per defecte. Abans de fer injectable el nom de la base
     * de dades, el {@code CREATE DATABASE}/{@code USE} de
     * {@link ConnexioServidor} elia el literal {@code BIBLIOTECA} — aquest
     * valor el manté perquè el comportament per defecte no canvï.
     */
    public static final String DEFAULT_DATABASE = "BIBLIOTECA";

    public ConnectionConfig {
        java.util.Objects.requireNonNull(dbType, "dbType");
        java.util.Objects.requireNonNull(host, "host");
        java.util.Objects.requireNonNull(user, "user");
        java.util.Objects.requireNonNull(password, "password");
        if (database == null || database.isBlank()) database = DEFAULT_DATABASE;
    }

    /**
     * Constructor de compatibilitat: equival al de sis components d'abans,
     * amb l'esquema per defecte {@link #DEFAULT_DATABASE}.
     */
    public ConnectionConfig(String dbType, String host, String user, String password,
                            String profile, String testUrl) {
        this(dbType, host, user, password, profile, testUrl, DEFAULT_DATABASE);
    }

    @Override
    public String toString() {
        return "ConnectionConfig[dbType=" + dbType + ", host=" + host + ", user=" + user
            + ", password=***, profile=" + profile + ", database=" + database + "]";
    }
}
