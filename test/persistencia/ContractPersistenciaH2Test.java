package persistencia;

import persistencia.internal.ConnectionConfig;
import persistencia.internal.ControladorPersistencia;

/**
 * Contracte de persistència contra <b>H2 en memòria</b>.
 *
 * <p>Aquesta classe s'executa dins de {@code make test}: el llançador JUnit
 * arrenca tota la suite en un sol JVM i selecciona cada classe de test
 * trobada a {@code test/}.
 *
 * <p>El contracte usa la seva <b>pròpia</b> base de dades en memòria
 * ({@code contract_h2}), separada de la que fa servir la resta de la suite
 * ({@code biblioteca_suite}). Així el contracte no pot contaminar —ni ser
 * contaminat per— la resta de les proves, i el canvi del bloc
 * {@code static} no altera la URL global del procés.
 *
 * <p>El bloc {@code static} només fixa {@code biblioteca.h2.url} si <em>no</em>
 * n'hi ha cap: en una execució suelta (fora de {@code make test}) la fixa per
 *què cap camí accidentat cap a la base de dades personal de l'usuari; dins
 * de {@code make test} el valor ja hi és i es respecta.
 */
class ContractPersistenciaH2Test extends ContractPersistenciaBase {

    /** Base de dades en memòria pròpia del contracte. */
    static final String URL_CONTRACTE =
        "jdbc:h2:mem:contract_h2;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";

    static {
        // La marca de test evita la còpia de seguretat automàtica i el
        // reinici periòdic de la base de dades.
        System.setProperty("biblioteca.test", "true");
        // No sobreescriu una URL ja indicada (la de `make test`).
        if (System.getProperty("biblioteca.h2.url") == null) {
            System.setProperty("biblioteca.h2.url", URL_CONTRACTE);
        }
    }

    @Override
    protected ControladorPersistencia obrirControlador() {
        return ControladorPersistencia.create(new ConnectionConfig(
            "h2", "localhost", "sa", "", "contract_h2", URL_CONTRACTE));
    }

    @Override
    protected String nomEngine() {
        return "H2";
    }

    @Override
    protected boolean cercaIgnoraMajuscules() {
        // H2 compara les cadenes de forma sensible a la caixa.
        return false;
    }

    @Override
    protected Integer limitCobertaBytes() {
        // El BLOB d'H2 no té límit de mida.
        return null;
    }
}
