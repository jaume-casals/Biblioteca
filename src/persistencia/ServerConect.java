package persistencia;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;

import herramienta.Config;
import herramienta.I18n;

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

	private record Migration(int version, String sql) {}

	private static final Migration[] MIGRATIONS = {
		new Migration(1,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS imatge_blob BLOB"),
		new Migration(2,  "CREATE TABLE IF NOT EXISTS llista (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL)"),
		new Migration(3,  "CREATE TABLE IF NOT EXISTS llibre_llista (" +
			"isbn BIGINT NOT NULL, llista_id INT NOT NULL, " +
			"valoracio FLOAT DEFAULT 0.0, llegit BOOLEAN DEFAULT FALSE, " +
			"PRIMARY KEY (isbn, llista_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (llista_id) REFERENCES llista(id) ON DELETE CASCADE)"),
		new Migration(4,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS notes VARCHAR(2048) DEFAULT ''"),
		new Migration(5,  "ALTER TABLE llista ADD COLUMN IF NOT EXISTS ordre INT DEFAULT 0"),
		new Migration(6,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_afegit TIMESTAMP DEFAULT CURRENT_TIMESTAMP"),
		new Migration(7,  "ALTER TABLE llista ADD COLUMN IF NOT EXISTS color VARCHAR(7) DEFAULT NULL"),
		new Migration(8,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines INT DEFAULT 0"),
		new Migration(9,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines_llegides INT DEFAULT 0"),
		new Migration(10, "CREATE TABLE IF NOT EXISTS prestec (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"nom_persona VARCHAR(255) NOT NULL, " +
			"data_prestec DATE NOT NULL, " +
			"retornat BOOLEAN DEFAULT FALSE, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"),
		new Migration(11, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS editorial VARCHAR(255) DEFAULT ''"),
		new Migration(12, "CREATE TABLE IF NOT EXISTS tag (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL UNIQUE)"),
		new Migration(13, "CREATE TABLE IF NOT EXISTS llibre_tag (" +
			"isbn BIGINT NOT NULL, tag_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, tag_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE)"),
		new Migration(14, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS serie VARCHAR(255) DEFAULT ''"),
		new Migration(15, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS volum INT DEFAULT 0"),
		new Migration(16, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_compra DATE DEFAULT NULL"),
		new Migration(17, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_lectura DATE DEFAULT NULL"),
		new Migration(18, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS idioma VARCHAR(100) DEFAULT NULL"),
		new Migration(19, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS format VARCHAR(50) DEFAULT NULL"),
		new Migration(20, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS desitjat BOOLEAN DEFAULT FALSE"),
		new Migration(21, "CREATE TABLE IF NOT EXISTS autor (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(500) NOT NULL UNIQUE)"),
		new Migration(22, "CREATE TABLE IF NOT EXISTS llibre_autor (" +
			"isbn BIGINT NOT NULL, autor_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, autor_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (autor_id) REFERENCES autor(id))"),
		new Migration(23, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pais_origen VARCHAR(100) DEFAULT NULL"),
		new Migration(24, "ALTER TABLE llibre MODIFY COLUMN notes TEXT"),
		new Migration(25, "CREATE TABLE IF NOT EXISTS lectura (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"data_inici DATE DEFAULT NULL, " +
			"data_fi DATE DEFAULT NULL, " +
			"pagines_llegides INT DEFAULT 0, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"),
		new Migration(26, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS estat VARCHAR(20) DEFAULT NULL"),
		new Migration(27, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS exemplars INT DEFAULT 1"),
		new Migration(28, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS llengua_original VARCHAR(100) DEFAULT NULL"),
		new Migration(29, "ALTER TABLE llibre MODIFY COLUMN valoracio DOUBLE"),
		new Migration(30, "ALTER TABLE llibre MODIFY COLUMN preu DOUBLE"),
		new Migration(31, "ALTER TABLE llibre_llista MODIFY COLUMN valoracio DOUBLE"),
		// Migració 32-34: passa els autors de la columna `llibre.autor` (string)
		// a la taula `llibre_autor` (relació N:M). Risc: si 33 falla parcialment
		// en una BBDD gran (OOM, FK violada, etc.) i 34 corre igual, es perd
		// la informació d'autors dels llibres que no s'han migrat.
		// Les sentències són idempotents (INSERT IGNORE + subselect amb NOT IN),
		// de manera que es poden re-correr manualment si cal — veure
		// `todo.txt` [1] Migration 34. Abans de re-córrer, comprova que
		// `SELECT COUNT(*) FROM llibre WHERE TRIM(autor) IS NOT NULL AND TRIM(autor) != ''`
		// coincideix amb `SELECT COUNT(*) FROM llibre_autor`.
		new Migration(32, "INSERT IGNORE INTO autor (nom) SELECT DISTINCT TRIM(autor) FROM llibre WHERE TRIM(autor) IS NOT NULL AND TRIM(autor) != '' AND ISBN NOT IN (SELECT isbn FROM llibre_autor)"),
		new Migration(33, "INSERT IGNORE INTO llibre_autor (isbn, autor_id) SELECT l.ISBN, a.id FROM llibre l JOIN autor a ON a.nom = TRIM(l.autor) WHERE TRIM(l.autor) IS NOT NULL AND TRIM(l.autor) != '' AND l.ISBN NOT IN (SELECT isbn FROM llibre_autor)"),
		new Migration(34, "ALTER TABLE llibre DROP COLUMN autor"),
		new Migration(35, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_ca VARCHAR(500) DEFAULT NULL"),
		new Migration(36, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_es VARCHAR(500) DEFAULT NULL"),
		new Migration(37, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_en VARCHAR(500) DEFAULT NULL"),
		new Migration(38, "CREATE INDEX IF NOT EXISTS idx_llibre_nom ON llibre(nom)"),
		new Migration(39, "CREATE INDEX IF NOT EXISTS idx_llibre_editorial_serie ON llibre(editorial, serie)"),
	};

	private Connection con;

	public ServerConect() {}

	public Connection getConnection() { return con; }

	public static Connection testConnection(java.util.Properties props) throws Exception {
		String dbType = props.getProperty("dbType", "h2");
		ServerConect sc = new ServerConect();
		if ("h2".equals(dbType)) {
			String dir = System.getProperty("user.home") + "/.biblioteca";
			new File(dir).mkdirs();
			String profile = props.getProperty("dbProfile", "biblioteca");
 String url = "jdbc:h2:" + dir.trim().replaceAll("\\s+", "") + "/" + profile.trim().replaceAll("\\s+", "") + ";MODE=MySQL;NON_KEYWORDS=VALUE";
			return sc.connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
		} else {
			String host = props.getProperty("dbHost", "localhost");
			String url = "jdbc:mariadb://" + host + "/?characterEncoding=UTF-8&useUnicode=true";
			return sc.connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
				url, props.getProperty("dbUser", ""), props.getProperty("dbPassword", ""));
		}
	}

	public void createDatabase() {
		String testUrl = System.getProperty("biblioteca.h2.url");
		ConnectionConfig cfg = ConnectionFactory.withConfig(
			testUrl != null ? "h2" : Config.getDbType(),
			Config.getDbHost(), Config.getDbUser(), Config.getDbPassword(),
			Config.getDbProfile(), testUrl);
		createDatabase(cfg);
	}

	public void createDatabase(ConnectionConfig cfg) {
		try {
			if (cfg.testUrl() != null) {
				con = connectViaDriver("org.h2.Driver", "h2", cfg.testUrl(), "sa", "");
			} else if ("h2".equals(cfg.dbType())) {
				String dir = System.getProperty("user.home") + "/.biblioteca";
				new File(dir).mkdirs();
				 String url = "jdbc:h2:" + dir.trim().replaceAll("\\s+", "") + "/" + cfg.profile() + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
				con = connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
			} else {
				String url = "jdbc:mariadb://" + cfg.host() + "/?characterEncoding=UTF-8&useUnicode=true";
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
			throw new RuntimeException(I18n.t("err_db_connect") + " " + I18n.t("err_db_init_" + cfg.dbType()), e);
		}
	}

	private void runMigrations() throws SQLException {
		boolean prevAutoCommit = con.getAutoCommit();
		// Si la connexió ja està en una transacció aliena (prevAutoCommit == false),
		// no podem fer rollback de tota la transacció — fem servir un SAVEPOINT
		// per aïllar el rollback a les migracions.
		java.sql.Savepoint sp = prevAutoCommit ? null : con.setSavepoint();
		try (Statement st = con.createStatement()) {
			st.executeUpdate(CREATE_SCHEMA_VERSION);
			java.util.Set<Integer> applied = new java.util.HashSet<>();
			try (ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
				while (rs.next()) applied.add(rs.getInt(1));
			}
			con.setAutoCommit(false);
			try (PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)")) {
				for (Migration m : MIGRATIONS) {
					if (!applied.contains(m.version())) {
						st.executeUpdate(m.sql());
						ins.setInt(1, m.version());
						ins.execute();
						applied.add(m.version());
					}
				}
			}
			con.commit();
		} catch (SQLException e) {
			if (prevAutoCommit) { try { con.rollback(); } catch (SQLException ignored) {} }
			else { try { con.rollback(sp); } catch (SQLException ignored) {} }
			throw e;
		} finally {
			try { con.setAutoCommit(prevAutoCommit); } catch (SQLException ignored) {}
		}
	}

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
		return JarLocator.locate(diag, this::hasJars);
	}

	private boolean hasJars(File dir) {
		if (!dir.isDirectory()) return false;
		String[] files = dir.list((d, n) -> n.endsWith(".jar"));
		return files != null && files.length > 0;
	}

	public void closeConection() {
		if (con == null) return;
		try { con.close(); }
		catch (SQLException e) {
			System.err.println(MessageFormat.format(I18n.t("err_db_close"), e.getMessage()));
		} finally {
			con = null;
		}
	}
}
