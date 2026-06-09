package herramienta;

/** Typed read-only view of database-related {@link Config} keys. */
public final class DbConfig {
    private DbConfig() {}

    public static String type()     { return Config.getDbType(); }
    public static String host()     { return Config.getDbHost(); }
    public static String user()     { return Config.getDbUser(); }
    public static String password() { return Config.getDbPassword(); }
    public static String profile()  { return Config.getDbProfile(); }
    public static boolean hasPassword() { return Config.hasDbPassword(); }
}
