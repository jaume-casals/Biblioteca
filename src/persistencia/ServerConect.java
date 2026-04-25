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
import domini.Tag;
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
				String url = "jdbc:h2:" + dir + "/biblioteca;MODE=MySQL;NON_KEYWORDS=VALUE;CACHE_SIZE=8192";
				con = connectViaDriver("org.h2.Driver", "h2", url, "sa", "");
			} else {
				String url = "jdbc:mariadb://" + Config.getDbHost() + "/";
				con = connectViaDriver("org.mariadb.jdbc.Driver", "mariadb-java-client",
					url, Config.getDbUser(), Config.getDbPassword());
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
		java.util.Set<Integer> applied = new java.util.HashSet<>();
		ResultSet rs = st.executeQuery("SELECT version FROM schema_version");
		while (rs.next()) applied.add(rs.getInt(1));
		PreparedStatement ins = con.prepareStatement("INSERT INTO schema_version VALUES (?)");
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
			ResultSet rs = stmt.executeQuery("SELECT ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge, (imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, editorial, serie, volum, data_compra, data_lectura, idioma, format, desitjat, pais_origen FROM llibre");
			ResultSetMetaData md = rs.getMetaData();
			int cols = md.getColumnCount();
			header = new String[cols];
			for (int i = 0; i < cols; i++)
				header[i] = md.getColumnClassName(i + 1);
			while (rs.next()) {
				Llibre l = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6),
					rs.getDouble(7), rs.getBoolean(8), rs.getString(9));
				l.setHasBlob(rs.getBoolean(10));
				l.setNotes(rs.getString(11));
				l.setPagines(rs.getInt(12));
				l.setPaginesLlegides(rs.getInt(13));
				l.setEditorial(rs.getString(14));
				l.setSerie(rs.getString(15));
				l.setVolum(rs.getInt(16));
				l.setDataCompra(rs.getString(17));
				l.setDataLectura(rs.getString(18));
				l.setIdioma(rs.getString(19));
				l.setFormat(rs.getString(20));
				l.setDesitjat(rs.getBoolean(21));
				l.setPaisOrigen(rs.getString(22));
				biblio.add(l);
			}
			// Batch-load autors
			java.util.Map<Long, Llibre> byISBN = new java.util.HashMap<>();
			for (Llibre l : biblio) byISBN.put(l.getISBN(), l);
			ResultSet ars = con.createStatement().executeQuery(
				"SELECT la.isbn, a.nom FROM llibre_autor la JOIN autor a ON la.autor_id = a.id ORDER BY la.isbn, a.nom");
			while (ars.next()) {
				Llibre l = byISBN.get(ars.getLong(1));
				if (l != null) l.getAutors().add(ars.getString(2));
			}
		} catch (SQLException e) {
			new DialogoError("Error al agafar tots els llibres", e).showErrorMessage();
		}
		return biblio;
	}

	public synchronized void afegirLlibre(Llibre llibre) throws SQLException {
		if (llibre == null) return;
		PreparedStatement ps = con.prepareStatement(
			"INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`,`imatge_blob`,`notes`,`pagines`,`pagines_llegides`,`editorial`,`serie`,`volum`,`data_compra`,`data_lectura`,`idioma`,`format`,`desitjat`,`pais_origen`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
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
		ps.setInt(12, llibre.getPagines());
		ps.setInt(13, llibre.getPaginesLlegides());
		ps.setString(14, llibre.getEditorial());
		ps.setString(15, llibre.getSerie());
		ps.setInt(16, llibre.getVolum());
		String dc = llibre.getDataCompra(), dl = llibre.getDataLectura();
		if (dc != null) { try { ps.setDate(17, java.sql.Date.valueOf(dc)); } catch (IllegalArgumentException e) { ps.setNull(17, java.sql.Types.DATE); } }
		else ps.setNull(17, java.sql.Types.DATE);
		if (dl != null) { try { ps.setDate(18, java.sql.Date.valueOf(dl)); } catch (IllegalArgumentException e) { ps.setNull(18, java.sql.Types.DATE); } }
		else ps.setNull(18, java.sql.Types.DATE);
		if (llibre.getIdioma() != null) ps.setString(19, llibre.getIdioma());
		else ps.setNull(19, java.sql.Types.VARCHAR);
		if (llibre.getFormat() != null) ps.setString(20, llibre.getFormat());
		else ps.setNull(20, java.sql.Types.VARCHAR);
		ps.setBoolean(21, llibre.getDesitjat());
		if (llibre.getPaisOrigen() != null) ps.setString(22, llibre.getPaisOrigen());
		else ps.setNull(22, java.sql.Types.VARCHAR);
		ps.execute();
		if (!llibre.getAutors().isEmpty()) syncAutors(llibre.getISBN(), llibre.getAutors());
	}

	private void syncAutors(long isbn, java.util.List<String> autors) throws SQLException {
		PreparedStatement del = con.prepareStatement("DELETE FROM llibre_autor WHERE isbn = ?");
		del.setLong(1, isbn);
		del.execute();
		for (String nom : autors) {
			if (nom == null || nom.isBlank()) continue;
			PreparedStatement ins = con.prepareStatement("INSERT IGNORE INTO autor (nom) VALUES (?)");
			ins.setString(1, nom);
			ins.execute();
			PreparedStatement sel = con.prepareStatement("SELECT id FROM autor WHERE nom = ?");
			sel.setString(1, nom);
			ResultSet rs = sel.executeQuery();
			if (rs.next()) {
				PreparedStatement link = con.prepareStatement(
					"INSERT IGNORE INTO llibre_autor (isbn, autor_id) VALUES (?, ?)");
				link.setLong(1, isbn);
				link.setInt(2, rs.getInt(1));
				link.execute();
			}
		}
	}

	public synchronized void deleteLlibre(long isbn) throws SQLException {
		PreparedStatement ps = con.prepareStatement("DELETE FROM llibre WHERE ISBN = ?");
		ps.setLong(1, isbn);
		ps.execute();
	}

	public synchronized void deleteLlibre(Llibre llibre) throws SQLException {
		if (llibre == null) return;
		deleteLlibre(llibre.getISBN());
	}

	public synchronized void clearAllData() throws SQLException {
		con.createStatement().executeUpdate("DELETE FROM prestec");
		con.createStatement().executeUpdate("DELETE FROM llibre_llista");
		con.createStatement().executeUpdate("DELETE FROM llista");
		con.createStatement().executeUpdate("DELETE FROM llibre_autor");
		con.createStatement().executeUpdate("DELETE FROM llibre_tag");
		con.createStatement().executeUpdate("DELETE FROM tag");
		con.createStatement().executeUpdate("DELETE FROM autor");
		con.createStatement().executeUpdate("DELETE FROM llibre");
	}

	public synchronized long getDbSizeBytes() {
		try {
			String url = con.getMetaData().getURL();
			if (url != null && url.startsWith("jdbc:h2:")) {
				String path = url.replaceFirst("jdbc:h2:", "").replaceAll(";.*", "");
				java.io.File f = new java.io.File(path + ".mv.db");
				return f.exists() ? f.length() : -1;
			}
		} catch (Exception ignored) {}
		return -1;
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
			ResultSet rs = con.createStatement().executeQuery("SELECT id, nom, ordre, color FROM llista ORDER BY ordre, nom");
			while (rs.next()) {
				Llista l = new Llista(rs.getInt(1), rs.getString(2));
				l.setOrdre(rs.getInt(3));
				l.setColor(rs.getString(4));
				llistes.add(l);
			}
		} catch (SQLException e) {
			new DialogoError("Error carregant les llistes", e).showErrorMessage();
		}
		return llistes;
	}

	public synchronized void updateLlistaOrdre(int id, int ordre) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE llista SET ordre = ? WHERE id = ?");
		ps.setInt(1, ordre);
		ps.setInt(2, id);
		ps.execute();
	}

	public synchronized ArrayList<Llibre> getRecentlyAdded(int n) {
		ArrayList<Llibre> llibres = new ArrayList<>();
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge, (imatge_blob IS NOT NULL) AS has_blob, notes, pagines, pagines_llegides, editorial, serie, volum, data_compra, data_lectura, idioma, format, desitjat, pais_origen " +
				"FROM llibre ORDER BY data_afegit DESC LIMIT ?");
			ps.setInt(1, n);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Llibre l = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6),
					rs.getDouble(7), rs.getBoolean(8), rs.getString(9));
				l.setHasBlob(rs.getBoolean(10));
				l.setNotes(rs.getString(11));
				l.setPagines(rs.getInt(12));
				l.setPaginesLlegides(rs.getInt(13));
				l.setEditorial(rs.getString(14));
				l.setSerie(rs.getString(15));
				l.setVolum(rs.getInt(16));
				l.setDataCompra(rs.getString(17));
				l.setDataLectura(rs.getString(18));
				l.setIdioma(rs.getString(19));
				l.setFormat(rs.getString(20));
				l.setDesitjat(rs.getBoolean(21));
				l.setPaisOrigen(rs.getString(22));
				llibres.add(l);
			}
		} catch (SQLException e) {
			new DialogoError("Error carregant els llibres recents", e).showErrorMessage();
		}
		return llibres;
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
				"SELECT l.ISBN, l.nom, l.autor, l.`any`, l.descripcio, ll.valoracio, l.preu, ll.llegit, l.imatge, (l.imatge_blob IS NOT NULL) AS has_blob, l.notes, l.pagines, l.pagines_llegides, l.editorial, l.serie, l.volum, l.data_compra, l.data_lectura, l.idioma, l.format, l.desitjat, l.pais_origen " +
				"FROM llibre l JOIN llibre_llista ll ON l.ISBN = ll.isbn WHERE ll.llista_id = ?");
			ps.setInt(1, llistaId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Llibre lib = new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6), rs.getDouble(7),
					rs.getBoolean(8), rs.getString(9));
				lib.setHasBlob(rs.getBoolean(10));
				lib.setNotes(rs.getString(11));
				lib.setPagines(rs.getInt(12));
				lib.setPaginesLlegides(rs.getInt(13));
				lib.setEditorial(rs.getString(14));
				lib.setSerie(rs.getString(15));
				lib.setVolum(rs.getInt(16));
				lib.setDataCompra(rs.getString(17));
				lib.setDataLectura(rs.getString(18));
				lib.setIdioma(rs.getString(19));
				lib.setFormat(rs.getString(20));
				lib.setDesitjat(rs.getBoolean(21));
				lib.setPaisOrigen(rs.getString(22));
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

	public synchronized void updateLlistaColor(int id, String color) throws SQLException {
		PreparedStatement ps = con.prepareStatement("UPDATE llista SET color = ? WHERE id = ?");
		if (color == null) ps.setNull(1, java.sql.Types.VARCHAR); else ps.setString(1, color);
		ps.setInt(2, id);
		ps.execute();
	}

	public synchronized byte[] getLlibreBlob(long isbn) {
		try {
			PreparedStatement ps = con.prepareStatement("SELECT imatge_blob FROM llibre WHERE ISBN = ?");
			ps.setLong(1, isbn);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getBytes(1);
		} catch (SQLException e) {
			new DialogoError("Error carregant la imatge del llibre", e).showErrorMessage();
		}
		return null;
	}

	public synchronized void addPrestec(long isbn, String nom) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"INSERT INTO prestec (isbn, nom_persona, data_prestec, retornat) VALUES (?, ?, CURRENT_DATE, FALSE)");
		ps.setLong(1, isbn);
		ps.setString(2, nom);
		ps.execute();
	}

	public synchronized void returnPrestec(long isbn) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"UPDATE prestec SET retornat = TRUE WHERE isbn = ? AND retornat = FALSE");
		ps.setLong(1, isbn);
		ps.execute();
	}

	public synchronized java.util.List<Object[]> getAllPrestecs() {
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT isbn, nom_persona, data_prestec, retornat FROM prestec ORDER BY id");
			while (rs.next())
				rows.add(new Object[]{ rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4) });
		} catch (SQLException e) {
			new DialogoError("Error carregant els préstecs", e).showErrorMessage();
		}
		return rows;
	}

	public synchronized java.util.Set<Long> getLoanedISBNs() {
		java.util.Set<Long> set = new java.util.HashSet<>();
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT DISTINCT isbn FROM prestec WHERE retornat = FALSE");
			while (rs.next()) set.add(rs.getLong(1));
		} catch (SQLException e) {
			new DialogoError("Error carregant els préstecs", e).showErrorMessage();
		}
		return set;
	}

	public synchronized ArrayList<Tag> getAllTags() {
		ArrayList<Tag> tags = new ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery("SELECT id, nom FROM tag ORDER BY nom");
			while (rs.next()) tags.add(new Tag(rs.getInt(1), rs.getString(2)));
		} catch (SQLException e) {
			new DialogoError("Error carregant les etiquetes", e).showErrorMessage();
		}
		return tags;
	}

	public synchronized int createTag(String nom) throws SQLException {
		PreparedStatement ps = con.prepareStatement("INSERT INTO tag (nom) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
		ps.setString(1, nom);
		ps.execute();
		ResultSet rs = ps.getGeneratedKeys();
		rs.next();
		return rs.getInt(1);
	}

	public synchronized void deleteTag(int id) throws SQLException {
		PreparedStatement ps = con.prepareStatement("DELETE FROM tag WHERE id = ?");
		ps.setInt(1, id);
		ps.execute();
	}

	public synchronized ArrayList<Tag> getTagsForLlibre(long isbn) {
		ArrayList<Tag> tags = new ArrayList<>();
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT t.id, t.nom FROM tag t JOIN llibre_tag lt ON t.id = lt.tag_id WHERE lt.isbn = ? ORDER BY t.nom");
			ps.setLong(1, isbn);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) tags.add(new Tag(rs.getInt(1), rs.getString(2)));
		} catch (SQLException e) {
			new DialogoError("Error carregant les etiquetes del llibre", e).showErrorMessage();
		}
		return tags;
	}

	public synchronized void addLlibreToTag(long isbn, int tagId) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"INSERT INTO llibre_tag (isbn, tag_id) VALUES (?, ?)");
		ps.setLong(1, isbn);
		ps.setInt(2, tagId);
		ps.execute();
	}

	public synchronized void removeLlibreFromTag(long isbn, int tagId) throws SQLException {
		PreparedStatement ps = con.prepareStatement(
			"DELETE FROM llibre_tag WHERE isbn = ? AND tag_id = ?");
		ps.setLong(1, isbn);
		ps.setInt(2, tagId);
		ps.execute();
	}

	public synchronized java.util.List<Object[]> getAllLlibreTag() {
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT isbn, tag_id FROM llibre_tag ORDER BY tag_id, isbn");
			while (rs.next())
				rows.add(new Object[]{ rs.getLong(1), rs.getInt(2) });
		} catch (SQLException e) {
			new DialogoError("Error carregant les dades d'etiquetes", e).showErrorMessage();
		}
		return rows;
	}

	public synchronized java.util.List<Object[]> getAllAutors() {
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery("SELECT id, nom FROM autor ORDER BY id");
			while (rs.next()) rows.add(new Object[]{ rs.getInt(1), rs.getString(2) });
		} catch (SQLException e) {
			new DialogoError("Error carregant els autors", e).showErrorMessage();
		}
		return rows;
	}

	public synchronized java.util.List<Object[]> getAllLlibreAutor() {
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT isbn, autor_id FROM llibre_autor ORDER BY isbn, autor_id");
			while (rs.next()) rows.add(new Object[]{ rs.getLong(1), rs.getInt(2) });
		} catch (SQLException e) {
			new DialogoError("Error carregant els autors dels llibres", e).showErrorMessage();
		}
		return rows;
	}

	public synchronized java.util.Set<Long> getLlibresWithTag(int tagId) {
		java.util.Set<Long> isbns = new java.util.HashSet<>();
		try {
			PreparedStatement ps = con.prepareStatement(
				"SELECT isbn FROM llibre_tag WHERE tag_id = ?");
			ps.setInt(1, tagId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) isbns.add(rs.getLong(1));
		} catch (SQLException e) {
			new DialogoError("Error carregant els llibres de l'etiqueta", e).showErrorMessage();
		}
		return isbns;
	}

	private static final java.util.Set<String> AUTOCOMPLETE_COLUMNS = new java.util.HashSet<>(
		java.util.Arrays.asList("editorial", "serie", "idioma", "pais_origen"));

	public synchronized java.util.List<String> getDistinctValues(String column) {
		java.util.List<String> vals = new java.util.ArrayList<>();
		if (!AUTOCOMPLETE_COLUMNS.contains(column)) return vals;
		try {
			ResultSet rs = con.createStatement().executeQuery(
				"SELECT DISTINCT `" + column + "` FROM llibre WHERE `" + column + "` IS NOT NULL AND `" + column + "` <> '' ORDER BY `" + column + "`");
			while (rs.next()) vals.add(rs.getString(1));
		} catch (SQLException e) {
			new DialogoError("Error carregant valors de " + column, e).showErrorMessage();
		}
		return vals;
	}

	public synchronized java.util.List<String> getDistinctAutorNames() {
		java.util.List<String> vals = new java.util.ArrayList<>();
		try {
			ResultSet rs = con.createStatement().executeQuery("SELECT nom FROM autor ORDER BY nom");
			while (rs.next()) vals.add(rs.getString(1));
		} catch (SQLException e) {
			new DialogoError("Error carregant autors", e).showErrorMessage();
		}
		return vals;
	}

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			new DialogoError("Fallo al tancar la connexió", e).showErrorMessage();
		}
	}
}
