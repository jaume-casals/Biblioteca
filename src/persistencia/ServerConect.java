package persistencia;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import domini.Llibre;
import domini.Llista;
import herramienta.Config;
import herramienta.DialogoError;

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
	};

	private Connection con;
	private String[] header;

	public ServerConect() {}

	public Connection getConnection() { return con; }
	public String[] getHeader()       { return header; }

	public void createDatabase() {
		try {
			String testUrl = System.getProperty("biblioteca.h2.url");
			if (testUrl != null) {
				con = connectViaDriver("org.h2.Driver", "h2", testUrl, "sa", "");
			} else if ("h2".equals(Config.getDbType())) {
				String dir = System.getProperty("user.home") + "/.biblioteca";
				new File(dir).mkdirs();
				String url = "jdbc:h2:" + dir + "/biblioteca;MODE=MySQL;NON_KEYWORDS=VALUE";
				con = connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
			} else {
				String pw  = Config.getDbPassword();
				String url = "jdbc:mariadb://" + Config.getDbHost() + "/?user=" + Config.getDbUser()
					+ (pw.isEmpty() ? "" : "&password=" + pw);
				con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client", url, null, null);
				Statement s = con.createStatement();
				s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
				s.executeUpdate("USE BIBLIOTECA;");
			}
			con.createStatement().executeUpdate(CREATE_TABLE);
			runMigrations();
		} catch (Exception e) {
			if (Boolean.getBoolean("biblioteca.test")) throw new RuntimeException(e);
			String hint = "h2".equals(Config.getDbType())
				? "Error inicialitzant la base de dades integrada."
				: "Comprova que MariaDB/MySQL estigui en execució i que l'usuari '"
					+ Config.getDbUser() + "' tingui permisos.";
			new DialogoError("No s'ha pogut connectar a la base de dades.\n" + hint, e)
				.showErrorMessage();
			System.exit(1);
		}
	}

	private void runMigrations() throws SQLException {
		Statement st = con.createStatement();
		st.executeUpdate(CREATE_SCHEMA_VERSION);
		ResultSet rs = st.executeQuery("SELECT MAX(version) FROM schema_version");
		int current = rs.next() ? rs.getInt(1) : 0;
		PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)");
		for (String[] m : MIGRATIONS) {
			int v = Integer.parseInt(m[0]);
			if (v > current) {
				st.executeUpdate(m[1]);
				ins.setInt(1, v);
				ins.execute();
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

	public synchronized ArrayList<Llibre> getAllLlibres() {
		ArrayList<Llibre> biblio = new ArrayList<>();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge, imatge_blob, notes FROM llibre");
			ResultSetMetaData md = rs.getMetaData();
			int cols = md.getColumnCount();
			header = new String[cols];
			for (int i = 0; i < cols; i++)
				header[i] = md.getColumnClassName(i + 1);
			while (rs.next()) {
				Llibre l = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6),
					rs.getDouble(7), rs.getBoolean(8), rs.getString(9));
				l.setImatgeBlob(rs.getBytes(10));
				l.setNotes(rs.getString(11));
				biblio.add(l);
			}
		} catch (SQLException e) {
			new DialogoError("Error al agafar tots els llibres", e).showErrorMessage();
		}
		return biblio;
	}

	public synchronized void afegirLlibre(Llibre llibre) throws SQLException {
		if (llibre == null) return;
		PreparedStatement ps = con.prepareStatement(
			"INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`imatge_blob`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
		ps.setLong(1, llibre.getISBN());
		ps.setString(2, llibre.getNom());
		ps.setString(3, llibre.getAutor() != null ? llibre.getAutor() : "");
		ps.setInt(4, llibre.getAny() != null ? llibre.getAny() : 0);
		ps.setString(5, llibre.getDescripcio() != null ? llibre.getDescripcio() : "");
		ps.setDouble(6, llibre.getValoracio() != null ? llibre.getValoracio() : 0.0);
		ps.setDouble(7, llibre.getPreu() != null ? llibre.getPreu() : 0.0);
		ps.setBoolean(8, Boolean.TRUE.equals(llibre.getLlegit()));
		ps.setString(9, llibre.getImatge() != null ? llibre.getImatge() : "");
		ps.setBytes(10, llibre.getImatgeBlob());
		ps.setString(11, llibre.getNotes());
		ps.execute();
	}

	public synchronized void deleteLlibre(Llibre llibre) throws SQLException {
		if (llibre == null) return;
		PreparedStatement ps = con.prepareStatement("DELETE FROM llibre WHERE ISBN = ?");
		ps.setLong(1, llibre.getISBN());
		ps.execute();
	}

	public synchronized void clearAllData() throws SQLException {
		con.createStatement().executeUpdate("DELETE FROM llibre_llista");
		con.createStatement().executeUpdate("DELETE FROM llista");
		con.createStatement().executeUpdate("DELETE FROM llibre");
	}

	public synchronized void resetDatabase() {
		try {
			Statement s = con.createStatement();
			if ("h2".equals(Config.getDbType())) {
				s.executeUpdate("DROP TABLE IF EXISTS llibre");
			} else {
				s.executeUpdate("DROP DATABASE IF EXISTS `BIBLIOTECA`");
				s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA");
				s.executeUpdate("USE BIBLIOTECA");
			}
			s.executeUpdate(CREATE_TABLE);
		} catch (SQLException e) {
			new DialogoError("Error reiniciant la base de dades", e).showErrorMessage();
		}
	}

	public synchronized void executeSQLFile(java.io.File file) throws Exception {
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
			StringBuilder stmt = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("--")) continue;
				String upper = line.toUpperCase();
				// skip server-specific statements not applicable to embedded or already-scoped connections
				if (upper.startsWith("USE ") || upper.startsWith("CREATE DATABASE")
						|| upper.startsWith("DROP DATABASE")) continue;
				stmt.append(line).append(" ");
				if (line.endsWith(";")) {
					con.createStatement().execute(stmt.toString().trim());
					stmt = new StringBuilder();
				}
			}
		}
	}

	public synchronized ArrayList<Llista> getAllLlistes() {
		ArrayList<Llista> llistes = new ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery("SELECT id, nom FROM llista ORDER BY nom");
			while (rs.next()) llistes.add(new Llista(rs.getInt(1), rs.getString(2)));
		} catch (SQLException e) {
			new DialogoError("Error carregant les llistes", e).showErrorMessage();
		}
		return llistes;
	}

	public synchronized java.util.List<Object[]> getAllLlibreLlista() {
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT isbn, llista_id, valoracio, llegit FROM llibre_llista ORDER BY llista_id, isbn");
			while (rs.next())
				rows.add(new Object[]{ rs.getLong(1), rs.getInt(2), rs.getDouble(3), rs.getBoolean(4) });
		} catch (SQLException e) {
			new DialogoError("Error carregant les dades de llista", e).showErrorMessage();
		}
		return rows;
	}

	public synchronized int createLlista(String nom) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO llista (nom) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, nom);
		ps.execute();
		ResultSet rs = ps.getGeneratedKeys();
		rs.next();
		return rs.getInt(1);
	}

	public synchronized void deleteLlista(int id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("DELETE FROM llista WHERE id = ?");
		ps.setInt(1, id);
		ps.execute();
	}

	public synchronized int getCountInLlista(int llistaId) {
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT COUNT(*) FROM llibre_llista WHERE llista_id = ?");
			ps.setInt(1, llistaId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getInt(1);
		} catch (SQLException e) {
			new DialogoError("Error comptant els llibres de la llista", e).showErrorMessage();
		}
		return 0;
	}

	public synchronized ArrayList<Llibre> getLlibresInLlista(int llistaId) {
		ArrayList<Llibre> llibres = new ArrayList<>();
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT l.ISBN, l.nom, l.autor, l.`any`, l.descripcio, ll.valoracio, l.preu, ll.llegit, l.imatge, l.imatge_blob, l.notes " +
				"FROM llibre l JOIN llibre_llista ll ON l.ISBN = ll.isbn WHERE ll.llista_id = ?");
			ps.setInt(1, llistaId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Llibre lib = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6), rs.getDouble(7),
					rs.getBoolean(8), rs.getString(9));
				lib.setImatgeBlob(rs.getBytes(10));
				lib.setNotes(rs.getString(11));
				llibres.add(lib);
			}
		} catch (SQLException e) {
			new DialogoError("Error carregant els llibres de la llista", e).showErrorMessage();
		}
		return llibres;
	}

	public synchronized void addLlibreToLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"INSERT INTO llibre_llista (isbn, llista_id, valoracio, llegit) VALUES (?, ?, ?, ?)");
		ps.setLong(1, isbn);
		ps.setInt(2, llistaId);
		ps.setDouble(3, valoracio);
		ps.setBoolean(4, llegit);
		ps.execute();
	}

	public synchronized void removeLlibreFromLlista(long isbn, int llistaId) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"DELETE FROM llibre_llista WHERE isbn = ? AND llista_id = ?");
		ps.setLong(1, isbn);
		ps.setInt(2, llistaId);
		ps.execute();
	}

	public synchronized void updateLlibreInLlista(long isbn, int llistaId, double valoracio, boolean llegit) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"UPDATE llibre_llista SET valoracio = ?, llegit = ? WHERE isbn = ? AND llista_id = ?");
		ps.setDouble(1, valoracio);
		ps.setBoolean(2, llegit);
		ps.setLong(3, isbn);
		ps.setInt(4, llistaId);
		ps.execute();
	}

	public synchronized ArrayList<Llista> getLlistesForLlibre(long isbn) {
		ArrayList<Llista> llistes = new ArrayList<>();
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT l.id, l.nom, ll.valoracio, ll.llegit FROM llista l " +
				"JOIN llibre_llista ll ON l.id = ll.llista_id WHERE ll.isbn = ? ORDER BY l.nom");
			ps.setLong(1, isbn);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Llista llista = new Llista(rs.getInt(1), rs.getString(2));
				llista.setValoracioLlibre(rs.getDouble(3));
				llista.setLlegitLlibre(rs.getBoolean(4));
				llistes.add(llista);
			}
		} catch (SQLException e) {
			new DialogoError("Error carregant les llistes del llibre", e).showErrorMessage();
		}
		return llistes;
	}

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			new DialogoError("Fallo al tancar la connexió", e).showErrorMessage();
		}
	}
}
