package persistencia;

import java.sql.*;
import java.util.LinkedList;
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
 			//AIXÒ NOMÉS FUNCIONA SI TENS L'USUARI USER AMB PERMISOS I SENSE CONTRASENYA
			con = DriverManager.getConnection("jdbc:mysql://localhost/?zeroDateTimeBehavior=convertToNull&user=user");
			Statement s = con.createStatement();
			s.executeUpdate("DROP DATABASE IF EXISTS `BIBLIOTECA`;"); //TODO: ELIMINAR AL FINAL
			s.executeUpdate("CREATE DATABASE IF NOT EXISTS BIBLIOTECA;");
			s.executeUpdate("USE BIBLIOTECA;");
			s.executeUpdate("CREATE TABLE IF NOT EXISTS llibre(" +
					 		"ISBN INT PRIMARY KEY, " +
							"nom VARCHAR(255) NOT NULL, " +
							"autor VARCHAR(255), " +
							"any DATE, " +
							"descripcio VARCHAR(512), " +
							"valoracio INT, " +
							"preu FLOAT, " +
							"LLEGIT BOOLEAN, " + 
							"imatge VARCHAR(255)" +
							");");
			System.out.println("no vull ser");

		} catch (SQLException e) {
			System.out.println("Ha fallat la creació de la base de dades");
			e.printStackTrace();
		}
	}

	public void startConection() {
		try {
			con = DriverManager.getConnection
			("jdbc:mysql://localhost:3306/BIBLIOTECA?zeroDateTimeBehavior=convertToNull");

		} catch (SQLException e) {
			System.out.println("Fallo en la connexió amb la base de dades");
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
						rs.getDouble(6), rs.getDouble(7), rs.getBoolean(8)));
		} catch (SQLException e) {
			System.out.println("Fallo al agafar tots els llibres");
			e.printStackTrace();
		}

		return biblio;

	}

	public void crearLlibre() {
		String query = "CREATE TABLE `biblioteca`.`llibre` (`ISBN` INT NOT NULL, `NOM` VARCHAR(255) NOT NULL, `Autor` VARCHAR(255) NOT NULL, `Any` INT NOT NULL, `Descripcio` TEXT NULL, `Valoracio` DOUBLE NULL, `Preu` DOUBLE NULL, `Llegit` TINYINT NULL,PRIMARY KEY (`ISBN`), UNIQUE INDEX `ISBN_UNIQUE` (`ISBN` ASC) VISIBLE);";
		try {
			PreparedStatement preparedStmt = con.prepareStatement(query);
			preparedStmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void afegirLlibre(Llibre llibre) {
		String query = " INSERT INTO `biblioteca`.`llibre` (`ISBN`, `NOM`, `Autor`, `Any`, `Descripcio`, `Valoracio`, `Preu`, `Llegit`) VALUES"
				+ " (?, ?, ?, ? ,? ,? ,? ,? )";

		if (llibre != null) {
			try {
				PreparedStatement preparedStmt = con.prepareStatement(query);

				preparedStmt.setInt(1, llibre.getISBN());
				preparedStmt.setString(2, llibre.getNom());
				preparedStmt.setString(3, llibre.getAutor());
				preparedStmt.setInt(4, llibre.getAny());
				preparedStmt.setString(5, llibre.getDescripcio());
				preparedStmt.setDouble(6, llibre.getValoracio());
				preparedStmt.setDouble(7, llibre.getPreu());
				preparedStmt.setBoolean(8, llibre.getLlegit());

				preparedStmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void deleteLlibre(Llibre llibre) {
		if (llibre != null) {
			try {
				String query = "DELETE FROM `biblioteca`.`llibre` WHERE (`ISBN` = ?);";

				PreparedStatement preparedStmt = con.prepareStatement(query);
				preparedStmt.setInt(1, llibre.getISBN());

				preparedStmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
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