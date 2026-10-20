package persistencia;

import domini.Llibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import persistencia.internal.ControladorPersistencia;
import persistencia.row.AutorRow;

/**
 * Sincronització dels autors d'un llibre.
 *
 * <p>La implementació agrupa la resolució dels ids d'autor en una sola
 * consulta i insereix els enllaços a {@code llibre_autor} en un únic lot.
 * Aquest test fixa el comportament observable, que no ha de canviar:
 *
 * <ul>
 *   <li>Un llibre amb dos autors crea dos enllaços.</li>
 *   <li>Desar un llibre substitueix el seu conjunt d'autors.</li>
 *   <li>Un autor repetit no genera enllaços duplicats.</li>
 *   <li>Un nom d'autor diferent del que ja existeix crea un autor nou
 *       en lloc de reutilitzar-ne un altre.</li>
 * </ul>
 */
class TestSincronitzacioAutors {

    private static final long ISBN = 9780306406157L;
    private static final long ISBN_2 = 9780262033848L;

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:sincronitzacio_autors;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    private Llibre llibre(long isbn, String... autors) {
        Llibre l = new Llibre(isbn, "Títol " + isbn, autors.length > 0 ? autors[0] : null,
            2000, "descripcio", 5.0, 10.0, false, "");
        if (autors.length > 0) l.posarAutors(java.util.Arrays.asList(autors));
        return l;
    }

    private long autorsDe(long isbn) throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        return cp.obtenirAllLlibreAutorRows().stream()
            .filter(r -> r.isbn() == isbn).count();
    }

    @Test
    @DisplayName("Un llibre amb dos autors crea dos enllaços")
    void dosAutorsCreateDosEnllaços() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(llibre(ISBN, "Herbert", "McCarthy"));

        assertThat(autorsDe(ISBN)).isEqualTo(2);
        assertThat(cp.obtenirAllAutorRows()).extracting(AutorRow::nom)
            .containsExactlyInAnyOrder("Herbert", "McCarthy");
    }

    @Test
    @DisplayName("Desar un llibre substitueix el seu conjunt d'autors")
    void desarSubstitueixAutors() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(llibre(ISBN, "Herbert", "McCarthy"));

        Llibre llegit = cp.obtenirAllLlibres().stream()
            .filter(l -> l.obtenirISBN() == ISBN).findFirst().orElseThrow();
        llegit.posarAutors(java.util.List.of("Herbert"));
        cp.actualitzarLlibre(llegit);

        assertThat(autorsDe(ISBN))
            .as("els autors que ja no hi consten s'han d'esborrar")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Un autor repetit no genera enllaços duplicats")
    void autorRepetitNoDuplicaEnllaços() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(llibre(ISBN, "Herbert", "Herbert", "Herbert"));

        assertThat(autorsDe(ISBN)).isEqualTo(1);
        assertThat(cp.obtenirAllAutorRows()).hasSize(1);
    }

    @Test
    @DisplayName("Un nom d'autor nou crea un autor nou")
    void nomNouCreaAutorNou() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(llibre(ISBN, "Herbert"));
        cp.afegirLlibre(llibre(ISBN_2, "McCarthy"));

        assertThat(cp.obtenirAllAutorRows()).extracting(AutorRow::nom)
            .containsExactlyInAnyOrder("Herbert", "McCarthy");
        assertThat(autorsDe(ISBN)).isEqualTo(1);
        assertThat(autorsDe(ISBN_2)).isEqualTo(1);
    }

    @Test
    @DisplayName("Un llibre sense autors no crea enllaços")
    void senseAutorsNoCreaEnllaços() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.afegirLlibre(llibre(ISBN));

        assertThat(autorsDe(ISBN)).isZero();
        assertThat(cp.obtenirAllAutorRows()).isEmpty();
    }
}
