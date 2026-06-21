package herramienta.io.export;

import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import herramienta.text.ValidadorLlibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for {@link GoodreadsExportService}.
 * Verifies the header, a single row, and the shelf/read-fallback mapping.
 */
class GoodreadsExportServiceTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:goodreads_export;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    @DisplayName("exportToCsv: header has all expected Goodreads columns")
    void header() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String header = csv.split("\n")[0];
        assertThat(header).contains("Book Id").contains("Title").contains("Author")
            .contains("ISBN13").contains("My Rating").contains("Publisher")
            .contains("Binding").contains("Number of Pages").contains("Year Published")
            .contains("Date Read").contains("Date Added").contains("Bookshelves")
            .contains("Exclusive Shelf").contains("My Review").contains("Read Count")
            .contains("BCID");
    }

    @Test
    @DisplayName("exportToCsv: empty library → just the header")
    void emptyLibrary() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String csv = GoodreadsExportService.exportarToCsv(cd);
        assertThat(csv.split("\n")).hasSize(1);
    }

    @Test
    @DisplayName("exportToCsv: one read book → Exclusive Shelf is 'read'")
    void llegirBookShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 10.0, 19.99, true, "");
        l.posarEditorial("Chilton");
        cd.afegirLlibre(l);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        assertThat(rows.length).isGreaterThanOrEqualTo(2);
        // Last quoted field of the data row = Exclusive Shelf
        assertThat(rows[1]).contains("\"read\"");
        assertThat(rows[1]).contains("Dune");
        assertThat(rows[1]).contains("Herbert");
    }

    @Test
    @DisplayName("exportToCsv: unread book in a shelf → Exclusive Shelf is the shelf name")
    void unreadBookShelfName() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 0.0, 19.99, false, "");
        cd.afegirLlibre(l);
        Llista shelf = cd.afegirLlista("Wishlist");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 0.0, false);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        assertThat(rows[1]).contains("\"Wishlist\"");
    }

    @Test
    @DisplayName("exportToCsv: unread book in NO shelf → Exclusive Shelf is 'to-read'")
    void unreadBookToRead() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 0.0, 0.0, false, "");
        cd.afegirLlibre(l);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        assertThat(rows[1]).contains("\"to-read\"");
    }

    @Test
    @DisplayName("exportToCsv: bookshelves column is the comma-joined shelf names")
    void bookshelvesColumn() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 0.0, 0.0, false, "");
        cd.afegirLlibre(l);
        Llista a = cd.afegirLlista("A");
        Llista b = cd.afegirLlista("B");
        cd.afegirLlibreToLlista(9780306406157L, a.obtenirId(), 0.0, false);
        cd.afegirLlibreToLlista(9780306406157L, b.obtenirId(), 0.0, false);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        // Look at the Bookshelves field — it must contain both A and B
        String row = rows[1];
        assertThat(row).contains("\"A, B\"").contains("\"A\"") ;
    }

    @Test
    @DisplayName("exportToCsv: My Rating is integer Goodreads scale (valoracio/2 rounded)")
    void myRatingMapping() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 10.0, 0.0, true, ""); // 10/2 = 5 stars
        cd.afegirLlibre(l);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        // Field index 7 (0-based) = My Rating
        // row is: 1,"Dune","Herbert","Herbert",,"=\"9780306406157\"","=\"9780306406157\"",5,...
        assertThat(rows[1]).contains(",5,");
    }

    @Test
    @DisplayName("exportToCsv: valoracio=0 maps to My Rating 0 (not negative)")
    void zeroRatingMapsToZero() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Herbert", 1965,
            "desc", 0.0, 0.0, false, "");
        cd.afegirLlibre(l);
        String csv = GoodreadsExportService.exportarToCsv(cd);
        String[] rows = csv.split("\n");
        // 0 → 0
        assertThat(rows[1]).contains(",0,");
    }
}
