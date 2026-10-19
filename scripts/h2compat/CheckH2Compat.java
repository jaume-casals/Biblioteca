package h2compat;

import domini.Llibre;
import persistencia.internal.ControladorPersistencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Eina de compatibilitat de H2.
 *
 * <p>Es fa servir dues vegades amb versions diferents del driver H2:
 *
 * <ul>
 *   <li>{@code create <ruta>} crea la base de dades amb el {@code INSERT}
 *       d'un llibre i les claus foranes, amb la versió antiga del driver.</li>
 *   <li>{@code verify <ruta>} obre la mateixa base de dades amb la versió
 *       nova i comprova que les dades i l'esquema segueixen intactes.</li>
 * </ul>
 *
 * <p>El fitxer es crea en un directori temporal: la base de dades real de
 * l'usuari no s'ha de tocar mai.
 */
public class CheckH2Compat {

    private static final long ISBN = 9780306406157L;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Ús: CheckH2Compat create|verify <ruta sense extension>");
            System.exit(2);
        }
        String mode = args[0];
        String ruta = args[1];
        String url = "jdbc:h2:" + ruta + ";MODE=MySQL;NON_KEYWORDS=VALUE";
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url", url);

        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        if ("create".equals(mode)) {
            Llibre l = new Llibre(ISBN, "Dune", "Herbert", 1965, "descripcio de prova", 9.0, 19.99, true, "");
            l.posarNotes("notes de prova");
            cp.afegirLlibre(l);
            System.out.println("CREATE OK: llibre " + ISBN + " inserit");
            return;
        }
        if ("verify".equals(mode)) {
            int versio = llegirVersioEsquema(cp);
            int llibres = cp.obtenirAllLlibres().size();
            Llibre l = cp.obtenirAllLlibres().isEmpty() ? null : cp.obtenirAllLlibres().get(0);
            if (llibres != 1) throw new IllegalStateException("S'esperava 1 llibre, n'hi ha " + llibres);
            if (l.obtenirISBN() != ISBN) throw new IllegalStateException("ISBN incorrecte: " + l.obtenirISBN());
            if (!"Dune".equals(l.obtenirNom())) throw new IllegalStateException("Nom incorrecte: " + l.obtenirNom());
            Llibre proves = cp.obtenirAllLlibres().get(0);
            cp.carregarHeavyFields(ISBN, proves);
            if (!"descripcio de prova".equals(proves.obtenirDescripcio()))
                throw new IllegalStateException("La descripcio no ha sobreviscut: " + proves.obtenirDescripcio());
            if (!"notes de prova".equals(proves.obtenirNotes()))
                throw new IllegalStateException("Les notes no han sobreviscut: " + proves.obtenirNotes());
            System.out.println("VERIFY OK: versio esquema=" + versio + " llibres=" + llibres);
            return;
        }
        System.err.println("Mode desconegut: " + mode);
        System.exit(2);
    }

    private static int llegirVersioEsquema(ControladorPersistencia cp) throws Exception {
        Connection con = cp.obtenirConnexio();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(version) FROM schema_version")) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
