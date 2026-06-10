package herramienta;

/** Typed read/write view of database-related {@link Config} keys. */
public final class DbConfig {
    private DbConfig() {}

    // Read API
    public static String type()     { return Config.getDbType(); }
    public static String host()     { return Config.getDbHost(); }
    public static String user()     { return Config.getDbUser(); }
    public static String password() { return Config.getDbPassword(); }
    public static String profile()  { return Config.getDbProfile(); }
    public static boolean hasPassword() { return Config.hasDbPassword(); }

    // Write API
    public static void setType(String type)       { Config.setDbType(type); }
    public static void setHost(String host)       { Config.setDbHost(host); }
    public static void setUser(String user)       { Config.setDbUser(user); }
    public static void setPassword(String pw)     { Config.setDbPassword(pw); }
    public static void setProfile(String name)    { Config.setDbProfile(name); }
}
