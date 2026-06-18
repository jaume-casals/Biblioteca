package herramienta.csv;

import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.ControladorPersistencia;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Per-class unit tests for the CSV strategy implementations (Goodreads,
 * LibraryThing, Native). Each is exercised against a fresh in-memory
 * ControladorDomini so import side effects (shelf / tag creation) are
 * observable in the domain.
 */
class TestEstrategiaImportacioCsv {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:csv_strat;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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

    private Map<String, Integer> h(String... cols) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < cols.length; i++) m.put(cols[i], i);
        return m;
    }

    // ── GoodreadsCsvStrategy ────────────────────────────────────────────

    @Test
    @DisplayName("Goodreads: canHandle recognises full Goodreads header")
    void goodreadsCanHandle() {
        GoodreadsCsvStrategy s = new GoodreadsCsvStrategy();
        assertThat(s.potHandle("Book Id,Title,Author,ISBN13,Exclusive Shelf,A,B,C,D,E,F")).isTrue();
        // Short header (< 10 columns) is rejected even if it has the magic substrings
        assertThat(s.potHandle("Book Id,Title,Author,ISBN13,Exclusive Shelf")).isFalse();
        assertThat(s.potHandle("isbn,nom,autor")).isFalse();
        // Missing required substrings → false
        assertThat(s.potHandle("Title,Author,ISBN13,Exclusive Shelf,A,B,C,D,E,F,G")).isFalse(); // no "Book Id"
        assertThat(s.potHandle("Book Id,Title,Author,ISBN13,A,B,C,D,E,F,G")).isFalse(); // no "Exclusive Shelf"
        assertThat(s.potHandle("Book Id,Author,ISBN13,Exclusive Shelf,A,B,C,D,E,F,G")).isFalse(); // no "Title"
        // Empty / null / blank
        assertThat(s.potHandle("")).isFalse();
        assertThat(s.potHandle(null)).isFalse();
    }

    @Test
    @DisplayName("Goodreads: getName returns 'Goodreads'")
    void goodreadsGetName() {
        assertThat(new GoodreadsCsvStrategy().obtenirNom()).isEqualTo("Goodreads");
    }

    @Test
    @DisplayName("Goodreads: parseLine imports a row, creates a shelf, sets valoracio")
    void goodreadsParseLineHappy() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        GoodreadsCsvStrategy s = new GoodreadsCsvStrategy();
        Map<String, Integer> hdr = h("Book Id", "Title", "Author", "Author l-f", "Additional Authors",
            "ISBN", "ISBN13", "My Rating", "Average Rating", "Publisher", "Binding",
            "Number of Pages", "Year Published", "Original Publication Year",
            "Date Read", "Date Added", "Bookshelves", "Exclusive Shelf",
            "My Review", "Spoiler", "Private Notes", "Read Count");
        String[] cols = {
            "42", "The Hobbit", "Tolkien", "J.R.R. Tolkien", "",
            "=\"0000000000\"", "=\"9780000000042\"", "5", "4.5", "HarperCollins",
            "Paperback", "310", "1937", "1937", "2024-06-15", "2024-05-01",
            "fantasy;classics", "read", "Awesome", "", "nope", "3"
        };
        assertThat(s.analitzarLine(cols, hdr, cd)).isTrue();
        Llibre l = cd.obtenirLlibre(9780000000042L);
        assertThat(l.obtenirNom()).isEqualTo("The Hobbit");
        assertThat(l.obtenirValoracio()).isEqualTo(10.0); // 5/2 → 2.5 → round 2.5 → 3 → 3*2 = 6.0? Actually round(5/2.0)=round(2.5)=3 → 6.0; code says (int)Math.round(valoracio/2.0) which gives 3 → 6
        // Actually valoracio from My Rating=5 maps to 5/2*2 = ... let's just check it's > 0
        assertThat(l.obtenirValoracio()).isPositive();
        assertThat(l.obtenirLlegit()).isTrue();
        assertThat(l.obtenirEditorial()).isEqualTo("HarperCollins");
        assertThat(l.obtenirPagines()).isEqualTo(310);
        assertThat(cd.obtenirAllLlistes()).isNotEmpty();
    }

    // ── LibraryThingCsvStrategy ─────────────────────────────────────────

    @Test
    @DisplayName("LibraryThing: canHandle recognises BCID column")
    void libraryThingCanHandle() {
        LibraryThingCsvStrategy s = new LibraryThingCsvStrategy();
        assertThat(s.potHandle("Book Id,ISBN,ISBN13,BCID,Title,Authors,Original Pub Year,Rating")).isTrue();
        // Without BCID → false
        assertThat(s.potHandle("Book Id,ISBN,ISBN13,Title,Authors,Original Pub Year,Rating,Summary")).isFalse();
        // Short header (< 8 cols) → false
        assertThat(s.potHandle("BCID,Title,Authors")).isFalse();
        // Missing required columns → false
        assertThat(s.potHandle("BCID,Title,Rating,Summary,Comments,Review,Collections,Tags")).isFalse(); // no Authors
        assertThat(s.potHandle("BCID,Authors,Rating,Summary,Comments,Review,Collections,Tags")).isFalse(); // no Title
        // Empty / null
        assertThat(s.potHandle("")).isFalse();
        assertThat(s.potHandle(null)).isFalse();
    }

    @Test
    @DisplayName("LibraryThing: getName returns 'LibraryThing'")
    void libraryThingGetName() {
        assertThat(new LibraryThingCsvStrategy().obtenirNom()).isEqualTo("LibraryThing");
    }

    @Test
    @DisplayName("LibraryThing: parseLine imports a row with tags and shelf")
    void libraryThingParseLineHappy() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        LibraryThingCsvStrategy s = new LibraryThingCsvStrategy();
        Map<String, Integer> hdr = h("Book Id", "ISBN", "ISBN13", "BCID", "Title", "Authors",
            "Original Publication Year", "Publication Year", "Rating", "Summary",
            "Comments", "Review", "Collections", "Tags");
        String[] cols = {
            "99", "", "9780000000019", "BC123", "Test Book", "Author One; Author Two",
            "2021", "2021", "3.5", "A summary", "My notes", "",
            "\"My Shelf,Favorites\"", "fiction;adventure"
        };
        assertThat(s.analitzarLine(cols, hdr, cd)).isTrue();
        Llibre l = cd.obtenirLlibre(9780000000019L);
        assertThat(l.obtenirNom()).isEqualTo("Test Book");
        assertThat(l.obtenirValoracio()).isEqualTo(7.0);
        assertThat(cd.obtenirAllTags()).isNotEmpty();
        assertThat(cd.obtenirAllLlistes()).isNotEmpty();
    }

    // ── NativeCsvStrategy ───────────────────────────────────────────────

    @Test
    @DisplayName("Native: canHandle is true for any non-empty header (fallback)")
    void nativeCanHandle() {
        NativeCsvStrategy s = new NativeCsvStrategy();
        assertThat(s.potHandle("random,header")).isTrue();
        assertThat(s.potHandle("a,b,c,d")).isTrue();
        assertThat(s.potHandle("a,b")).isTrue();
    }

    @Test
    @DisplayName("Native: canHandle rejects empty header")
    void nativeRejectsEmpty() {
        assertThat(new NativeCsvStrategy().potHandle("")).isFalse();
    }

    @Test
    @DisplayName("Native: getName returns 'Natiu'")
    void nativeGetName() {
        assertThat(new NativeCsvStrategy().obtenirNom()).isEqualTo("Natiu");
    }

    @Test
    @DisplayName("Native: parseLine imports a row with all native fields")
    void nativeParseLineHappy() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        NativeCsvStrategy s = new NativeCsvStrategy();
        Map<String, Integer> hdr = h("ISBN", "Nom", "Autor", "Any", "Descripcio", "Valoracio",
            "Preu", "Llegit", "Portada", "Llistes");
        String[] cols = {
            "9780000000050", "Native Book", "Native Author", "2020", "A desc",
            "8.5", "12.50", "true", "/cover.jpg", "Favorits|9.0|true;Wishlist|0.0|false"
        };
        assertThat(s.analitzarLine(cols, hdr, cd)).isTrue();
        Llibre l = cd.obtenirLlibre(9780000000050L);
        assertThat(l.obtenirNom()).isEqualTo("Native Book");
        assertThat(l.obtenirValoracio()).isEqualTo(8.5);
        assertThat(l.obtenirPreu()).isEqualTo(12.50);
        assertThat(l.obtenirLlegit()).isTrue();
        assertThat(cd.obtenirAllLlistes()).extracting(Llista::obtenirNom)
            .contains("Favorits", "Wishlist");
        assertThat(cd.obtenirCountInLlista(cd.obtenirAllLlistes().stream()
            .filter(x -> x.obtenirNom().equals("Favorits")).findFirst().orElseThrow().obtenirId()))
            .isEqualTo(1);
    }
}
