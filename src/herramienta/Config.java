package herramienta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private static final File FILE = new File(
        System.getProperty("user.home") + "/.biblioteca/config.properties");

    private static final Properties props = new Properties();

    static {
        if (FILE.exists()) {
            try (FileInputStream in = new FileInputStream(FILE)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    public static boolean isDarkMode() {
        return Boolean.parseBoolean(props.getProperty("darkMode", "false"));
    }

    public static void setDarkMode(boolean dark) {
        props.setProperty("darkMode", String.valueOf(dark));
        save();
    }

    public static String getDbHost() { return props.getProperty("dbHost", "localhost"); }
    public static void setDbHost(String host) { props.setProperty("dbHost", host); save(); }

    public static String getDbUser() { return props.getProperty("dbUser", "user"); }
    public static void setDbUser(String user) { props.setProperty("dbUser", user); save(); }

    public static String getDbPassword() { return props.getProperty("dbPassword", ""); }
    public static void setDbPassword(String pw) { props.setProperty("dbPassword", pw); save(); }

    public static String getDefaultImgDir() {
        return props.getProperty("defaultImgDir", System.getProperty("user.home"));
    }
    public static void setDefaultImgDir(String dir) { props.setProperty("defaultImgDir", dir); save(); }

    /** "h2" (embedded, default) or "mariadb" (external server). */
    public static String getDbType() { return props.getProperty("dbType", "h2"); }
    public static void setDbType(String type) { props.setProperty("dbType", type); save(); }

    private static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(FILE)) {
                props.store(out, "Biblioteca configuration");
            }
        } catch (IOException ignored) {}
    }
}
