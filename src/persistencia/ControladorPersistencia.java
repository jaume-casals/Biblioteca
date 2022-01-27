
package persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import domini.Llibre;

public class ControladorPersistencia {

	private static ControladorPersistencia inst;
	private ServerConect sc;

	public static ControladorPersistencia getInstance() {
		if (ControladorPersistencia.inst == null) ControladorPersistencia.inst = new ControladorPersistencia();
		return ControladorPersistencia.inst;
	}

	public ControladorPersistencia() {
		sc = new ServerConect();
		sc.createDatabase();
		sc.closeConection();
	}

}
