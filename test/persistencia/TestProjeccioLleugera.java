package persistencia;

import domini.Llibre;
import domini.LlibreFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.internal.ControladorPersistencia;

/**
 * Contracte de la projecció lleugera a la via de cerca i paginació.
 *
 * <p>La consulta de cerca/paginació projecta només les columnes lleugeres i
 * retorna llibres amb els camps pesats (descripció i notes) <em>no</em>
 * carregats. Això és intencionat: la taula no els mostra, i el panell de
 * detalls els carrega a màdul quan l'usuari obre un llibre.
 *
 * <p>Aquest test fixa tres coses:
 * <ul>
 *   <li>La cerca i la paginació retornen llibres lleugers.</li>
 *   <li>Desar un resultat de cerca no esborra la descripció ni les notes
 *       emmagatzemades (el comportament lleuger, ja existent, es manté).</li>
 *   <li>Filtrar per llista i etiqueta alhora no duplica cap llibre: els dos
 *       JOIN són sobre claus primàries compostes amb un valor fix, de manera
 *       que cap llibre pot aparèixer més d'una vegada.</li>
 * </ul>
 */
class TestProjeccioLleugera {

    private static final long ISBN_A = 9780306406157L;
    private static final long ISBN_B = 9780262033848L;

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:projeccio_lleugera;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    private void emplenar(ControladorPersistencia cp) throws Exception {
        Llibre a = new Llibre(ISBN_A, "Dune", "Herbert", 1965, "descripcio A", 9.0, 19.99, true, "");
        a.posarNotes("notes A");
        cp.afegirLlibre(a);
        Llibre b = new Llibre(ISBN_B, "Neuromancer", "Gibson", 1984, "descripcio B", 8.0, 12.5, false, "");
        b.posarNotes("notes B");
        cp.afegirLlibre(b);
    }

    @Test
    @DisplayName("La cerca torna llibres amb els camps pesats sense carregar")
    void cercaRetornaLlibresLleugers() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emplenar(cp);

        var resultats = cp.cercarLlibres(LlibreFilter.empty(), 0, 100);

        assertThat(resultats).hasSize(2);
        for (Llibre l : resultats) {
            assertThat(l.teCampsPesatsCarregats())
                .as("la cerca ha de projectar la vista lleugera")
                .isFalse();
        }
    }

    @Test
    @DisplayName("La paginació torna llibres amb els camps pesats sense carregar")
    void paginacioRetornaLlibresLleugers() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emplenar(cp);

        var resultats = cp.cercarLlibres(LlibreFilter.empty(), 0, 1);

        assertThat(resultats).hasSize(1);
        assertThat(resultats.get(0).teCampsPesatsCarregats()).isFalse();
    }

    @Test
    @DisplayName("Desar un resultat de cerca no esborra la descripció ni les notes")
    void desarResultatDeCercaNoEsborra() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emplenar(cp);

        Llibre fromCerca = cp.cercarLlibres(LlibreFilter.empty().withIsbn(ISBN_A), 0, 100).get(0);
        assertThat(fromCerca.teCampsPesatsCarregats()).isFalse();
        fromCerca.posarNom("Dune (revisada)");
        cp.actualitzarLlibre(fromCerca);

        Llibre comprovacio = cp.obtenirAllLlibres().stream()
            .filter(l -> l.obtenirISBN() == ISBN_A).findFirst().orElseThrow();
        cp.carregarHeavyFields(ISBN_A, comprovacio);
        assertThat(comprovacio.obtenirDescripcio()).isEqualTo("descripcio A");
        assertThat(comprovacio.obtenirNotes()).isEqualTo("notes A");
        assertThat(comprovacio.obtenirNom()).isEqualTo("Dune (revisada)");
    }

    @Test
    @DisplayName("Filtrar per llista i etiqueta alhora no duplica cap llibre")
    void filtreSobreDosJoinNoDuplica() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emplenar(cp);
        int llistaId = cp.crearLlista("Fixa");
        int tagId = cp.crearTag("Ciencia-ficcio");
        cp.afegirLlibreToLlista(ISBN_A, llistaId, 0, false);
        cp.afegirLlibreToLlista(ISBN_B, llistaId, 0, false);
        cp.afegirLlibreToTag(ISBN_A, tagId);
        cp.afegirLlibreToTag(ISBN_B, tagId);

        var resultats = cp.cercarLlibres(
            LlibreFilter.empty().withLlistaId(llistaId).withTagId(tagId), 0, 0);

        assertThat(resultats).hasSize(2);
        assertThat(resultats.stream().map(Llibre::obtenirISBN).distinct().count())
            .as("cap llibre pot aparèixer duplicat quan hi ha dos JOIN")
            .isEqualTo(2);
    }
}
