package herramienta;

/** Typed read/write view of database-related {@link Config} keys. */
public final class ConfiguracioDb {
    private ConfiguracioDb() {}

    // Read API
    public static String type()     { return Configuracio.obtenirDbType(); }
    public static String host()     { return Configuracio.obtenirDbHost(); }
    public static String user()     { return Configuracio.obtenirDbUser(); }
    public static String password() { return Configuracio.obtenirDbPassword(); }
    public static String profile()  { return Configuracio.obtenirDbProfile(); }
    public static boolean tePassword() { return Configuracio.teDbPassword(); }

    // Write API
    public static void setType(String type)       { Configuracio.posarDbType(type); }
    public static void posarHost(String host)       { Configuracio.posarDbHost(host); }
    public static void posarUser(String user)       { Configuracio.posarDbUser(user); }
    public static void posarPassword(String pw)     { Configuracio.posarDbPassword(pw); }
    public static void posarPassword(char[] pw)     { Configuracio.posarDbPassword(pw); }
    public static void posarProfile(String name)    { Configuracio.posarDbProfile(name); }
}
