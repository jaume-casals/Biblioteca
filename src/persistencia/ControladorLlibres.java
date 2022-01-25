
package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import domini.Llibre;

public class ControladorLlibres {

	private Connection con;
	private List<Llibre> biblio;

	public ControladorLlibres(Connection con, List<Llibre> biblio) {
		this.con = con;
		this.biblio = biblio;
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
				biblio.add(llibre);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		organitzarLlibre();
	}

	public void deleteLlibre(Llibre llibre) {
		if (llibre != null) {
			try {
				String query = "DELETE FROM `biblioteca`.`llibre` WHERE (`ISBN` = ?);";

				PreparedStatement preparedStmt = con.prepareStatement(query);
				preparedStmt.setInt(1, llibre.getISBN());

				preparedStmt.execute();
				biblio.remove(llibre);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		organitzarLlibre();
	}

	public Llibre buscarLlibre(Integer ISBN) {

		int i = 0;
		while (i < biblio.size()) {

			if (biblio.get(i).getISBN() == ISBN)
				return biblio.get(i);

			i++;
		}
		organitzarLlibre();
		return null;
	}

	public void organitzarLlibre() {
		Collections.sort(biblio);
	}
}
