package persistencia;

import java.sql.*;
import java.util.ArrayList;
import domini.Llibre;

public class ServerConect {

	private Connection con;

	private String header[];

	public ServerConect() {
	}

	public Connection getConnection() {
		return con;
	}

	public String[] getHeader() {
		return header;
	}

	public void createDatabase() {
		try {
//			Class.forName("com.mysql.cj.jdbc.Driver"); // innecessari
			// AIXÒ NOMÉS FUNCIONA SI TENS L'USUARI USER AMB PERMISOS I SENSE CONTRASENYA
			con = DriverManager.getConnection("jdbc:mysql://localhost/?zeroDateTimeBehavior=convertToNull&user=user");
		} catch (SQLException e) {
			System.out.println("Ha fallat la connexió a la base de dades");
			e.printStackTrace();
		}

		try {
			Statement s = con.createStatement();
			s.executeUpdate("DROP DATABASE IF EXISTS `BIBLIOTECA`;"); // TODO: ELIMINAR AL FINAL
			s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
			s.executeUpdate("USE BIBLIOTECA;");
			s.executeUpdate("CREATE TABLE IF NOT EXISTS llibre(" + "ISBN INT PRIMARY KEY, "
					+ "nom VARCHAR(255) NOT NULL, " + "autor VARCHAR(255), " + "any INT, " + "descripcio VARCHAR(512), "
					+ "valoracio FLOAT, " + "preu FLOAT, " + "llegit BOOLEAN, " + "imatge VARCHAR(255)" + ");");
		} catch (SQLException e) {
			System.out.println("Ha fallat la creació de la base de dades");
			e.printStackTrace();
		}

		try {
			PreparedStatement ps = con.prepareStatement("insert into llibre values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
			for (int i = 0; i < 7; ++i) {
				ps.setInt(1, i + 10000);
				ps.setString(2, (int) (7 / (7 - i)) + "Titol");
				ps.setString(3, (int) (i % 2) + "Autor");
				ps.setInt(4, i * 1000);
				ps.setString(5, "a");
				ps.setFloat(6, (float) (2.0 + i / 100.0));
				ps.setFloat(7, (float) (20.0 + i / 100.0));
				ps.setBoolean(8, i % 2 == 0);
				ps.setString(9, "arg1");

				ps.executeUpdate();
			}
			ps.close();
		} catch (Exception e) {
			System.out.println("Ha fallat omplir la base de dades");
			e.printStackTrace();
		}

	}

	public ArrayList<Llibre> getAllLlibres() {
		ArrayList<Llibre> biblio = new ArrayList<Llibre>();

		try {
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select * from llibre");
			ResultSetMetaData md = (ResultSetMetaData) rs.getMetaData();

			header = new String[md.getColumnCount() + 1];
			for (int i = 1; i < md.getColumnCount() - 1; i++) {
				header[i] = md.getColumnClassName(i);
			}

			while (rs.next())
				biblio.add(new Llibre(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5),
						rs.getDouble(6), rs.getDouble(7), rs.getBoolean(8), rs.getString(9)));
		} catch (SQLException e) {
			System.out.println("Error al agafar tots els llibres");
			e.printStackTrace();
		}

		return biblio;

	}

	public void afegirLlibre(Llibre llibre) {
		String query = " INSERT INTO llibre (`ISBN`, `nom`, `autor`, `any`, `descripcio`, `valoracio`, `preu`, `llegit`, `imatge`) VALUES"
				+ " (?, ?, ?, ? ,? ,? ,? ,?, ? )";
		if (llibre != null) {
			try {
				PreparedStatement preparedStmt = con.prepareStatement(query);

				preparedStmt.setInt(1, llibre.getISBN());

				preparedStmt.setString(2, llibre.getNom());

				if (llibre.getAutor() != null)
					preparedStmt.setString(3, llibre.getAutor());
				else
					preparedStmt.setString(3, "");

				if (llibre.getAny() != null)
					preparedStmt.setInt(4, llibre.getAny());
				else
					preparedStmt.setInt(4, 0);

				if (llibre.getDescripcio() != null)
					preparedStmt.setString(5, llibre.getDescripcio());
				else
					preparedStmt.setString(5, "");

				if (llibre.getValoracio() != null)
					preparedStmt.setDouble(6, llibre.getValoracio());
				else
					preparedStmt.setDouble(6, 0.0);

				if (llibre.getPreu() != null)
					preparedStmt.setDouble(7, llibre.getPreu());
				else
					preparedStmt.setDouble(7, 0.0);

				if (llibre.getLlegit() != null)
					preparedStmt.setBoolean(8, llibre.getLlegit());
				else
					preparedStmt.setBoolean(8, false);

				if (llibre.getPortada() != null)
					preparedStmt.setString(9, llibre.getPortada());
				else
					preparedStmt.setString(9, "portades/default_cover.png");

				preparedStmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void deleteLlibre(Llibre llibre) {
		if (llibre != null) {
			try {
				String query = "DELETE FROM llibre WHERE (`ISBN` = ?);";

				PreparedStatement preparedStmt = con.prepareStatement(query);
				preparedStmt.setInt(1, llibre.getISBN());

				preparedStmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void resetDatabase() {
		try {
			Statement s = con.createStatement();
			s.executeUpdate("DROP DATABASE IF EXISTS `BIBLIOTECA`;");
			s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
			s.executeUpdate("USE BIBLIOTECA;");
			s.executeUpdate("CREATE TABLE IF NOT EXISTS llibre(" + "ISBN INT PRIMARY KEY, "
					+ "nom VARCHAR(255) NOT NULL, " + "autor VARCHAR(255), " + "any INT, " + "descripcio VARCHAR(512), "
					+ "valoracio FLOAT, " + "preu FLOAT, " + "llegit BOOLEAN, " + "imatge VARCHAR(255)" + ");");
		} catch (SQLException e) {
			System.out.println("Error reiniciant la base de dades");
			e.printStackTrace();
		}
	}

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			System.out.println("Fallo al tancar la connexió");
			e.printStackTrace();
		}
	}
}