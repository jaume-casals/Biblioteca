package persistencia.internal;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;

import javax.swing.SwingUtilities;

import herramienta.config.Configuracio;
import herramienta.i18n.I18n;

public class ConnexioServidor {

	private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ConnexioServidor.class.getName());

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

	private record Migracio(int version, String sql) {}

	private static final Migracio[] MIGRATIONS = {
		new Migracio(1,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS imatge_blob BLOB"),
		new Migracio(2,  "CREATE TABLE IF NOT EXISTS llista (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL)"),
		new Migracio(3,  "CREATE TABLE IF NOT EXISTS llibre_llista (" +
			"isbn BIGINT NOT NULL, llista_id INT NOT NULL, " +
			"valoracio FLOAT DEFAULT 0.0, llegit BOOLEAN DEFAULT FALSE, " +
			"PRIMARY KEY (isbn, llista_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (llista_id) REFERENCES llista(id) ON DELETE CASCADE)"),
		new Migracio(4,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS notes VARCHAR(2048) DEFAULT ''"),
		new Migracio(5,  "ALTER TABLE llista ADD COLUMN IF NOT EXISTS ordre INT DEFAULT 0"),
		new Migracio(6,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_afegit TIMESTAMP DEFAULT CURRENT_TIMESTAMP"),
		new Migracio(7,  "ALTER TABLE llista ADD COLUMN IF NOT EXISTS color VARCHAR(7) DEFAULT NULL"),
		new Migracio(8,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines INT DEFAULT 0"),
		new Migracio(9,  "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pagines_llegides INT DEFAULT 0"),
		new Migracio(10, "CREATE TABLE IF NOT EXISTS prestec (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"nom_persona VARCHAR(255) NOT NULL, " +
			"data_prestec DATE NOT NULL, " +
			"retornat BOOLEAN DEFAULT FALSE, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"),
		new Migracio(11, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS editorial VARCHAR(255) DEFAULT ''"),
		new Migracio(12, "CREATE TABLE IF NOT EXISTS tag (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(100) NOT NULL UNIQUE)"),
		new Migracio(13, "CREATE TABLE IF NOT EXISTS llibre_tag (" +
			"isbn BIGINT NOT NULL, tag_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, tag_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE)"),
		new Migracio(14, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS serie VARCHAR(255) DEFAULT ''"),
		new Migracio(15, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS volum INT DEFAULT 0"),
		new Migracio(16, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_compra DATE DEFAULT NULL"),
		new Migracio(17, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS data_lectura DATE DEFAULT NULL"),
		new Migracio(18, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS idioma VARCHAR(100) DEFAULT NULL"),
		new Migracio(19, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS format VARCHAR(50) DEFAULT NULL"),
		new Migracio(20, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS desitjat BOOLEAN DEFAULT FALSE"),
		new Migracio(21, "CREATE TABLE IF NOT EXISTS autor (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(500) NOT NULL UNIQUE)"),
		new Migracio(22, "CREATE TABLE IF NOT EXISTS llibre_autor (" +
			"isbn BIGINT NOT NULL, autor_id INT NOT NULL, " +
			"PRIMARY KEY (isbn, autor_id), " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE, " +
			"FOREIGN KEY (autor_id) REFERENCES autor(id))"),
		new Migracio(23, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS pais_origen VARCHAR(100) DEFAULT NULL"),
		new Migracio(24, "ALTER TABLE llibre MODIFY COLUMN notes TEXT"),
		new Migracio(25, "CREATE TABLE IF NOT EXISTS lectura (" +
			"id INT AUTO_INCREMENT PRIMARY KEY, " +
			"isbn BIGINT NOT NULL, " +
			"data_inici DATE DEFAULT NULL, " +
			"data_fi DATE DEFAULT NULL, " +
			"pagines_llegides INT DEFAULT 0, " +
			"FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE)"),
		new Migracio(26, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS estat VARCHAR(20) DEFAULT NULL"),
		new Migracio(27, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS exemplars INT DEFAULT 1"),
		new Migracio(28, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS llengua_original VARCHAR(100) DEFAULT NULL"),
		new Migracio(29, "ALTER TABLE llibre MODIFY COLUMN valoracio DOUBLE"),
		new Migracio(30, "ALTER TABLE llibre MODIFY COLUMN preu DOUBLE"),
		new Migracio(31, "ALTER TABLE llibre_llista MODIFY COLUMN valoracio DOUBLE"),
		// Migració 32-34: passa els autors de la columna `llibre.autor` (string)
		// a la taula `llibre_autor` (relació N:M). Risc: si 33 falla parcialment
		// en una BBDD gran (OOM, FK violada, etc.) i 34 corre igual, es perd
		// la informació d'autors dels llibres que no s'han migrat.
		// Les sentències són idempotents (INSERT IGNORE + subselect amb NOT IN),
		// de manera que es poden re-correr manualment si cal — veure
		// `todo.txt` [1] Migration 34. Abans de re-córrer, comprova que
		// `SELECT COUNT(*) FROM llibre WHERE TRIM(autor) IS NOT NULL AND TRIM(autor) != ''`
		// coincideix amb `SELECT COUNT(*) FROM llibre_autor`.
		new Migracio(32, "INSERT IGNORE INTO autor (nom) SELECT DISTINCT TRIM(autor) FROM llibre WHERE TRIM(autor) IS NOT NULL AND TRIM(autor) != '' AND ISBN NOT IN (SELECT isbn FROM llibre_autor)"),
		new Migracio(33, "INSERT IGNORE INTO llibre_autor (isbn, autor_id) SELECT l.ISBN, a.id FROM llibre l JOIN autor a ON a.nom = TRIM(l.autor) WHERE TRIM(l.autor) IS NOT NULL AND TRIM(l.autor) != '' AND l.ISBN NOT IN (SELECT isbn FROM llibre_autor)"),
		new Migracio(34, "ALTER TABLE llibre DROP COLUMN autor"),
		new Migracio(35, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_ca VARCHAR(500) DEFAULT NULL"),
		new Migracio(36, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_es VARCHAR(500) DEFAULT NULL"),
		new Migracio(37, "ALTER TABLE llibre ADD COLUMN IF NOT EXISTS nom_en VARCHAR(500) DEFAULT NULL"),
		new Migracio(38, "CREATE INDEX IF NOT EXISTS idx_llibre_nom ON llibre(nom)"),
		new Migracio(39, "CREATE INDEX IF NOT EXISTS idx_llibre_editorial_serie ON llibre(editorial, serie)"),
	};

	/** Assignada un sol cop dins de {@link #crearDatabase(ConnectionConfig)} o
	 *  {@link #crearDatabase()} i mai reassignada; {@link #tancarConnection()}
	 *  posa el camp a null després de tancar. No assignis una nova Connection en un altre lloc. */
	private Connection con;

	public ConnexioServidor() {}

	public Connection obtenirConnexio() { return con; }

	public static Connection testConnection(java.util.Properties props) throws Exception {
		return testConnection(props, props.getProperty("dbPassword", "").toCharArray());
	}

	/** Variant que pren la contrasenya com a {@code char[]} perquè el consumidor
	 *  la pugui posar a zero després de la crida en lloc de deixar una
	 *  {@code String} a l'heap.
	 *  <p>El contracte JDBC {@link java.sql.Driver#connect(String, java.util.Properties)}
	 *  requereix la contrasenya com a {@code String} dins la {@code Properties},
	 *  de manera que una única còpia de la contrasenya com a {@code String} és
	 *  inevitable per a MariaDB; la còpia viu dins de les {@code Properties}
	 *  de la {@link Connection} fins que el driver l'allibera. El {@code char[]}
	 *  del consumidor és l'única porció que aquest mètode pot mantenir
	 *  curta: posa-la a zero tan aviat com la connexió tingui èxit o falli. */
	public static Connection testConnection(java.util.Properties props, char[] password) throws Exception {
		String dbType = props.getProperty("dbType", "h2");
		ConnexioServidor sc = new ConnexioServidor();
		if ("h2".equals(dbType)) {
			String dir = System.getProperty("user.home") + "/.biblioteca";
			new File(dir).mkdirs();
			String profile = sanitizeH2Profile(props.getProperty("dbProfile", "biblioteca"));
			String url = "jdbc:h2:" + dir.trim().replaceAll("^\\s+|\\s+$", "").replaceAll("/+$", "")
				+ "/" + profile + ";MODE=MySQL;NON_KEYWORDS=VALUE";
			return sc.connectViaDriver("org.h2.Driver", "h2", url, "sa", new char[0]);
		} else {
			String host = props.getProperty("dbHost", "localhost");
			String url = "jdbc:mariadb://" + host + "/?characterEncoding=UTF-8&useUnicode=true";
			return sc.connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
				url, props.getProperty("dbUser", ""), password);
		}
	}

	public void crearDatabase() {
		String testUrl = System.getProperty("biblioteca.h2.url");
		// La fàbrica ConnectionFactory.withConfig() era una indirecta de
		// 14 línies sense política — s'ha inlineat aquí segons el finding
		// LOW de tot.txt.
		ConnectionConfig cfg = new ConnectionConfig(
			testUrl != null ? "h2" : Configuracio.obtenirDbType(),
			Configuracio.obtenirDbHost(), Configuracio.obtenirDbUser(), Configuracio.obtenirDbPassword(),
			Configuracio.obtenirDbProfile(), testUrl);
		crearDatabase(cfg);
	}

	public void crearDatabase(ConnectionConfig cfg) {
		try {
			if (cfg.testUrl() != null) {
				con = connectViaDriver("org.h2.Driver", "h2", cfg.testUrl(), "sa", new char[0]);
			} else if ("h2".equals(cfg.dbType())) {
				String dir = System.getProperty("user.home") + "/.biblioteca";
				new File(dir).mkdirs();
				String url = "jdbc:h2:" + dir.strip().replaceAll("/+$", "")
					+ "/" + sanitizeH2Profile(cfg.profile()) + ";MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
				con = connectViaDriver("org.h2.Driver", "h2", url, "sa", new char[0]);
			} else if ("mariadb".equals(cfg.dbType())) {
				String url = "jdbc:mariadb://" + cfg.host() + "/?characterEncoding=UTF-8&useUnicode=true";
				String cfgPwd = cfg.password();
				char[] pwd = (cfgPwd == null || cfgPwd.isEmpty()) ? new char[0] : cfgPwd.toCharArray();
				con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
					url, cfg.user(), pwd);
				try (Statement s = con.createStatement()) {
					s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
					s.executeUpdate("USE BIBLIOTECA;");
				}
			} else {
				// dbType desconegut / futur. Llança un missatge precís
				// en lloc de caure a I18n.t("err_db_init_" + dbType), que
				// retornaria la clau mateixa per a un tipus desconegut i
				// mostraria a l'usuari un error enganyós "err_db_init_h2"
				// (veure el finding HIGH de tot.txt sobre aquest fitxer).
				throw new IllegalArgumentException("Tipus de BBDD desconegut: " + cfg.dbType()
					+ " (s'esperava 'h2' o 'mariadb')");
			}
			try (Statement s = con.createStatement()) {
				s.executeUpdate(CREATE_TABLE);
			}
			runMigrations();
		} catch (Exception e) {
			// Si la connexió ja s'ha obert però runMigrations (o un pas
			// posterior) falla, tanquem-la per no deixar viu un JDBC
			// Connection que pot retenir el lock de fitxer H2.
			tancarConnection();
			throw new RuntimeException(I18n.t("err_db_connect") + " " + inicialitzarErrorKey(cfg.dbType()), e);
		}
	}

	/** Mapeja un dbType conegut a la clau i18n {@code err_db_init_*} corresponent.
	 *  Llança per a tipus desconeguts perquè el consumidor no acabi fent
	 *  {@code I18n.t("err_db_init_" + dbType)} (que retornaria la clau mateixa
	 *  per a tipus desconeguts, emmascarant l'error real). */
	private static String inicialitzarErrorKey(String dbType) {
		switch (dbType) {
			case "h2":     return I18n.t("err_db_init_h2");
			case "mariadb": return I18n.t("err_db_init_mariadb");
			default: throw new IllegalArgumentException("Tipus de BBDD desconegut: " + dbType);
		}
	}

	/** Rebutja separadors de ruta en noms de perfil de fitxer H2 (jdbc:h2:dir/perfil). */
	static String sanitizeH2Profile(String profile) {
		if (profile == null || profile.isBlank()) return "biblioteca";
		String p = profile.trim();
		if (p.contains("/") || p.contains("\\") || p.contains(".."))
			throw new IllegalArgumentException("Nom de perfil de BD H2 no vàlid: " + profile);
		return p;
	}

	/**
	 * Aplica les migracions d'esquema pendents i registra cada versió completada a
	 * {@code schema_version}. L'estratègia de transacció depèn del motor connectat,
	 * detectat via {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
	 *
	 * <p><b>H2</b> admet DDL transaccional. La sentència DDL i l'INSERT
	 * {@code schema_version} corresponent s'executen dins d'una sola transacció JDBC
	 * ({@code setAutoCommit(false)} → execute → {@code commit}, o
	 * {@code rollback} en cas de fallada). Una fallada a mitja seqüència en H2
	 * deixa l'esquema sense canvis i la migració es reintenta a la propera arrencada.
	 *
	 * <p><b>MariaDB / MySQL</b> no admeten DDL transaccional: cada sentència DDL
	 * fa commit implícit i no es pot desfer. En aquests motors s'omet
	 * intencionadament l'embolcall {@code setAutoCommit(false)} i cada migració
	 * s'aplica com dues operacions atòmiques independents: el DDL (auto-committed)
	 * seguit de l'INSERT {@code schema_version} (la seva pròpia transacció
	 * auto-commit). Si l'INSERT falla es registra i la migració queda sense
	 * enregistrar perquè la propera arrencada la reintent. Si el DDL mateix
	 * falla, la connexió pot tenir un estat parcialment migrat: els operadors
	 * han d'inspeccionar {@code schema_version} (que llista les migracions
	 * enregistrades com aplicades) i l'esquema en viu per determinar quines
	 * sentències es van completar i acabar la migració manualment. Tot el DDL
	 * de {@link #MIGRATIONS} està escrit per ser idempotent (guardes com ara
	 * {@code IF NOT EXISTS}), de manera que un reintent contra un esquema
	 * parcialment migrat és segur.
	 */
	private void runMigrations() throws SQLException {
		// Constructor-only: les migracions comparteixen `con` amb la resta
		// de l'aplicació i el muten (setAutoCommit), per la qual cosa
		// executar-les fora del fil del constructor de ControladorPersistencia
		// podria interferir amb un SwingWorker que estigués fent servir la
		// mateixa connexió.
		assert !SwingUtilities.isEventDispatchThread() : "migrations must not run on EDT";
		boolean esH2 = esH2Connection();
		boolean anteriorAutoCommit = con.getAutoCommit();
		try (Statement st = con.createStatement()) {
			st.executeUpdate(CREATE_SCHEMA_VERSION);
			java.util.Set<Integer> applied = new java.util.HashSet<>();
			try (ResultSet rs = st.executeQuery("SELECT version FROM schema_version")) {
				while (rs.next()) applied.add(rs.getInt(1));
			}
			try (PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)")) {
				for (Migracio m : MIGRATIONS) {
					if (applied.contains(m.version())) continue;
					boolean recorded = esH2
						? runMigrationH2(st, ins, m)
						: runMigrationNonTransactional(st, ins, m);
					if (recorded) applied.add(m.version());
				}
			}
		} finally {
			try { con.setAutoCommit(anteriorAutoCommit); } catch (SQLException ignored) {}
		}
	}

	private boolean esH2Connection() throws SQLException {
		String productName = con.getMetaData().getDatabaseProductName();
		return productName != null && productName.toLowerCase(java.util.Locale.ROOT).startsWith("h2");
	}

	private boolean runMigrationH2(Statement st, PreparedStatement ins, Migracio m) throws SQLException {
		con.setAutoCommit(false);
		try {
			if (m.version() == 34) {
				if (!aplicarMigration34DropAutor(st)) {
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
				LOG.log(java.util.logging.Level.WARNING, "Ha fallat el rollback de la migració " + m.version(), ex);
			}
			throw e;
		} finally {
			con.setAutoCommit(true);
		}
	}

	/**
	 * Stub conservat per claredat — el pla d'atomicitat original 32 → 33 → 34
	 * (veure tot.txt F6) es va rebutjar perquè la seqüència H2 32→33→34
	 * topa amb "Column AUTOR not found" quan 32 es re-executa després d'un
	 * commit parcial previ (un cas límit real que el model de transacció
	 * per-migració existent tolera però la runMigrations3234H2 fusionada
	 * no). Es conserva el model original de tres transaccions; el finding
	 * de tot.txt està documentat però la correcció no s'aplica aquí per
	 * mantenir el comportament de migració idèntic a la versió llargament
	 * publicada.
	 */
	@SuppressWarnings("unused")
	private boolean runMigrations3234H2(Statement st, PreparedStatement ins) throws SQLException {
		throw new UnsupportedOperationException("see comment");
	}

	private boolean runMigrationNonTransactional(Statement st, PreparedStatement ins, Migracio m) throws SQLException {
		if (m.version() == 34) {
			aplicarMigration34DropAutor(st);
		} else {
			st.executeUpdate(m.sql());
		}
		try {
			ins.setInt(1, m.version());
			ins.execute();
			return true;
		} catch (SQLException e) {
			LOG.log(java.util.logging.Level.WARNING, "Ha fallat la inserció de schema_version a la migració " + m.version(), e);
			return false;
		}
	}

	/**
	 * Esborra {@code llibre.autor} només quan cada fila d'autor heretada no buida té un
	 * enllaç {@code llibre_autor} corresponent. Salta (retorna fals) si queden files sense migrar.
	 */
	private boolean aplicarMigration34DropAutor(Statement st) throws SQLException {
		if (!columnExists("llibre", "autor")) return true;
		int unmigrated;
		try (ResultSet rs = st.executeQuery(
				"SELECT COUNT(*) FROM llibre l WHERE TRIM(l.autor) IS NOT NULL AND TRIM(l.autor) != '' " +
				"AND l.ISBN NOT IN (SELECT isbn FROM llibre_autor)")) {
			rs.next();
			unmigrated = rs.getInt(1);
		}
		if (unmigrated > 0) {
			LOG.warning("Migració 34: ometent DROP COLUMN autor — " + unmigrated
				+ " llibre(s) encara no tenen files a llibre_autor (torna a córrer després d'arreglar les dades)");
			return false;
		}
		st.executeUpdate("ALTER TABLE llibre DROP COLUMN autor");
		return true;
	}

	private boolean columnExists(String table, String column) throws SQLException {
		// Fa servir la casing de l'identificador que reporta el JDBC (H2
		// posa en minúscules, MariaDB conserva la mescla) per no haver de
		// fer servir un fallback toUpperCase / mescla escrit a mà. Cercar
		// la columna amb la casing que el motor realment emmagatzema
		// evita el fals negatiu "column not found" en un MariaDB
		// case-sensitive on la consulta de catàleg ha usat la casing
		// incorrecta.
		java.sql.DatabaseMetaData meta = con.getMetaData();
		String catalog = con.getCatalog();
		String lookupTable = table;
		String lookupColumn = column;
		if (meta.storesLowerCaseIdentifiers()) {
			lookupTable = table.toLowerCase(java.util.Locale.ROOT);
			lookupColumn = column.toLowerCase(java.util.Locale.ROOT);
		} else if (meta.storesUpperCaseIdentifiers()) {
			lookupTable = table.toUpperCase(java.util.Locale.ROOT);
			lookupColumn = column.toUpperCase(java.util.Locale.ROOT);
		}
		try (ResultSet rs = meta.getColumns(catalog, null, lookupTable, lookupColumn)) {
			return rs.next();
		}
	}

	private Connection connectViaDriver(String driverClass, String jarNameHint,
			String url, String user, char[] password) throws Exception {
		java.sql.Driver driver;
		try {
			driver = (java.sql.Driver) Class.forName(driverClass).getDeclaredConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			driver = carregarDriverFromLib(driverClass, jarNameHint);
		}
		java.util.Properties props = new java.util.Properties();
		if (user != null) props.setProperty("user", user);
		if (password != null) {
			String pwd = new String(password);
			java.util.Arrays.fill(password, '\0');
			props.setProperty("password", pwd);
		}
		Connection c = driver.connect(url, props);
		if (c == null) throw new Exception("El driver " + driverClass + " no ha acceptat l'URL: " + url);
		return c;
	}

	private java.sql.Driver carregarDriverFromLib(String driverClass, String jarNameHint) throws Exception {
		StringBuilder diag = new StringBuilder();
		File libDir = cercarLibDir(diag);
		if (libDir == null) {
			diag.insert(0, "No s'ha trobat el directori lib/. Registre de cerca:\n");
			throw new ClassNotFoundException(diag.toString());
		}
		File[] jars = libDir.listFiles(
			f -> f.getName().contains(jarNameHint) && f.getName().endsWith(".jar"));
		if (jars == null || jars.length == 0) {
			diag.insert(0, "No s'ha trobat el JAR del driver '" + jarNameHint + "'. Registre de cerca:\n");
			throw new ClassNotFoundException(diag.toString());
		}
		java.net.URLClassLoader cl = new java.net.URLClassLoader(
			new java.net.URL[]{jars[0].toURI().toURL()},
			ClassLoader.getSystemClassLoader());
		return (java.sql.Driver) Class.forName(driverClass, true, cl).getDeclaredConstructor().newInstance();
	}

	private File cercarLibDir(StringBuilder diag) {
		return JarLocator.locate(diag, this::teJars);
	}

	private boolean teJars(File dir) {
		if (!dir.isDirectory()) return false;
		String[] files = dir.list((d, n) -> n.endsWith(".jar"));
		return files != null && files.length > 0;
	}

	public void tancarConnection() {
		if (con == null) return;
		try { con.close(); }
		catch (SQLException e) {
			LOG.log(java.util.logging.Level.WARNING, MessageFormat.format(I18n.t("err_db_close"), e.getMessage()), e);
		} finally {
			con = null;
		}
	}
}
