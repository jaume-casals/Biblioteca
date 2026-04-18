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
		"imatge VARCHAR(255)" +
		");";

	private Connection con;
	private String[] header;

	public ServerConect() {}

	public Connection getConnection() { return con; }
	public String[] getHeader()       { return header; }

	public void createDatabase() {
		try {
			String dbType = Config.getDbType();
			if ("h2".equals(dbType)) {
				// Embedded H2 — no server required
				String dir = System.getProperty("user.home") + "/.biblioteca";
				new File(dir).mkdirs();
				con = DriverManager.getConnection(
					"jdbc:h2:" + dir + "/biblioteca;MODE=MySQL;NON_KEYWORDS=VALUE",
					"sa", "");
			} else {
				// External MariaDB / MySQL
				String pw  = Config.getDbPassword();
				String url = "jdbc:mariadb://" + Config.getDbHost() + "/?user=" + Config.getDbUser()
					+ (pw.isEmpty() ? "" : "&password=" + pw);
				con = DriverManager.getConnection(url);
				Statement s = con.createStatement();
				s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
				s.executeUpdate("USE BIBLIOTECA;");
			}
			con.createStatement().executeUpdate(CREATE_TABLE);
		} catch (Exception e) {
			String hint = "h2".equals(Config.getDbType())
				? "Error inicialitzant la base de dades integrada."
				: "Comprova que MariaDB/MySQL estigui en execució i que l'usuari '"
					+ Config.getDbUser() + "' tingui permisos.";
			new DialogoError("No s'ha pogut connectar a la base de dades.\n" + hint, e)
				.showErrorMessage();
			System.exit(1);
		}
	}

	public ArrayList<Llibre> getAllLlibres() {
		ArrayList<Llibre> biblio = new ArrayList<>();
		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT ISBN, nom, autor, `any`, descripcio, valoracio, preu, llegit, imatge FROM llibre");
			ResultSetMetaData md = rs.getMetaData();
			header = new String[md.getColumnCount() + 1];
			for (int i = 1; i < md.getColumnCount() - 1; i++)
				header[i] = md.getColumnClassName(i);
			while (rs.next())
				biblio.add(new Llibre(rs.getLong(1), rs.getString(2), rs.getString(3),
					rs.getInt(4), rs.getString(5), rs.getDouble(6),
					rs.getDouble(7), rs.getBoolean(8), rs.getString(9)));
		} catch (SQLException e) {
			new DialogoError("Error al agafar tots els llibres", e).showErrorMessage();
		}
		return biblio;
	}

	public void afegirLlibre(Llibre llibre) {
		if (llibre == null) return;
		String query = "INSERT INTO llibre (`ISBN`,`nom`,`autor`,`any`,`descripcio`,`valoracio`,`preu`,`llegit`,`imatge`) VALUES (?,?,?,?,?,?,?,?,?)";
		try {
			PreparedStatement ps = con.prepareStatement(query);
			ps.setLong(1, llibre.getISBN());
			ps.setString(2, llibre.getNom());
			ps.setString(3, llibre.getAutor() != null ? llibre.getAutor() : "");
			ps.setInt(4, llibre.getAny() != null ? llibre.getAny() : 0);
			ps.setString(5, llibre.getDescripcio() != null ? llibre.getDescripcio() : "");
			ps.setDouble(6, llibre.getValoracio() != null ? llibre.getValoracio() : 0.0);
			ps.setDouble(7, llibre.getPreu() != null ? llibre.getPreu() : 0.0);
			ps.setBoolean(8, Boolean.TRUE.equals(llibre.getLlegit()));
			ps.setString(9, llibre.getImatge() != null ? llibre.getImatge() : "");
			ps.execute();
		} catch (SQLException e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public void deleteLlibre(Llibre llibre) {
		if (llibre == null) return;
		try {
			PreparedStatement ps = con.prepareStatement("DELETE FROM llibre WHERE ISBN = ?");
			ps.setLong(1, llibre.getISBN());
			ps.execute();
		} catch (SQLException e) {
			new DialogoError(e).showErrorMessage();
		}
	}

	public void resetDatabase() {
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

	public void executeSQLFile(java.io.File file) throws Exception {
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

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			new DialogoError("Fallo al tancar la connexió", e).showErrorMessage();
		}
	}
}
