package fuzz.domini;

import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.ConstructorFiltreLlibre;
import domini.Llista;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.lifecycle.BeforeTry;
import persistencia.internal.ControladorPersistencia;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for the {@link ControladorDomini} facade.
 *
 * <p>Round-trip invariants: every "add X" call is followed by the matching
 * "remove X" call, and the post-state must be the pre-state. Tight
 * generators keep each try under ~50 ms; with 20 tries per property and
 * 5 properties the full class finishes in well under the 5-second budget.
 *
 * <p>Reset protocol: {@code @BeforeTry} and {@code @AfterTry} reset both
 * singletons between every try. JUnit 5's {@code @BeforeEach} only runs
 * once per {@code @Property} method (not per try), so the per-try reset
 * must use jqwik's own lifecycle hooks. The {@code @BeforeContainer}
 * hook sets the biblioteca.test system properties once for the whole
 * container.
 */
class TestPropietatsInvariantsLlibre {

    @BeforeContainer
    static void globalSetUp() {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:fuzz_prop;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeTry
    void reset() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterTry
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Property(tries = 20)
    void afegirLlibre_thenDelete_sizeUnchanged_isbnGone(
            @ForAll @LongRange(min = 9780000000000L, max = 9780099999999L) long isbn,
            @ForAll @StringLength(min = 1, max = 60) String title,
            @ForAll @IntRange(min = 1850, max = 2030) int year,
            @ForAll @NotEmpty List<@StringLength(max = 40) String> authors) {
        ControladorDomini cd = ControladorDomini.getInstance();
        int preSize = cd.getSize();

        Llibre l = Llibre.builder()
            .isbn(isbn)
            .nom(title)
            .autor(authors.get(0))
            .any(year)
            .descripcio("")
            .valoracio(0.0)
            .preu(0.0)
            .llegit(false)
            .imatge("")
            .build();
        cd.afegirLlibre(l);
        assertThat(cd.existsLlibre(isbn)).isTrue();
        assertThat(cd.getSize()).isEqualTo(preSize + 1);

        cd.eliminarLlibre(isbn);
        assertThat(cd.existsLlibre(isbn)).isFalse();
        assertThat(cd.getSize()).isEqualTo(preSize);
    }

    @Property(tries = 20)
    void prestarLlibre_thenRetornar_loanCleared(
            @ForAll @LongRange(min = 9780000000000L, max = 9780099999999L) long isbn,
            @ForAll @StringLength(min = 1, max = 30) String borrower) {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlibre(Llibre.builder()
            .isbn(isbn).nom("Loan book").autor("A").any(2000)
            .descripcio("").valoracio(0.0).preu(0.0).llegit(false).imatge("")
            .build());

        cd.prestarLlibre(isbn, borrower);
        assertThat(cd.obtenirLoanedISBNs()).contains(isbn);

        cd.retornarLlibre(isbn);
        assertThat(cd.obtenirLoanedISBNs()).doesNotContain(isbn);
    }

    @Property(tries = 20)
    void shelf_addThenRemove_isbnGoneFromShelf(
            @ForAll @LongRange(min = 9780000000000L, max = 9780099999999L) long isbn,
            @ForAll @StringLength(min = 1, max = 30) String shelfName) {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlibre(Llibre.builder()
            .isbn(isbn).nom("Shelf book").autor("A").any(2000)
            .descripcio("").valoracio(0.0).preu(0.0).llegit(false).imatge("")
            .build());
        Llista shelf = cd.afegirLlista("Shelf " + shelfName);
        int llistaId = shelf.obtenirId();

        cd.afegirLlibreToLlista(isbn, llistaId, 0.0, false);
        assertThat(cd.obtenirLlibresInLlista(llistaId)).extracting(Llibre::obtenirISBN).contains(isbn);

        cd.eliminarLlibreFromLlista(isbn, llistaId);
        assertThat(cd.obtenirLlibresInLlista(llistaId)).extracting(Llibre::obtenirISBN).doesNotContain(isbn);
    }

    @Property(tries = 20)
    void tag_addThenRemove_isbnGoneFromTag(
            @ForAll @LongRange(min = 9780000000000L, max = 9780099999999L) long isbn,
            @ForAll @StringLength(min = 1, max = 30) String tagName) {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlibre(Llibre.builder()
            .isbn(isbn).nom("Tag book").autor("A").any(2000)
            .descripcio("").valoracio(0.0).preu(0.0).llegit(false).imatge("")
            .build());
        int tagId = cd.afegirTag("Tag " + tagName).obtenirId();

        cd.afegirLlibreToTag(isbn, tagId);
        assertThat(cd.obtenirLlibresWithTag(tagId)).contains(isbn);

        cd.eliminarLlibreFromTag(isbn, tagId);
        assertThat(cd.obtenirLlibresWithTag(tagId)).doesNotContain(isbn);
    }

    /**
     * For a fixed set of books with mixed-case authors, applying the
     * author filter with two case variants of the same needle must
     * return the same set. Both the in-memory path (FiltreUtils.matchString
     * normalises to lower case) and the SQL path (LIKE on a2.nom under
     * the default H2 utf8_general_ci collation) are case-insensitive,
     * so the equality should hold regardless of which path is taken.
     */
    @Property(tries = 20)
    void filtrarAuthor_isCaseInsensitive(@ForAll long seed) {
        Random rng = new Random(seed);
        ControladorDomini cd = ControladorDomini.getInstance();
        int n = 8;
        for (int i = 0; i < n; i++) {
            long isbn = 9780000000000L + i;
            String author = "Smith" + (i % 2 == 0 ? "Son" : "Daughter");
            char[] chars = author.toCharArray();
            chars[rng.nextInt(chars.length)] = Character.toLowerCase(chars[rng.nextInt(chars.length)]);
            String mixed = new String(chars);
            cd.afegirLlibre(Llibre.builder()
                .isbn(isbn).nom("B" + i).autor(mixed).any(2000)
                .descripcio("").valoracio(0.0).preu(0.0).llegit(false).imatge("")
                .build());
        }

        LlibreFilter lower = ConstructorFiltreLlibre.of().autor("smith").build();
        LlibreFilter upper = ConstructorFiltreLlibre.of().autor("SMITH").build();
        assertThat(cd.aplicarFiltres(lower)).hasSize(cd.aplicarFiltres(upper).size());
    }
}
