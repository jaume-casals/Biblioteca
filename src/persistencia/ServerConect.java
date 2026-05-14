package persistencia;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import herramienta.Config;

public class ServerConect {

	private static final String CREATE_TABLE =
		"CREATE TABLE IF NOT EXISTS llibre(" +
		"ISBN BIGINT PRIMARY KEY, " +
		"nom VARCHAR(255) NOT NULL, " +
		"autor VARCHAR(255), " +
		"`any` INT, " +
		"descripcio VARCHAR(512), " +
		"valoracio FLOAT, " +
		"preu FLOAT, " +
		"llegit BOOLEAN, " +
		"imatge VARCHAR(255), " +
		"imatge_blob BLOB" +
		");";

	private static final String CREATE_SCHEMA_VERSION =
		"CREATE TABLE IF NOT EXISTS schema_version (version INT NOT NULL);";

	// Each entry: [version_number, sql_to_apply]
	private static final String[][] MIGRATIONS = {
		{"1", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS imatge_blob BLOB"},
		{"2", "CREATE TABLE IF NOT EXISTS llista (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL)"},
		{"3", "CREATE TABLE IF NOT EXISTS llibre_llista (" +
			"isbn BIGINT NOT NULL, llista_id INT NOT NULL, " +
			"valoracio FLOAT DEFAULT 0.0, llegit BOOLEAN DEFAULT FALSE, " +
			"PRIMARY KEY (isbn, llista_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (llista_id) REFERENCES llista(id) ON DELETE CASCADE)"},
		{"4", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS notes VARCHAR(2048) DEFAULT ''"},
		{"5", "ALTER TABLE llista ADD COLUMN IF NOT EXISTS ordre INT DEFAULT 0"},
		{"6", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_afegit TIMESTAMP DEFAULT CURRENT_TIMESTAMP"},
		{"7", "ALTER TABLE llista ADD COLUMN IF NOT EXISTS color VARCHAR(7) DEFAULT NULL"},
		{"8", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines INT DEFAULT 0"},
		{"9", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines_llegides INT DEFAULT 0"},
		{"10", "CREATE TABLE IF NOT EXISTS prestec (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"nom_persona VARCHAR(255) NOT NULL, " +
			"data_prestec DATE NOT NULL, " +
			"retornat BOOLEAN DEFAULT FALSE, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"},
		{"11", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS editorial VARCHAR(255) DEFAULT ''"},
		{"12", "CREATE TABLE IF NOT EXISTS tag (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL UNIQUE)"},
		{"14", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS serie VARCHAR(255) DEFAULT ''"},
		{"15", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS volum INT DEFAULT 0"},
		{"16", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_compra DATE DEFAULT NULL"},
		{"17", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_lectura DATE DEFAULT NULL"},
		{"18", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS idioma VARCHAR(100) DEFAULT NULL"},
		{"19", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS format VARCHAR(50) DEFAULT NULL"},
		{"20", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS desitjat BOOLEAN DEFAULT FALSE"},
		{"13", "CREATE TABLE IF NOT EXISTS llibre_tag (" +
			"isbn BIGINT NOT NULL, tag_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, tag_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE)"},
		{"21", "CREATE TABLE IF NOT EXISTS autor (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(500) NOT NULL UNIQUE)"},
		{"22", "CREATE TABLE IF NOT EXISTS llibre_autor (" +
			"isbn BIGINT NOT NULL, autor_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, autor_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (autor_id) REFERENCES autor(id))"},
		{"23", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pais_origen VARCHAR(100) DEFAULT NULL"},
		{"24", "ALTER TABLE llibre MODIFY COLUMN notes TEXT"},
		{"25", "CREATE TABLE IF NOT EXISTS lectura (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"data_inici DATE DEFAULT NULL, " +
			"data_fi DATE DEFAULT NULL, " +
			"pagines_llegides INT DEFAULT 0, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"},
		{"26", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS estat VARCHAR(20) DEFAULT NULL"},
		{"27", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS exemplars INT DEFAULT 1"},
		{"28", "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS llengua_original VARCHAR(100) DEFAULT NULL"},
	};

	private Connection con;

	public ServerConect() {}

	public Connection getConnection() { return con; }

	public void createDatabase() {
		String testUrl = System.getProperty("biblioteca.h2.url");
		createDatabase(new ConnectionConfig(
			testUrl != null ? "h2" : Config.getDbType(),
			Config.getDbHost(), Config.getDbUser(), Config.getDbPassword(),
			Config.getDbProfile(), testUrl));
	}

	public void createDatabase(ConnectionConfig cfg) {
		try {
			if (cfg.testUrl() != null) {
				con = connectViaDriver("org.h2.Driver", "h2", cfg.testUrl(), "sa", "");
			} else if ("h2".equals(cfg.dbType())) {
				String dir = System.getProperty("user.home") + "/.biblioteca";
				new File(dir).mkdirs();
				String url = "jdbc:h2:" + dir + "/" + cfg.profile() + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
				con = connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
			} else {
				String url = "jdbc:mariadb://" + cfg.host() + "/";
				con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
					url, cfg.user(), cfg.password());
				try (Statement s = con.createStatement()) {
					s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
					s.executeUpdate("USE BIBLIOTECA;");
				}
			}
			try (Statement s = con.createStatement()) {
				s.executeUpdate(CREATE_TABLE);
			}
			runMigrations();
		} catch (Exception e) {
			if (Boolean.getBoolean("biblioteca.test")) throw new RuntimeException(e);
			String hint = "h2".equals(cfg.dbType())
				? "Error inicialitzant la base de dades integrada."
				: "Comprova que MariaDB/MySQL estigui en execució i que l'usuari '"
					+ cfg.user() + "' tingui permisos.";
			System.err.println("No s'ha pogut connectar a la base de dades.\n" + hint + ": " + e.getMessage());
			System.exit(1);
		}
	}

	private void runMigrations() throws SQLException {
		try (Statement st = con.createStatement()) {
			st.executeUpdate(CREATE_SCHEMA_VERSION);
			java.util.Set<Integer> applied = new java.util.HashSet<>();
			try (ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
				while (rs.next()) applied.add(rs.getInt(1));
			}
			try (PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)")) {
				for (String[] m : MIGRATIONS) {
					int v = Integer.parseInt(m[0]);
					if (!applied.contains(v)) {
						st.executeUpdate(m[1]);
						ins.setInt(1, v);
						ins.execute();
						applied.add(v);
					}
				}
			}
		}
	}

	/**
	 * Loads JDBC driver and connects. Falls back to dynamic JAR loading from lib/
	 * if the driver class is not on the classpath (e.g. running from VSCode without
	 * the library configured in the project's runtime classpath).
	 */
	private Connection connectViaDriver(String driverClass, String jarNameHint,
			String url, String user, String password) throws Exception {
		java.sql.Driver driver;
		try {
			driver = (java.sql.Driver) Class.forName(driverClass).getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			driver = loadDriverFromLib(driverClass, jarNameHint);
		}
		java.util.Properties props = new java.util.Properties();
		if (user     != null) props.setProperty("user",     user);
		if (password != null) props.setProperty("password", password);
		Connection c = driver.connect(url, props);
		if (c == null) throw new Exception("Driver " + driverClass + " did not accept URL: " + url);
		return c;
	}

	private java.sql.Driver loadDriverFromLib(String driverClass, String jarNameHint) throws Exception {
		StringBuilder diag = new StringBuilder();
		File libDir = findLibDir(diag);
		File[] jars = libDir.listFiles(
			f -> f.getName().contains(jarNameHint) && f.getName().endsWith(".jar"));
		if (jars == null || jars.length == 0) {
			diag.insert(0, "Driver JAR '" + jarNameHint + "' not found. Search log:\n");
			throw new ClassNotFoundException(diag.toString());
		}
		java.net.URLClassLoader cl = new java.net.URLClassLoader(
			new java.net.URL[]{jars[0].toURI().toURL()},
			ClassLoader.getSystemClassLoader());
		return (java.sql.Driver) Class.forName(driverClass, true, cl).getDeclaredConstructor().newInstance();
	}

	private File findLibDir(StringBuilder diag) {
		diag.append("  user.dir=").append(System.getProperty("user.dir")).append("\n");
		diag.append("  biblioteca.root=").append(System.getProperty("biblioteca.root")).append("\n");

		// 1. Explicit project root via -Dbiblioteca.root (launch.json vmArgs)
		String root = System.getProperty("biblioteca.root");
		if (root != null && !root.isBlank()) {
			File lib = new File(root, "lib");
			diag.append("  [1] ").append(lib.getAbsolutePath())
				.append(lib.isDirectory() ? " EXISTS" : " missing").append("\n");
			if (lib.isDirectory()) return lib;
		}

		// 2. Working directory lib/
		File wdLib = new File(System.getProperty("user.dir"), "lib");
		diag.append("  [2] ").append(wdLib.getAbsolutePath())
			.append(hasJars(wdLib) ? " HAS_JARS" : (wdLib.isDirectory() ? " no-jars" : " missing")).append("\n");
		if (hasJars(wdLib)) return wdLib;

		// 3. Walk down one level from working dir (e.g. working dir is parent of project)
		File[] children = new File(System.getProperty("user.dir")).listFiles(File::isDirectory);
		if (children != null) {
			for (File child : children) {
				File lib = new File(child, "lib");
				diag.append("  [3.d] ").append(lib.getAbsolutePath())
					.append(hasJars(lib) ? " HAS_JARS" : (lib.isDirectory() ? " no-jars" : " missing")).append("\n");
				if (hasJars(lib)) return lib;
			}
		}

		// 4. Walk up from working dir
		File dir = new File(System.getProperty("user.dir"));
		for (int i = 0; i < 6; i++) {
			File lib = new File(dir, "lib");
			diag.append("  [4." + i + "] ").append(lib.getAbsolutePath())
				.append(hasJars(lib) ? " HAS_JARS" : (lib.isDirectory() ? " no-jars" : " missing")).append("\n");
			if (hasJars(lib)) return lib;
			dir = dir.getParentFile();
			if (dir == null) break;
		}

		// 5. Walk up from class file location
		try {
			java.net.URL loc = ServerConect.class.getProtectionDomain().getCodeSource().getLocation();
			diag.append("  classSource=").append(loc).append("\n");
			dir = new File(loc.toURI());
			for (int i = 0; i < 10; i++) {
				File lib = new File(dir, "lib");
				diag.append("  [5." + i + "] ").append(lib.getAbsolutePath())
					.append(hasJars(lib) ? " HAS_JARS" : (lib.isDirectory() ? " no-jars" : " missing")).append("\n");
				if (hasJars(lib)) return lib;
				dir = dir.getParentFile();
				if (dir == null) break;
			}
		} catch (Exception e) {
			diag.append("  classSource=ERROR:").append(e.getMessage()).append("\n");
		}

		return wdLib; // last resort — let caller report the path
	}

	private boolean hasJars(File dir) {
		if (!dir.isDirectory()) return false;
		String[] files = dir.list((d, n) -> n.endsWith(".jar"));
		return files != null && files.length > 0;
	}

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			System.err.println("Fallo al tancar la connexió: " + e.getMessage());
		}
	}
}
