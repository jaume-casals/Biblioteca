package persistencia;

import domini.Llibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.internal.ControladorPersistencia;

/**
 * Els camps pesats (descripció i notes) només s'escripten a la base de dades
 * quan el llibre els té carregats. Aquest test fixa el contracte:
 *
 * <ul>
 *   <li>Desar un objecte que <em>no</em> ha carregat els camps pesats no
 *       esborra el que hi ha emmagatzemat.</li>
 *   <li>Desar un objecte que <em>sí</em> que els té carregats sí que els
 *       escriu, inclosa el buidat explícit.</li>
 * </ul>
 *
 * <p>El segon cas cobreix la pèrdua de dades històrica: el panell de detalls
 * carrega els camps pesats en segon pla i, si la càrrega falla, el formulari
 * mostra la descripció buida; endesa-la com a buida esborrava la descripció
 * i les notes originals del llibre.
 */
class TestCampsPesatsNoSborren {

    private static final long ISBN = 9780306406157L;

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:camps_pesats;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    private void emmagatzemarLlibre(ControladorPersistencia cp) throws Exception {
        Llibre l = new Llibre(ISBN, "Dune", "Herbert", 1965, "descripcio original", 9.0, 19.99, true, "");
        l.posarNotes("notes originals");
        cp.afegirLlibre(l);
    }

    private Llibre llegirCampsPesats(ControladorPersistencia cp) throws Exception {
        Llibre target = cp.obtenirAllLlibres().get(0);
        cp.carregarHeavyFields(ISBN, target);
        return target;
    }

    @Test
    @DisplayName("Desar un llibre de la vista lleugera no esborra la descripció ni les notes")
    void actualitzarSenseCampsPesatsNoEsborra() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);
        cp.posarLlibreBlob(ISBN, new byte[] {1, 2, 3});

        Llibre lleuger = cp.obtenirAllLlibres().get(0);
        assertThat(lleuger.teCampsPesatsCarregats())
            .as("la vista lleugera no ha de declarar els camps pesats com a carregats")
            .isFalse();

        lleuger.posarNom("Dune (edicio revisada)");
        cp.actualitzarLlibre(lleuger);

        Llibre comprovacio = llegirCampsPesats(cp);
        assertThat(comprovacio.obtenirDescripcio())
            .as("la descripcio emmagatzemada no es pot esborrar amb un llibre sense camps pesats")
            .isEqualTo("descripcio original");
        assertThat(comprovacio.obtenirNotes())
            .as("les notes emmagatzemades no es poden esborrar amb un llibre sense camps pesats")
            .isEqualTo("notes originals");
        assertThat(cp.obtenirLlibreBlob(ISBN)).containsExactly(1, 2, 3);
        assertThat(cp.obtenirAllLlibres().get(0).obtenirNom()).isEqualTo("Dune (edicio revisada)");
    }

    @Test
    @DisplayName("Desar un llibre amb els camps pesats carregats sí que els escriu")
    void actualitzarAmbCampsPesatsEscriu() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        Llibre carregat = cp.obtenirAllLlibres().get(0);
        cp.carregarHeavyFields(ISBN, carregat);
        assertThat(carregat.teCampsPesatsCarregats()).isTrue();

        carregat.posarDescripcio("descripcio nova");
        carregat.posarNotes("notes noves");
        cp.actualitzarLlibre(carregat);

        Llibre comprovacio = llegirCampsPesats(cp);
        assertThat(comprovacio.obtenirDescripcio()).isEqualTo("descripcio nova");
        assertThat(comprovacio.obtenirNotes()).isEqualTo("notes noves");
    }

    @Test
    @DisplayName("El buidat explícit de la descripció sí que s'explica (camps carregats i valor buit)")
    void buidatExplicitSiEscriu() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        Llibre carregat = cp.obtenirAllLlibres().get(0);
        cp.carregarHeavyFields(ISBN, carregat);
        carregat.posarDescripcio("");
        carregat.posarNotes("");
        cp.actualitzarLlibre(carregat);

        Llibre comprovacio = llegirCampsPesats(cp);
        assertThat(comprovacio.obtenirDescripcio()).isEmpty();
        assertThat(comprovacio.obtenirNotes()).isEmpty();
    }
}
