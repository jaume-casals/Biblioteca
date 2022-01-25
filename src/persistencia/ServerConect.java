package persistencia;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

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

	public void startConection() {
		try {

			Class.forName("com.mysql.cj.jdbc.Driver");

			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/BIBLIOTECA", "mama", "mama");

		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	public List<Llibre> getAllLlibres() {
		List<Llibre> biblio = new LinkedList<Llibre>();

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
			e.printStackTrace();
		}

		return biblio;

	}

	public void closeConection() {
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}