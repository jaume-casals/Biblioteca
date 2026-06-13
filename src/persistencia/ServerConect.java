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

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ServerConect.class.getName());

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

	/** Set once inside {@link #createDatabase(ConnectionConfig)} or
	 *  {@link #createDatabase()} and never reassigned; {@link #closeConnection()}
	 *  nulls the field after closing. Do not assign a new Connection elsewhere. */
	private Connection con;

	public ServerConect() {}

	public Connection getConnection() { return con; }

	public static Connection testConnection(java.util.Properties props) throws Exception {
		return testConnection(props, props.getProperty("dbPassword", "").toCharArray());
	}

	/** Variant that takes the password as a {@code char[]} so the caller can
	 *  zero it after the call instead of leaving a {@code String} in the heap. */
	public static Connection testConnection(java.util.Properties props, char[] password) throws Exception {
		String dbType = props.getProperty("dbType", "h2");
		ServerConect sc = new ServerConect();
		if ("h2".equals(dbType)) {
			String dir = System.getProperty("user.home") + "/.biblioteca";
			new File(dir).mkdirs();
			String profile = sanitizeH2Profile(props.getProperty("dbProfile", "biblioteca"));
			String url = "jdbc:h2:" + dir.trim().replaceAll("^\\s+|\\s+$", "").replaceAll("/+$", "")
				+ "/" + profile + ";MODE=MySQL;NON_KEYWORDS=VALUE";
			return sc.connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
		} else {
			String host = props.getProperty("dbHost", "localhost");
			String url = "jdbc:mariadb://" + host + "/?characterEncoding=UTF-8&useUnicode=true";
			return sc.connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
				url, props.getProperty("dbUser", ""), new String(password));
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
				String url = "jdbc:h2:" + dir.strip().replaceAll("/+$", "")
					+ "/" + sanitizeH2Profile(cfg.profile()) + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
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

	/** Rejects path separators in H2 file profile names (jdbc:h2:dir/profile). */
	static String sanitizeH2Profile(String profile) {
		if (profile == null || profile.isBlank()) return "biblioteca";
		String p = profile.trim();
		if (p.contains("/") || p.contains("\\") || p.contains(".."))
			throw new IllegalArgumentException("Invalid H2 db profile name: " + profile);
		return p;
	}

	/**
	 * Applies pending schema migrations and records each completed version in
	 * {@code schema_version}. The transaction strategy depends on the connected
	 * engine, detected via {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
	 *
	 * <p><b>H2</b> supports transactional DDL. The DDL statement and the matching
	 * {@code schema_version} INSERT run inside a single JDBC transaction
	 * ({@code setAutoCommit(false)} → execute → {@code commit}, or
	 * {@code rollback} on failure). A mid-sequence failure on H2 leaves the
	 * schema unchanged and the migration is re-attempted on the next startup.
	 *
	 * <p><b>MariaDB / MySQL</b> do not support transactional DDL: every DDL
	 * statement implicitly commits and cannot be rolled back. On these engines
	 * the {@code setAutoCommit(false)} wrapper is intentionally skipped and
	 * each migration is applied as two independent atomic operations: the DDL
	 * (auto-committed) followed by the {@code schema_version} INSERT (its own
	 * auto-commit transaction). If the INSERT fails it is logged and the
	 * migration is left unrecorded so the next startup will retry it. If the
	 * DDL itself fails, the connection may have partially-migrated state:
	 * operators must inspect {@code schema_version} (which lists the
	 * migrations recorded as applied) and the live schema to determine which
	 * statements completed and finish the migration manually. All DDL in
	 * {@link #MIGRATIONS} is written to be idempotent (guards such as
	 * {@code IF NOT EXISTS}), so a retry against a partially-migrated schema
	 * is safe.
	 */
	private void runMigrations() throws SQLException {
		boolean isH2 = isH2Connection();
		boolean prevAutoCommit = con.getAutoCommit();
		try (Statement st = con.createStatement()) {
			st.executeUpdate(CREATE_SCHEMA_VERSION);
			java.util.Set<Integer> applied = new java.util.HashSet<>();
			try (ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
				while (rs.next()) applied.add(rs.getInt(1));
			}
			try (PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)")) {
				for (Migration m : MIGRATIONS) {
					if (applied.contains(m.version())) continue;
					boolean recorded = isH2
						? runMigrationH2(st, ins, m)
						: runMigrationNonTransactional(st, ins, m);
					if (recorded) applied.add(m.version());
				}
			}
		} finally {
			try { con.setAutoCommit(prevAutoCommit); } catch (SQLException ignored) {}
		}
	}

	private boolean isH2Connection() throws SQLException {
		String productName = con.getMetaData().getDatabaseProductName();
		return productName != null && productName.toLowerCase(java.util.Locale.ROOT).startsWith("h2");
	}

	private boolean runMigrationH2(Statement st, PreparedStatement ins, Migration m) throws SQLException {
		boolean prev = con.getAutoCommit();
		try {
			con.setAutoCommit(false);
			if (m.version() == 34) {
				if (!applyMigration34DropAutor(st)) {
					con.rollback();
					return false;
				}
			} else {
				st.executeUpdate(m.sql());
			}
			ins.setInt(1, m.version());
			ins.execute();
			con.commit();
			return true;
		} catch (SQLException e) {
			try { con.rollback(); } catch (SQLException ex) {
				LOG.log(java.util.logging.Level.WARNING, "Migration " + m.version() + " rollback failed", ex);
			}
			throw e;
		} finally {
			con.setAutoCommit(prev);
		}
	}

	private boolean runMigrationNonTransactional(Statement st, PreparedStatement ins, Migration m) throws SQLException {
		if (m.version() == 34) {
			applyMigration34DropAutor(st);
		} else {
			st.executeUpdate(m.sql());
		}
		try {
			ins.setInt(1, m.version());
			ins.execute();
			return true;
		} catch (SQLException e) {
			LOG.log(java.util.logging.Level.WARNING, "Migration " + m.version() + " schema_version insert failed", e);
			return false;
		}
	}

	/**
	 * Drops {@code llibre.autor} only when every non-empty legacy author row has a
	 * matching {@code llibre_autor} link. Skips (returns false) if unmigrated rows remain.
	 */
	private boolean applyMigration34DropAutor(Statement st) throws SQLException {
		if (!columnExists("llibre", "autor")) return true;
		int unmigrated;
		try (ResultSet rs = st.executeQuery(
				"SELECT COUNT(*) FROM llibre l WHERE TRIM(l.autor) IS NOT NULL AND TRIM(l.autor) != '' " +
				"AND l.ISBN NOT IN (SELECT isbn FROM llibre_autor)")) {
			rs.next();
			unmigrated = rs.getInt(1);
		}
		if (unmigrated > 0) {
			LOG.warning("Migration 34: skipping DROP COLUMN autor — " + unmigrated
				+ " book(s) still lack llibre_autor rows (re-run after fixing data)");
			return false;
		}
		st.executeUpdate("ALTER TABLE llibre DROP COLUMN autor");
		return true;
	}

	private boolean columnExists(String table, String column) throws SQLException {
		java.sql.DatabaseMetaData meta = con.getMetaData();
		String catalog = con.getCatalog();
		try (ResultSet rs = meta.getColumns(catalog, null, table, column)) {
			if (rs.next()) return true;
		}
		try (ResultSet rs = meta.getColumns(catalog, null, table.toUpperCase(java.util.Locale.ROOT),
				column.toUpperCase(java.util.Locale.ROOT))) {
			return rs.next();
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

	public void closeConnection() {
		if (con == null) return;
		try { con.close(); }
		catch (SQLException e) {
			LOG.log(java.util.logging.Level.WARNING, MessageFormat.format(I18n.t("err_db_close"), e.getMessage()), e);
		} finally {
			con = null;
		}
	}
}
