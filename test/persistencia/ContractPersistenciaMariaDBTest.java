package persistencia;

import org.junit.jupiter.api.Assumptions;
import persistencia.internal.ConnectionConfig;
import persistencia.internal.ControladorPersistencia;

/**
 * Contracte de persistència contra <b>MariaDB real</b>.
 *
 * <p>Aquesta classe <b>no</b> s'executa dins de {@code make test}: el
 * llançador JUnit arrenca tota la suite en un sol JVM i el
 * {@code biblioteca.h2.url} que hi posa força el tipus de base de dades a
 * H2. Per això el contracte de MariaDB té el seu propio llançador
 * ({@code make test-mariadb} / {@code scripts/mariadb_test.sh}), en una JVM
 * separada i <b>sense</b> {@code biblioteca.h2.url}.
 *
 * <p>Sense les propietats de connexió, la classe queda <b>saltada</b> (no
 * fallada) i no obre cap connexió: no s'arriba a tocar cap base de dades.
 *
 * <p>Propietats del sistema (totes opcionals excepte la primera):
 * <ul>
 *   <li>{@code biblioteca.mariadb.host} — {@code host} o {@code host:port} (obligatòria)</li>
 *   <li>{@code biblioteca.mariadb.user} — usuari (per defecte {@code root})</li>
 *   <li>{@code biblioteca.mariadb.password} — contrasenya (per defecte buida)</li>
 *   <li>{@code biblioteca.mariadb.database} — esquema on escriure
 *       (per defecte {@code BIBLIOTECA_TEST}; <b>mai</b> {@code BIBLIOTECA},
 *       que és l'esquema de producció)</li>
 * </ul>
 */
class ContractPersistenciaMariaDBTest extends ContractPersistenciaBase {

    static final String PROP_HOST     = "biblioteca.mariadb.host";
    static final String PROP_USER     = "biblioteca.mariadb.user";
    static final String PROP_PASSWORD = "biblioteca.mariadb.password";
    static final String PROP_DATABASE = "biblioteca.mariadb.database";

    /** Esquema de rebut per defecte — mai el de producció. */
    static final String ESQUEMA_PER_DEFECTE = "BIBLIOTECA_TEST";

    static {
        // Marca de test: evita la còpia de seguretat automàtica.
        // NO es posa biblioteca.h2.url: el seu efecte seria forçar dbType=h2.
        System.setProperty("biblioteca.test", "true");
    }

    @Override
    protected ControladorPersistencia obrirControlador() {
        String host = System.getProperty(PROP_HOST);
        Assumptions.assumeTrue(host != null && !host.isBlank(),
            "MariaDB no configurat: cal -D" + PROP_HOST + "=host[:port] "
                + "-D" + PROP_USER + "=... -D" + PROP_PASSWORD + "=... "
                + "-D" + PROP_DATABASE + "=... (veure docs/MARIADB-TEST.md)");

        String user = System.getProperty(PROP_USER, "root");
        String password = System.getProperty(PROP_PASSWORD, "");
        String database = System.getProperty(PROP_DATABASE, ESQUEMA_PER_DEFECTE);
        Assumptions.assumeFalse("BIBLIOTECA".equals(database),
            "El contracte de proves no ha d'escriure a l'esquema de producció BIBLIOTECA");

        return ControladorPersistencia.create(new ConnectionConfig(
            "mariadb", host, user, password, "contract", null, database));
    }

    @Override
    protected String nomEngine() {
        return "MariaDB";
    }

    @Override
    protected boolean cercaIgnoraMajuscules() {
        // MariaDB usa per defecte una col·lació *_ci: el LIKE no distingeix
        // majúscules de minúscules.
        return true;
    }

    @Override
    protected Integer limitCobertaBytes() {
        // El tipus BLOB de MySQL/MariaDB es limita a 65535 bytes.
        return 65535;
    }
}
