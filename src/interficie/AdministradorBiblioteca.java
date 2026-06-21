package interficie;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Operacions d'administració de la zona de perill.
 * Sub-interfície separada perquè els consumidors que només necessiten
 * lectura/escriptura (UI, API) no depenguin de les capacitats de
 * còpia/restauració/buidatge. Les declaracions {@code throws} explícites
 * permeten als consumidors tractar les fallades d'I/O i de SQL per
 * separat (en lloc d'un genèric {@code catch (Exception e)} que perd
 * tota la informació de tipus).
 */
public interface AdministradorBiblioteca {
    void copiaSegToSQL(File f) throws IOException, SQLException;
    void restaurarFromSQL(File f) throws IOException, SQLException;
    void netejarAll() throws IOException, SQLException;
}
