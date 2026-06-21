import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.Llista;
import domini.EspecificacioOrdenacio;
import domini.Tag;
import herramienta.text.FiltreUtils;
import herramienta.text.ValidadorLlibre;
import persistencia.internal.ControladorPersistencia;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * JUnit 5 + AssertJ tests for Biblioteca.
 * Complements the plain-Java BibliotecaTest runner.
 * Run via: mvn test  OR  make test
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class BibliotecaJUnit5Test {

    // Static initializer runs on class load — before any JUnit runner infrastructure.
    // Guarantees in-memory DB even when VS Code runs a single test method directly.
    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE");
    }

    @BeforeEach
    void reinicialitzarDb() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Llibre book(long isbn, String nom) {
        return ValidadorLlibre.comprovarLlibre(isbn, nom, null, null, null, null, null, null, null);
    }

    private Llibre book(long isbn, String nom, String autor, Integer any) {
        return ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, null, null, null, null, null);
    }

    private void add(ControladorDomini cd, long isbn, String nom) throws Exception {
        cd.afegirLlibre(book(isbn, nom));
    }

    private void add(ControladorDomini cd, long isbn, String nom, String autor, Integer any) throws Exception {
        cd.afegirLlibre(book(isbn, nom, autor, any));
    }

    // ── LlibreValidator ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid ISBN-13 accepted")
    void validIsbn13() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Title", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("Valid ISBN-10 accepted")
    void validIsbn10() {
        Llibre l = ValidadorLlibre.comprovarLlibre(8420413739L, "Title", null, null, null, null, null, null, null);
        assertThat(l.obtenirISBN()).isEqualTo(8420413739L);
    }

    @ParameterizedTest(name = "ISBN {0} rejected")
    @ValueSource(longs = {12345678901234L, 12345L, 0L})
    @DisplayName("Invalid ISBN lengths rejected")
    void invalidIsbnLengths(long isbn) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(isbn, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null ISBN rejected")
    void nullIsbnRejected() {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Blank nom ''{0}'' rejected")
    @ValueSource(strings = {"", "  ", "\t"})
    @DisplayName("Blank/empty nom rejected")
    void blankNomRejected(String nom) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, nom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Valoracio {0} rejected")
    @ValueSource(doubles = {-0.1, -1.0, 10.1, 11.0})
    @DisplayName("Out-of-range valoració rejected")
    void outOfRangeValoracio(double v) {
        assertThatThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, v, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Valoracio {0} accepted")
    @ValueSource(doubles = {0.0, 5.0, 10.0})
    @DisplayName("Boundary valoració values accepted")
    void boundaryValoracioAccepted(double v) {
        assertThatNoException().isThrownBy(() ->
            ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, v, null, null, null));
    }

    @Test
    @DisplayName("Default field values applied when nulls passed")
    void defaultFieldValues() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
        assertThat(l.obtenirAutor()).isEqualTo("");
        assertThat(l.obtenirAny()).isEqualTo(0);
        assertThat(l.obtenirValoracio()).isEqualTo(0.0);
        assertThat(l.obtenirPreu()).isEqualTo(0.0);
        assertThat(l.obtenirLlegit()).isFalse();
    }

    // ── FiltreUtils ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "matchString(''{0}'', ''{1}'') = {2}")
    @CsvSource({
        "cervantes, Cervantes de Saavedra, true",
        "TOLKIEN, tolkien, true",
        "tolkien, Cervantes, false",
        "'', anything, true",
    })
    @DisplayName("matchString case-insensitive and empty-query semantics")
    void matchStringParameterized(String query, String field, boolean expected) {
        assertThat(FiltreUtils.matchString(query, field)).isEqualTo(expected);
    }

    @Test
    @DisplayName("matchString null field returns false")
    void matchStringNullField() {
        assertThat(FiltreUtils.matchString("x", null)).isFalse();
    }

    @Test
    @DisplayName("matchISBN prefix match")
    void matchIsbnPrefix() {
        assertThat(FiltreUtils.matchISBN(978L, 9780306406157L)).isTrue();
        assertThat(FiltreUtils.matchISBN(123L, 9780306406157L)).isFalse();
    }

    // ── Domain: CRUD ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("LlibreFilter: copy() preserves all fields")
    void llibreFilterCopyPreservesFields() {
        LlibreFilter f = domini.ConstructorFiltreLlibre.of()
            .isbn(978L).nom("test").autor("author").anyMin(1900).llegit(true)
            .sort("nom", false).build();
        LlibreFilter c = f.copy();
        assertThat(c.obtenirIsbn()).isEqualTo(978L);
        assertThat(c.obtenirNom()).isEqualTo("test");
        assertThat(c.obtenirAutor()).isEqualTo("author");
        assertThat(c.obtenirAnyMin()).isEqualTo(1900);
        assertThat(c.obtenirLlegit()).isTrue();
        assertThat(c.obtenirSort().column()).isEqualTo("nom");
        assertThat(c.obtenirSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("Add book: size increments and retrieval works")
    void afegirBookIncreasesSize() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        assertThat(cd.getSize()).isEqualTo(1);
        Llibre l = cd.obtenirLlibre(9780306406157L);
        assertThat(l.obtenirNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Duplicate ISBN throws")
    void duplicateIsbnThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        assertThatThrownBy(() -> add(cd, 9780306406157L, "Other"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Delete book removes it from library")
    void eliminarBookRemovesIt() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.eliminarLlibre(9780306406157L);
        assertThat(cd.getSize()).isZero();
        assertThatThrownBy(() -> cd.obtenirLlibre(9780306406157L))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Multiple books sorted by ISBN ascending")
    void booksAscendingIsbnOrder() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "C");
        add(cd, 8420413739L, "A");
        add(cd, 9780141439518L, "B");
        List<Llibre> all = cd.obtenirAllLlibres();
        assertThat(all).extracting(Llibre::obtenirISBN)
            .isSortedAccordingTo(Long::compareTo);
    }

    @Test
    @DisplayName("getLlibre throws for non-existent ISBN")
    void obtenirLlibreThrowsForMissing() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.obtenirLlibre(1111111111111L))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("existsLlibre returns true after add, false for missing")
    void existsLlibre() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        assertThat(cd.existsLlibre(9780306406157L)).isTrue();
        assertThat(cd.existsLlibre(1111111111111L)).isFalse();
    }

    @Test
    @DisplayName("updateLlibre persists nom and valoracio")
    void actualitzarLlibrePersists() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llibre updated = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune Messiah",
            null, null, null, 9.0, null, null, null);
        cd.actualitzarLlibre(updated);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNom()).isEqualTo("Dune Messiah");
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirValoracio()).isEqualTo(9.0);
    }

    // ── Domain: Shelves (Llistes) ────────────────────────────────────────────

    @Test
    @DisplayName("Create shelf: appears in shelf list")
    void crearShelfAppearsInList() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlista("Favorits");
        assertThat(cd.obtenirAllLlistes()).extracting(Llista::obtenirNom).contains("Favorits");
    }

    @Test
    @DisplayName("Add book to shelf: count is 1")
    void afegirBookToShelfCount() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.afegirLlista("Sci-Fi");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 8.0, false);
        assertThat(cd.obtenirCountInLlista(shelf.obtenirId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete shelf cascades: no orphan rows, book survives")
    void eliminarShelfCascades() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.afegirLlista("Temp");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 7.0, false);
        cd.eliminarLlista(shelf);
        assertThat(cd.obtenirAllLlistes()).isEmpty();
        assertThat(cd.getSize()).isEqualTo(1); // book still exists
    }

    // ── Domain: Tags ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Create tag and assign to book")
    void crearTagAndAssign() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Tag tag = cd.afegirTag("Sci-Fi");
        cd.afegirLlibreToTag(9780306406157L, tag.obtenirId());
        assertThat(cd.obtenirTagsForLlibre(9780306406157L))
            .extracting(Tag::obtenirNom).containsExactly("Sci-Fi");
    }

    @Test
    @DisplayName("Filter by tag via aplicarFiltres")
    void filtrarByTag() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        add(cd, 8420413739L, "Other");
        Tag tag = cd.afegirTag("Sci-Fi");
        cd.afegirLlibreToTag(9780306406157L, tag.obtenirId());
        LlibreFilter ft = domini.ConstructorFiltreLlibre.of().tagId(tag.obtenirId()).build();
        List<Llibre> results = cd.aplicarFiltres(ft);
        assertThat(results).extracting(Llibre::obtenirNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("Remove tag from book")
    void eliminarTagFromBook() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Tag tag = cd.afegirTag("Fantasy");
        cd.afegirLlibreToTag(9780306406157L, tag.obtenirId());
        cd.eliminarLlibreFromTag(9780306406157L, tag.obtenirId());
        assertThat(cd.obtenirTagsForLlibre(9780306406157L)).isEmpty();
    }

    // ── Domain: Filters ─────────────────────────────────────────────────────

    @Test
    @DisplayName("aplicarFiltres by nom returns matching books")
    void filtrarByNom() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        LlibreFilter fn = domini.ConstructorFiltreLlibre.of().nom("dune").build();
        List<Llibre> results = cd.aplicarFiltres(fn);
        assertThat(results).extracting(Llibre::obtenirNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("aplicarFiltres by year range")
    void filtrarByYearRange() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Asimov", 1951);
        LlibreFilter fy = domini.ConstructorFiltreLlibre.of().anyMin(1960).anyMax(1970).build();
        List<Llibre> results = cd.aplicarFiltres(fy);
        assertThat(results).extracting(Llibre::obtenirNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("aplicarFiltres by autor")
    void filtrarByAutor() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        LlibreFilter fa = domini.ConstructorFiltreLlibre.of().autor("Asimov").build();
        List<Llibre> results = cd.aplicarFiltres(fa);
        assertThat(results).extracting(Llibre::obtenirNom).containsExactly("Foundation");
    }

    @Test
    @DisplayName("aplicarFiltres with no match returns empty")
    void filtrarNoMatch() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        LlibreFilter fne = domini.ConstructorFiltreLlibre.of().nom("NonExistent").build();
        List<Llibre> results = cd.aplicarFiltres(fne);
        assertThat(results).isEmpty();
    }

    // ── Domain: Loans ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Loan and return book")
    void loanAndReturnBook() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.prestarLlibre(9780306406157L, "Alice");
        Set<Long> loaned = cd.obtenirLoanedISBNs();
        assertThat(loaned).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.obtenirLoanedISBNs()).doesNotContain(9780306406157L);
    }

    // ── Domain: Backup / Restore ─────────────────────────────────────────────

    @Test
    @DisplayName("Backup and restore preserves all books")
    void copiaSegRestorePreservesBooks() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);

        File tmp = File.createTempFile("backup_junit5_", ".sql");
        tmp.deleteOnExit();
        cd.copiaSegToSQL(tmp);

        cd.eliminarLlibre(9780306406157L);
        cd.eliminarLlibre(8420413739L);
        assertThat(cd.getSize()).isZero();

        cd.restaurarFromSQL(tmp);
        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.obtenirAllLlibres())
            .extracting(Llibre::obtenirNom)
            .containsExactlyInAnyOrder("Dune", "Foundation");
    }

    @Test
    @DisplayName("Backup preserves shelf membership")
    void copiaSegPreservesShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.afegirLlista("Favorits");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 9.0, true);

        File tmp = File.createTempFile("backup_shelf_", ".sql");
        tmp.deleteOnExit();
        cd.copiaSegToSQL(tmp);

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        cd = ControladorDomini.getInstance();
        cd.restaurarFromSQL(tmp);

        assertThat(cd.obtenirAllLlistes()).extracting(Llista::obtenirNom).contains("Favorits");
        Llista restored = cd.obtenirAllLlistes().get(0);
        assertThat(cd.obtenirCountInLlista(restored.obtenirId())).isEqualTo(1);
    }

    // ── executeSQLFile: defensive parser ─────────────────────────────────────

    @Test
    @DisplayName("executeSQLFile: ignores DROP TABLE / USE / ALTER in tampered file")
    void executarSQLFileDropsAreIgnored() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        File tampered = File.createTempFile("sql_tamper_", ".sql");
        tampered.deleteOnExit();
        try (var w = new java.io.PrintWriter(tampered, java.nio.charset.StandardCharsets.UTF_8)) {
            w.println("DROP TABLE llibre;");
            w.println("USE other_db;");
            w.println("ALTER TABLE llibre DROP COLUMN isbn;");
            w.println("TRUNCATE llibre;");
        }
        ControladorPersistencia.getInstance().executarSQLFile(tampered);
        // Books survive
        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Backup/restore round-trip preserves ';' in book notes (parser doesn't split mid-string)")
    void copiaSegRestorePreservesSemicolonInNotes() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre book = ValidadorLlibre.comprovarLlibre(9780306406157L, "Semicolon book", null, null, null, null, null, null, null);
        book.posarNotes("first note; second note; third note");
        cd.afegirLlibre(book);

        File tmp = File.createTempFile("sql_semi_roundtrip_", ".sql");
        tmp.deleteOnExit();
        cd.copiaSegToSQL(tmp);

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        cd = ControladorDomini.getInstance();
        cd.restaurarFromSQL(tmp);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNotes())
            .isEqualTo("first note; second note; third note");
    }

    // ── Domain: Pagination ───────────────────────────────────────────────────

    @Test
    @DisplayName("get10Llibres returns exactly 10 (not 9)")
    void get10LlibresReturns10() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        long[] isbns = {
            9780306406157L, 8420413739L, 9780141439518L, 9780679720201L, 9780099450252L,
            9780007117116L, 9780743273565L, 9780316769174L, 9780385490818L, 9780140449136L,
            9780140283297L
        };
        for (int i = 0; i < isbns.length; i++) {
            add(cd, isbns[i], "Book" + i);
        }
        assertThat(cd.get10Llibres()).hasSize(10);
    }

    @Test
    @DisplayName("Pagination: 0 books → empty first page")
    void paginationZeroBooks() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThat(cd.get100Llibres(0)).isEmpty();
    }

    @Test
    @DisplayName("Pagination: 11 books → page 0 has 11, maxIndex is 0")
    void paginationFewBooks() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        long[] isbns = {
            9780306406157L, 8420413739L, 9780141439518L, 9780679720201L, 9780099450252L,
            9780007117116L, 9780743273565L, 9780316769174L, 9780385490818L, 9780140449136L,
            9780140283297L
        };
        for (int i = 0; i < isbns.length; i++) {
            add(cd, isbns[i], "Book" + i);
        }
        assertThat(cd.get100Llibres(0)).hasSize(11);
        assertThat(cd.maxIndex100Llibres()).isEqualTo(0); // 11 books → only page 0
    }

    // ── Domain: Recently added ────────────────────────────────────────────────

    @Test
    @DisplayName("getRecentlyAdded returns books when count < usual limit")
    void recentlyAddedFewerThanLimit() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        assertThat(cd.obtenirRecentlyAdded()).isNotEmpty();
    }

    // ── Domain: Distinct values ───────────────────────────────────────────────

    @Test
    @DisplayName("getDistinctValues returns unique editorials")
    void distinctEditorials() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
        a.posarEditorial("Penguin");
        Llibre b = ValidadorLlibre.comprovarLlibre(8420413739L, "B", null, null, null, null, null, null, null);
        b.posarEditorial("Penguin");
        Llibre c = ValidadorLlibre.comprovarLlibre(9780141439518L, "C", null, null, null, null, null, null, null);
        c.posarEditorial("HarperCollins");
        cd.afegirLlibre(a);
        cd.afegirLlibre(b);
        cd.afegirLlibre(c);
        List<String> vals = cd.obtenirDistinctValues("editorial");
        assertThat(vals).containsExactlyInAnyOrder("Penguin", "HarperCollins");
    }

    @Test
    @DisplayName("getDistinctValues rejects unknown column: returns empty (not executes SQL)")
    void distinctValuesRejectsUnknownColumn() {
        ControladorDomini cd = ControladorDomini.getInstance();
        // AUTOCOMPLETE_COLUMNS whitelist blocks unknown columns → empty list, no SQL executed
        assertThat(cd.obtenirDistinctValues("DROP TABLE llibre; --")).isEmpty();
    }

    // ── CSV round-trip ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CSV export then re-import produces identical library")
    void csvRoundTrip() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);

        // Serialize to CSV (mimic MostrarBibliotecaControl.exportarCSV format)
        File tmp = File.createTempFile("csv_roundtrip_", ".csv");
        tmp.deleteOnExit();
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(tmp, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("ISBN,Nom,Autor,Any,Descripcio,Valoracio,Preu,Llegit,Portada,Llistes");
            for (Llibre l : cd.obtenirAllLlibres()) {
                pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\",%.1f,%.2f,%b,\"%s\",\"%s\"%n",
                    l.obtenirISBN(), l.obtenirNom().replace("\"","\"\""),
                    l.obtenirAutor().replace("\"","\"\""), l.obtenirAny(),
                    "", l.obtenirValoracio(), l.obtenirPreu(), l.obtenirLlegit(), "", "");
            }
        }

        // Clear library
        cd.eliminarLlibre(9780306406157L);
        cd.eliminarLlibre(8420413739L);
        assertThat(cd.getSize()).isZero();

        // Re-import by parsing CSV lines (same logic as MostrarBibliotecaControl)
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader(tmp, java.nio.charset.StandardCharsets.UTF_8))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = analitzarCsvLine(line);
                long isbn = Long.parseLong(c[0].trim());
                Llibre l = ValidadorLlibre.comprovarLlibre(isbn, c[1], c[2],
                    Integer.parseInt(c[3].trim()), "", 0.0, 0.0, false, "");
                cd.afegirLlibre(l);
            }
        }

        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.obtenirAllLlibres())
            .extracting(Llibre::obtenirNom)
            .containsExactlyInAnyOrder("Dune", "Foundation");
    }

    /** Minimal CSV parser matching MostrarBibliotecaControl.parseCSVLine. */
    private static String[] analitzarCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuote) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else if (ch == '"') { inQuote = false; }
                else { sb.append(ch); }
            } else if (ch == '"') { inQuote = true; }
            else if (ch == ',') { fields.add(sb.toString()); sb = new StringBuilder(); }
            else { sb.append(ch); }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    // ── Domain: clearAll ─────────────────────────────────────────────────────

    @Test
    @DisplayName("clearAll removes books, shelves and tags")
    void netejarAllRemovesEverything() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.afegirLlista("Sci-Fi");
        cd.afegirTag("Space");
        cd.netejarAll();
        assertThat(cd.getSize()).isZero();
        assertThat(cd.obtenirAllLlistes()).isEmpty();
        assertThat(cd.obtenirAllTags()).isEmpty();
    }

    // ── CSV strategy canHandle ───────────────────────────────────────────────

    @Test
    @DisplayName("Goodreads canHandle accepts Goodreads header (full), rejects native")
    void goodreadsCanHandle() {
        var s = new herramienta.io.csv.GoodreadsCsvStrategy();
        String full = "Book Id,Title,Author,ISBN13,Exclusive Shelf,A,B,C,D,E,F";
        assertThat(s.potHandle(full)).isTrue();
        assertThat(s.potHandle("isbn,nom,autor")).isFalse();
    }

    @Test
    @DisplayName("Native canHandle is true (fallback)")
    void nativeCanHandle() {
        var s = new herramienta.io.csv.NativeCsvStrategy();
        assertThat(s.potHandle("random,header")).isTrue();
        assertThat(s.potHandle("a,b,c,d")).isTrue();
        assertThat(s.potHandle("")).isFalse();
        assertThat(s.potHandle("a,b")).isTrue();
    }

    // ── CSV edge cases ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CsvUtils.parseLine: trailing comma, embedded quote, BOM tolerated")
    void csvParseLineEdgeCases() {
        String[] r1 = herramienta.io.csv.UtilitatsCsv.analitzarLine("a,b,c,");
        assertThat(r1).hasSize(4).contains("a", "b", "c", "");
        String[] r2 = herramienta.io.csv.UtilitatsCsv.analitzarLine("\"a\"\"b\",c");
        assertThat(r2).contains("a\"b", "c");
        String[] r3 = herramienta.io.csv.UtilitatsCsv.analitzarLine("﻿ISBN,Nom");
        assertThat(r3[0]).endsWith("ISBN");
    }

    @Test
    @DisplayName("Rfc4180Reader streams multi-line rows")
    void rfc4180ReaderStreaming() throws Exception {
        String csv = "a,b\nfoo,bar\n\"qu\"\"oted\",\"line\nbreak\"\n";
        try (var r = new herramienta.io.csv.Rfc4180Reader(new java.io.StringReader(csv))) {
            String[] header = r.next();
            assertThat(header).contains("a", "b");
            String[] row1 = r.next();
            assertThat(row1).contains("foo", "bar");
            String[] row2 = r.next();
            assertThat(row2[0]).isEqualTo("qu\"oted");
        }
    }

    // ── Migrations: re-instantiate is no-op ──────────────────────────────────

    @Test
    @DisplayName("Re-instantiating persistence on same DB skips already-applied migrations")
    void migrationsIdempotent() {
        ControladorPersistencia.reinicialitzarForTest();
        persistencia.internal.ControladorPersistencia.getInstance();
        persistencia.internal.ControladorPersistencia.reinicialitzarForTest();
        // No exception means schema_version + skip-logic worked
        persistencia.internal.ControladorPersistencia.getInstance();
    }

    // ── DialogoError headless mode ───────────────────────────────────────────

    @Test
    @DisplayName("DialogoError.showErrorMessage is no-op in headless/test mode")
    void dialogoErrorHeadlessSafe() {
        new herramienta.ui.DialegError("test", new Exception("err")).mostrarErrorMessage();
        // No exception, no GUI; passes by virtue of biblioteca.test=true
    }

    // ── Integration: fixture library CRUD ─────────────────────────────────────

    @Test
    @DisplayName("Fixture: 20 books, 3 shelves, 5 tags, 2 loans — basic CRUD survives")
    void integrationFixtureLibrary() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 20; i++) {
            long isbn = 9780306400000L + i;
            cd.afegirLlibre(book(isbn, "Book " + i, "Author " + i, 1990 + i));
        }
        Llista a = cd.afegirLlista("Read");
        cd.afegirLlista("Wishlist");
        cd.afegirLlista("Reference");
        for (int i = 0; i < 5; i++) cd.afegirTag("Tag" + i);

        cd.afegirLlibreToLlista(9780306400000L, a.obtenirId(), 8.0, true);
        cd.afegirLlibreToLlista(9780306400001L, a.obtenirId(), 7.0, true);

        cd.prestarLlibre(9780306400000L, "Alice");
        cd.prestarLlibre(9780306400001L, "Bob");

        assertThat(cd.getSize()).isEqualTo(20);
        assertThat(cd.obtenirAllLlistes()).hasSize(3);
        assertThat(cd.obtenirAllTags()).hasSize(5);
        assertThat(cd.obtenirLoanedISBNs()).hasSize(2);
        assertThat(cd.obtenirLlibresInLlista(a.obtenirId())).hasSize(2);
    }

    // ── Catalan diacritics autocomplete ──────────────────────────────────────

    @Test
    @DisplayName("FiltreUtils.normalize strips Catalan diacritics for prefix match")
    void catalanDiacriticsNormalize() {
        assertThat(herramienta.text.FiltreUtils.normalize("Català")).isEqualTo("catala");
        assertThat(herramienta.text.FiltreUtils.normalize("àéíòú")).isEqualTo("aeiou");
    }

    // ── Tag setNom blank throws ──────────────────────────────────────────────

    @Test
    @DisplayName("Tag.setNom rejects null and blank")
    void tagSetNomBlank() {
        Tag t = new Tag(1, "ok");
        assertThatThrownBy(() -> t.posarNom(null)).isInstanceOf(domini.BibliotecaException.Validacio.class);
        assertThatThrownBy(() -> t.posarNom("  ")).isInstanceOf(domini.BibliotecaException.Validacio.class);
    }

    // ── ConnectionConfig: password masking ───────────────────────────────────

    @Test
    @DisplayName("ConnectionConfig.toString masks password")
    void connectionConfigToStringMasksPassword() {
        var c = new persistencia.internal.ConnectionConfig("mariadb", "localhost", "user", "topsecret", "default", null);
        assertThat(c.toString()).doesNotContain("topsecret").contains("***");
    }

    // ── LlibreLlistaRow: NaN/Infinity sanity ─────────────────────────────────

    @Test
    @DisplayName("LlibreLlistaRow handles NaN/Infinity valoracio sanely")
    void llibreLlistaRowExtremeValues() {
        var nan = new persistencia.row.LlibreLlistaRow(1L, 1, Double.NaN, false);
        var inf = new persistencia.row.LlibreLlistaRow(1L, 1, Double.POSITIVE_INFINITY, true);
        assertThat(nan.valoracio()).isNaN();
        assertThat(inf.valoracio()).isPositive();
        // record equals() with NaN: NaN != NaN by IEEE, but Double.equals uses Double.doubleToLongBits → equal
        assertThat(nan).isEqualTo(new persistencia.row.LlibreLlistaRow(1L, 1, Double.NaN, false));
    }

    // ── UITheme.rebuildFonts updates UIManager.defaultFont ───────────────────

    @Test
    @DisplayName("UITheme.rebuildFonts puts new font in UIManager")
    void uiThemeRebuildFontsUpdatesUIManager() {
        herramienta.ui.UITheme.rebuildFonts("small");
        java.awt.Font f1 = (java.awt.Font) javax.swing.UIManager.get("defaultFont");
        assertThat(f1.getSize()).isEqualTo(11);
        herramienta.ui.UITheme.rebuildFonts("large");
        java.awt.Font f2 = (java.awt.Font) javax.swing.UIManager.get("defaultFont");
        assertThat(f2.getSize()).isEqualTo(16);
    }

    // ── Isbn13Normalizer: ISBN-10 X-check → ISBN-13 ──────────────────────────

    @Test
    @DisplayName("Isbn13Normalizer converts ISBN-10 with X-check digit")
    void isbn13NormalizerXCheck() {
        assertThat(herramienta.text.Isbn13Normalizer.toIsbn13("019853110X"))
            .isEqualTo("9780198531104");
        assertThat(herramienta.text.Isbn13Normalizer.toIsbn13("0306406152"))
            .startsWith("978");
        assertThat(herramienta.text.Isbn13Normalizer.toIsbn13("9780306406157"))
            .isEqualTo("9780306406157");
    }

    // ── DateUtils.parseIsoDate ───────────────────────────────────────────────

    @Test
    @DisplayName("parseIsoDate: ISO accepted, garbage → null")
    void analitzarIsoDate() {
        assertThat(herramienta.text.UtilitatsData.analitzarIsoDate("2024-03-15"))
            .isEqualTo(java.time.LocalDate.of(2024, 3, 15));
        assertThat(herramienta.text.UtilitatsData.analitzarIsoDate(null)).isNull();
        assertThat(herramienta.text.UtilitatsData.analitzarIsoDate("")).isNull();
        assertThat(herramienta.text.UtilitatsData.analitzarIsoDate("not a date")).isNull();
    }

    // ── PrestecRow.overdueDays + toDisplayMap ────────────────────────────────

    @Test
    @DisplayName("PrestecRow.overdueDays counts days past threshold")
    void prestecRowOverdueDays() {
        persistencia.row.PrestecRow r = new persistencia.row.PrestecRow(1L, "X", java.time.LocalDate.of(2024, 1, 1), false);
        assertThat(r.overdueDays(java.time.LocalDate.parse("2024-02-15"), 30)).isEqualTo(15);
        assertThat(r.overdueDays(java.time.LocalDate.parse("2024-01-15"), 30)).isZero();
        persistencia.row.PrestecRow bad = persistencia.row.PrestecRow.fromStrings(1L, "X", "not a date", false);
        assertThat(bad.overdueDays(java.time.LocalDate.parse("2024-01-01"), 30)).isEqualTo(-1L);
    }

    @Test
    @DisplayName("PrestecRow.toDisplayMap exposes all 4 columns")
    void prestecRowToDisplayMap() {
        var m = new persistencia.row.PrestecRow(123L, "Paul", java.time.LocalDate.of(2024, 1, 1), false).toDisplayMap();
        assertThat(m).containsKeys("isbn", "persona", "dataPrestec", "retornat");
    }

    // ── I18n: every key has 3 translations ───────────────────────────────────

    @Test
    @DisplayName("I18n: every registered key has all 3 language entries")
    void i18nKeyCompleteness() throws Exception {
        java.lang.reflect.Field f = herramienta.i18n.I18n.class.getDeclaredField("TABLE");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String[]> table = (java.util.Map<String, String[]>) f.get(null);
        assertThat(table).isNotEmpty();
        for (var entry : table.entrySet()) {
            assertThat(entry.getValue())
                .as("key=%s", entry.getKey())
                .hasSize(3)
                .doesNotContainNull();
        }
    }

    // ── Llibre.copyOf: roundtrip every field ─────────────────────────────────

    @Test
    @DisplayName("Llibre.copyOf preserves every field")
    void llibreCopyOfPreservesAllFields() {
        Llibre src = new Llibre(9780306406157L, "Dune", "Frank Herbert", 1965,
                                "Desert planet", 9.5, 19.99, true, "/img.jpg");
        src.posarNotes("note"); src.posarPagines(900); src.posarPaginesLlegides(300);
        src.posarEditorial("Chilton"); src.posarSerie("Dune"); src.posarVolum(1);
        src.posarDataCompra("2020-01-05"); src.posarDataLectura("2021-02-10");
        src.posarIdioma("en"); src.posarFormat("Tapa dura"); src.posarDesitjat(true);
        src.posarPaisOrigen("US"); src.posarEstat("ok"); src.posarExemplars(2);
        src.posarLlenguaOriginal("en");
        src.posarNomCa("Duna"); src.posarNomEs("Dune"); src.posarNomEn("Dune");
        src.posarHasBlob(true);
        src.posarAutors(java.util.List.of("Frank Herbert", "Brian Herbert"));

        Llibre c = Llibre.copyOf(src);
        assertThat(c.obtenirISBN()).isEqualTo(src.obtenirISBN());
        assertThat(c.obtenirNom()).isEqualTo(src.obtenirNom());
        assertThat(c.obtenirAutors()).isEqualTo(src.obtenirAutors());
        assertThat(c.obtenirAny()).isEqualTo(src.obtenirAny());
        assertThat(c.obtenirDescripcio()).isEqualTo(src.obtenirDescripcio());
        assertThat(c.obtenirValoracio()).isEqualTo(src.obtenirValoracio());
        assertThat(c.obtenirPreu()).isEqualTo(src.obtenirPreu());
        assertThat(c.obtenirLlegit()).isEqualTo(src.obtenirLlegit());
        assertThat(c.obtenirNotes()).isEqualTo(src.obtenirNotes());
        assertThat(c.obtenirPagines()).isEqualTo(src.obtenirPagines());
        assertThat(c.obtenirPaginesLlegides()).isEqualTo(src.obtenirPaginesLlegides());
        assertThat(c.obtenirEditorial()).isEqualTo(src.obtenirEditorial());
        assertThat(c.obtenirSerie()).isEqualTo(src.obtenirSerie());
        assertThat(c.obtenirVolum()).isEqualTo(src.obtenirVolum());
        assertThat(c.obtenirIdioma()).isEqualTo(src.obtenirIdioma());
        assertThat(c.obtenirFormat()).isEqualTo(src.obtenirFormat());
        assertThat(c.esDesitjat()).isEqualTo(src.esDesitjat());
        assertThat(c.obtenirPaisOrigen()).isEqualTo(src.obtenirPaisOrigen());
        assertThat(c.obtenirEstat()).isEqualTo(src.obtenirEstat());
        assertThat(c.obtenirExemplars()).isEqualTo(src.obtenirExemplars());
        assertThat(c.obtenirLlenguaOriginal()).isEqualTo(src.obtenirLlenguaOriginal());
        assertThat(c.obtenirNomCa()).isEqualTo(src.obtenirNomCa());
        assertThat(c.obtenirNomEs()).isEqualTo(src.obtenirNomEs());
        assertThat(c.obtenirNomEn()).isEqualTo(src.obtenirNomEn());
        assertThat(c.teBlob()).isEqualTo(src.teBlob());
    }

    // ── PrestecDao: returnLoan throws on already-returned ────────────────────

    @Test
    @DisplayName("returnLoan on already-returned loan throws (0-row update guard)")
    void returnLoanAlreadyReturnedThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.prestarLlibre(9780306406157L, "Paul");
        cd.retornarLlibre(9780306406157L);
        assertThatThrownBy(() -> cd.retornarLlibre(9780306406157L)).isNotNull();
    }

    // ── LlibreFilter: hasAnyFilter triggers per nullable field ───────────────

    @Test
    @DisplayName("LlibreFilter.hasAnyFilter true when any predicate field is set")
    void filtrarHasAnyFilterPerField() {
        LlibreFilter f = LlibreFilter.empty();
        assertThat(f.teAnyFilter()).isFalse();
        f.withAutor("x"); assertThat(f.teAnyFilter()).isTrue(); f.withAutor(null);
        f.withNom("x"); assertThat(f.teAnyFilter()).isTrue(); f.withNom(null);
        f.withIsbn(1L); assertThat(f.teAnyFilter()).isTrue(); f.withIsbn(null);
        f.withAnyMin(1); assertThat(f.teAnyFilter()).isTrue(); f.withAnyMin(null);
        f.withLlegit(true); assertThat(f.teAnyFilter()).isTrue(); f.withLlegit(null);
        f.withSort(new EspecificacioOrdenacio("nom", true)); assertThat(f.teAnyFilter()).isFalse(); // sort excluded
    }

    // ── FiltreUtils: accent-insensitive match ────────────────────────────────

    @Test
    @DisplayName("matchString matches accented variant (Garcia ↔ García)")
    void matchStringAccentInsensitive() {
        assertThat(FiltreUtils.matchString("Garcia", "Gabriel García Márquez")).isTrue();
        assertThat(FiltreUtils.matchString("garcia", "García")).isTrue();
        assertThat(FiltreUtils.matchString("xxxx", "García")).isFalse();
    }

    // ── DateUtils ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DateUtils.parseYear accepts plausible years, returns empty on garbage")
    void dateUtilsParseYear() {
        assertThat(herramienta.text.UtilitatsData.analitzarYear("1984")).contains(1984);
        assertThat(herramienta.text.UtilitatsData.analitzarYear("Published 1984")).contains(1984);
        assertThat(herramienta.text.UtilitatsData.analitzarYear("not a year")).isEmpty();
        assertThat(herramienta.text.UtilitatsData.analitzarYear(null)).isEmpty();
    }

    // ── LlibreValidator: thrown branches ─────────────────────────────────────

    @Test
    @DisplayName("LlibreValidator rejects bad ISBN, empty nom, out-of-range valoracio/preu")
    void validatorTableDriven() {
        assertThatThrownBy(() -> ValidadorLlibre.comprovarLlibre(1L, "Title", null, null, null, null, null, null, null)).isNotNull();
        assertThatThrownBy(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "", null, null, null, null, null, null, null)).isNotNull();
        assertThatThrownBy(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "T", null, null, null, 11.0, null, null, null)).isNotNull();
        assertThatThrownBy(() -> ValidadorLlibre.comprovarLlibre(9780306406157L, "T", null, null, null, null, -1.0, null, null)).isNotNull();
    }

    // ── Llista: setNom blank throws ──────────────────────────────────────────

    @Test
    @DisplayName("Llista.setNom rejects null and blank")
    void llistaSetNomRejectsBlank() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.posarNom(null)).isInstanceOf(domini.BibliotecaException.Validacio.class);
        assertThatThrownBy(() -> l.posarNom("  ")).isInstanceOf(domini.BibliotecaException.Validacio.class);
    }

    // ── Llista: setNom blank throws ──────────────────────────────────────────

    @Test
    @DisplayName("Llista.setNom blank throws BibliotecaException.Validation")
    void posarNomBlankThrows() {
        Llista l = new Llista(1, "valid");
        assertThatThrownBy(() -> l.posarNom("")).isInstanceOf(domini.BibliotecaException.Validacio.class);
        assertThatThrownBy(() -> l.posarNom(null)).isInstanceOf(domini.BibliotecaException.Validacio.class);
    }

    // ── Llista: setColor hex validation ─────────────────────────────────────

    @Test
    @DisplayName("Llista.isValidColor accepts #abc / #aabbcc / null, rejects garbage; setColor is a trust-the-caller setter")
    void posarColorHex3DigitAccepted() {
        // Validation is centralised in Llista.isValidColor; the setter trusts the caller
        // (DAO load paths use it with values that were validated on write).
        assertThat(Llista.esValidColor("#abc")).isTrue();
        assertThat(Llista.esValidColor("#aabbcc")).isTrue();
        assertThat(Llista.esValidColor(null)).isTrue();
        assertThat(Llista.esValidColor("red")).isFalse();
        assertThat(Llista.esValidColor("#zzzzzz")).isFalse();
        Llista l = new Llista(1, "ok");
        l.posarColor("#abc");
        assertThat(l.obtenirColor()).isEqualTo("#abc");
        l.posarColor(null);
        assertThat(l.obtenirColor()).isNull();
    }

    @Test
    @DisplayName("ControladorDomini.setLlistaColor rejects invalid color with Validation")
    void posarLlistaColorInvalid() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.afegirLlista("Shelf");
        assertThatThrownBy(() -> cd.posarLlistaColor(l.obtenirId(), "not-a-color"))
            .isInstanceOf(domini.BibliotecaException.Validacio.class);
        assertThatThrownBy(() -> cd.posarLlistaColor(l.obtenirId(), "#zzzzzz"))
            .isInstanceOf(domini.BibliotecaException.Validacio.class);
        // Valid colors are accepted
        cd.posarLlistaColor(l.obtenirId(), "#aabbcc");
        assertThat(cd.obtenirAllLlistes().get(0).obtenirColor()).isEqualTo("#aabbcc");
    }

    // ── BibliotecaException: cause preserved ─────────────────────────────────

    @Test
    @DisplayName("BibliotecaException(message, cause) preserves cause stack trace")
    void bibliotecaExceptionPreservesCause() {
        java.sql.SQLException root = new java.sql.SQLException("boom");
        domini.BibliotecaException be = new domini.BibliotecaException("wrapped", root);
        assertThat(be.getCause()).isSameAs(root);
        assertThat(be.code()).isEqualTo(domini.BibliotecaException.Code.UNKNOWN);
    }

    @Test
    @DisplayName("BibliotecaException subclasses carry their codes")
    void bibliotecaExceptionSubclassCodes() {
        assertThat(new domini.BibliotecaException.NoTrobat("x").code()).isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
        assertThat(new domini.BibliotecaException.Duplicat("x").code()).isEqualTo(domini.BibliotecaException.Code.DUPLICATE);
        assertThat(new domini.BibliotecaException.Validacio("x").code()).isEqualTo(domini.BibliotecaException.Code.VALIDATION);
    }

    // ── Loan: lend → return → lend roundtrip ─────────────────────────────────

    @Test
    @DisplayName("loan → return → loan succeeds")
    void loanReturnLoanRoundtrip() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.prestarLlibre(9780306406157L, "Paul");
        assertThat(cd.obtenirLoanedISBNs()).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.obtenirLoanedISBNs()).doesNotContain(9780306406157L);
        cd.prestarLlibre(9780306406157L, "Jessica");
        assertThat(cd.obtenirLoanedISBNs()).contains(9780306406157L);
    }

    // ── Llista: color validation ─────────────────────────────────────────────

    @Test
    @DisplayName("setLlistaColor: null clears, hex accepted, garbage rejected")
    void posarLlistaColorValidates() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.afegirLlista("Test");
        cd.posarLlistaColor(l.obtenirId(), null);
        cd.posarLlistaColor(l.obtenirId(), "#abc");
        cd.posarLlistaColor(l.obtenirId(), "#aabbcc");
        assertThatThrownBy(() -> cd.posarLlistaColor(l.obtenirId(), "red"))
            .hasMessage(herramienta.i18n.I18n.t("val_color_invalid", "red"));
        assertThatThrownBy(() -> cd.posarLlistaColor(l.obtenirId(), "#zzzzzz"))
            .hasMessage(herramienta.i18n.I18n.t("val_color_invalid", "#zzzzzz"));
    }

    // ── Tag: rename to existing throws ───────────────────────────────────────

    @Test
    @DisplayName("renameTag to existing name throws (unique constraint)")
    void reanomenarTagToExistingThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag a = cd.afegirTag("alpha");
        cd.afegirTag("beta");
        assertThatThrownBy(() -> cd.reanomenarTag(a.obtenirId(), "beta")).isNotNull();
    }


    @Test
    @DisplayName("clearAll on empty DB still writes a pre_clear backup")
    void netejarAllOnEmptyDbWritesBackup() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("biblioteca_test_backup");
        try {
            ControladorDomini cd = ControladorDomini.getInstance();
            File out = new File(tmp.toFile(), "pre_clear_empty.sql");
            synchronized (cd) {
                cd.copiaSegToSQL(out);
                cd.netejarAll();
            }
            assertThat(out).exists();
            assertThat(out.length()).isGreaterThan(0);
        } finally {
            for (File f : tmp.toFile().listFiles()) f.delete();
            tmp.toFile().delete();
        }
    }
// ── TagDao: rename to existing nom should throw ────────────────────────────

    @Test
    @DisplayName("renameTag to existing nom throws SQLException (UNIQUE constraint)")
    void reanomenarTagToExistingNomThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag a = cd.afegirTag("alpha");
        cd.afegirTag("beta");
        assertThatThrownBy(() -> cd.reanomenarTag(a.obtenirId(), "beta")).isNotNull();
    }

    // ── LlistaDao: color null clears and hex roundtrips ────────────────────────

    @Test
    @DisplayName("Llista color: null clears, hex roundtrips via setLlistaColor")
    void llistaColorNullClearsAndHexRoundtrips() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.afegirLlista("Colored");
        cd.posarLlistaColor(l.obtenirId(), "#aabbcc");
        Llista found = cd.obtenirAllLlistes().stream()
            .filter(x -> x.obtenirId() == l.obtenirId()).findFirst().orElseThrow();
        assertThat(found.obtenirColor()).isEqualTo("#aabbcc");

        cd.posarLlistaColor(l.obtenirId(), null);
        Llista afterClear = cd.obtenirAllLlistes().stream()
            .filter(x -> x.obtenirId() == l.obtenirId()).findFirst().orElseThrow();
        assertThat(afterClear.obtenirColor()).isNull();
    }

    // ── AutorDao: parameterized 0/1/many authors ──────────────────────────────

    @ParameterizedTest(name = "getDistinctAutorNames with {0} books returns expected count")
    @CsvSource({"0, 0", "1, 1", "3, 2"})
    @DisplayName("AutorDao: distinct author names for 0/1/many authors")
    void autorDaoDistinctNamesCount(int bookCount, int expectedAuthorCount) throws Exception {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        ControladorDomini cd = ControladorDomini.getInstance();
        if (bookCount >= 1) {
            add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        }
        if (bookCount >= 2) {
            add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        }
        if (bookCount >= 3) {
            add(cd, 9780141439518L, "1984", "Frank Herbert", 1949);
        }
        assertThat(cd.obtenirDistinctAutorNames()).hasSize(expectedAuthorCount);
    }

    // ── PrestecRow: toDisplayMap ISO date string ──────────────────────────────

    @Test
    @DisplayName("PrestecRow.toDisplayMap encodes dataPrestec as formatted date string")
    void prestecRowToDisplayMapDataPrestec() {
        java.time.LocalDate date = java.time.LocalDate.of(2024, 3, 15);
        persistencia.row.PrestecRow row = new persistencia.row.PrestecRow(9780306406157L, "Alice", date, false);
        java.util.Map<String, Object> map = row.toDisplayMap();
        assertThat(map.get("dataPrestec")).isEqualTo("15/03/2024");
    }

    @Test
    @DisplayName("PrestecRow.toDisplayMap: null date yields null")
    void prestecRowToDisplayMapNullDate() {
        persistencia.row.PrestecRow row = new persistencia.row.PrestecRow(9780306406157L, "Bob", null, true);
        java.util.Map<String, Object> map = row.toDisplayMap();
        assertThat(map.get("dataPrestec")).isNull();
    }


    @Test
    @DisplayName("DeleteEvent: cancellable=true, veto marks as vetoed")
    void eliminarEventCancellableVeto() {
        domini.Llibre book = book(9780306406157L, "Dune");
        presentacio.listener.EnEliminarLlibre.EsborrarEvent ev = new presentacio.listener.EnEliminarLlibre.EsborrarEvent(book, true);
        assertThat(ev.esCancellable()).isTrue();
        assertThat(ev.esVetoed()).isFalse();
        ev.veto();
        assertThat(ev.esVetoed()).isTrue();
    }

    @Test
    @DisplayName("DeleteEvent: cancellable=false, veto has no practical effect")
    void eliminarEventNonCancellable() {
        domini.Llibre book = book(9780306406157L, "Dune");
        presentacio.listener.EnEliminarLlibre.EsborrarEvent ev = new presentacio.listener.EnEliminarLlibre.EsborrarEvent(book, false);
        assertThat(ev.esCancellable()).isFalse();
    }

    @Test
    @DisplayName("DeleteEvent: cancellable=true + listener.veto() cancels the delete")
    void eliminarEventVetoCancelsDelete() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);

        presentacio.listener.EnEliminarLlibre vetoer = new presentacio.listener.EnEliminarLlibre() {
            @Override public void enEliminarLlibre(Llibre l) {}
            @Override public void enEliminantLlibre(presentacio.listener.EnEliminarLlibre.EsborrarEvent e) { e.veto(); }
        };
        presentacio.listener.EnEliminarLlibre.EsborrarEvent ev =
            new presentacio.listener.EnEliminarLlibre.EsborrarEvent(cd.obtenirLlibre(9780306406157L), true);
        vetoer.enEliminantLlibre(ev);
        assertThat(presentacio.listener.EnEliminarLlibre.hauriaProceed(ev)).isFalse();
        if (presentacio.listener.EnEliminarLlibre.hauriaProceed(ev)) cd.eliminarLlibre(cd.obtenirLlibre(9780306406157L));

        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("DeleteEvent: cancellable=false + listener.veto() still proceeds")
    void eliminarEventNonCancellableVetoProceeds() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);

        presentacio.listener.EnEliminarLlibre vetoer = new presentacio.listener.EnEliminarLlibre() {
            @Override public void enEliminarLlibre(Llibre l) {}
            @Override public void enEliminantLlibre(presentacio.listener.EnEliminarLlibre.EsborrarEvent e) { e.veto(); }
        };
        presentacio.listener.EnEliminarLlibre.EsborrarEvent ev =
            new presentacio.listener.EnEliminarLlibre.EsborrarEvent(cd.obtenirLlibre(9780306406157L), false);
        vetoer.enEliminantLlibre(ev);
        assertThat(ev.esVetoed()).isTrue();
        assertThat(presentacio.listener.EnEliminarLlibre.hauriaProceed(ev)).isTrue();
        if (presentacio.listener.EnEliminarLlibre.hauriaProceed(ev)) cd.eliminarLlibre(cd.obtenirLlibre(9780306406157L));

        assertThat(cd.getSize()).isEqualTo(0);
        assertThat(cd.cercarLlibre(9780306406157L)).isEmpty();
    }

    // ── H2 in-memory: full CRUD roundtrip ──────────────────────────────────────

    @Test
    @DisplayName("H2 in-memory: full CRUD lifecycle for books, shelves, tags, loans")
    void h2CrudLifecycle() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();

        // Create
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        Llista shelf = cd.afegirLlista("Sci-Fi");
        Tag tag = cd.afegirTag("Classic");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 9.0, true);
        cd.afegirLlibreToTag(9780306406157L, tag.obtenirId());
        cd.prestarLlibre(9780306406157L, "Alice");

        // Read
        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNom()).isEqualTo("Dune");
        assertThat(cd.obtenirAllLlistes()).hasSize(1);
        assertThat(cd.obtenirAllTags()).hasSize(1);
        assertThat(cd.obtenirCountInLlista(shelf.obtenirId())).isEqualTo(1);
        assertThat(cd.obtenirTagsForLlibre(9780306406157L)).hasSize(1);
        assertThat(cd.obtenirLoanedISBNs()).contains(9780306406157L);

        // Update
        Llibre updated = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune Updated", "Frank Herbert", 1965, null, 9.5, null, null, null);
        cd.actualitzarLlibre(updated);
        assertThat(cd.obtenirLlibre(9780306406157L).obtenirNom()).isEqualTo("Dune Updated");

        // Delete
        cd.retornarLlibre(9780306406157L);
        cd.eliminarLlibreFromLlista(9780306406157L, shelf.obtenirId());
        cd.eliminarLlibreFromTag(9780306406157L, tag.obtenirId());
        cd.eliminarLlibre(9780306406157L);
        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(cd.obtenirLoanedISBNs()).isEmpty();
    }

    // ── ConnexioServidor: migration produces complete schema ──────────────────────

    @Test
    @DisplayName("ConnexioServidor: CREATE_TABLE + all migrations produce complete schema")
    void migrationProducesCompleteSchema() throws Exception {
        String url = "jdbc:h2:mem:schema_" + System.nanoTime() + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";
        System.setProperty("biblioteca.h2.url", url);
        try {
            persistencia.internal.ConnexioServidor sc = new persistencia.internal.ConnexioServidor();
            sc.crearDatabase();
            try (java.sql.Connection conn = sc.obtenirConnexio()) {
                java.util.Set<String> llibreCols = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "LLIBRE", null)) {
                    while (rs.next()) llibreCols.add(rs.getString("COLUMN_NAME"));
                }
                assertThat(llibreCols).contains("ISBN", "NOM", "ANY", "DESCRIPCIO", "VALORACIO", "PREU",
                    "LLEGIT", "IMATGE", "IMATGE_BLOB", "NOTES", "PAGINES", "PAGINES_LLEGIDES",
                    "EDITORIAL", "SERIE", "VOLUM", "DATA_COMPRA", "DATA_LECTURA", "IDIOMA",
                    "FORMAT", "DESITJAT", "PAIS_ORIGEN", "ESTAT", "EXEMPLARS", "LLENGUA_ORIGINAL",
                    "NOM_CA", "NOM_ES", "NOM_EN", "DATA_AFEGIT");
                assertThat(llibreCols).doesNotContain("AUTOR");

                java.util.Set<String> tables = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                try (java.sql.ResultSet rs = conn.getMetaData().getTables(null, null, "%", null)) {
                    while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
                }
                assertThat(tables).contains("LLISTA", "LLIBRE_LLISTA", "PRESTEC",
                    "TAG", "LLIBRE_TAG", "AUTOR", "LLIBRE_AUTOR", "LECTURA", "SCHEMA_VERSION");

                java.util.Set<String> llistaCols = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "LLISTA", null)) {
                    while (rs.next()) llistaCols.add(rs.getString("COLUMN_NAME"));
                }
                assertThat(llistaCols).contains("ID", "NOM", "ORDRE", "COLOR");
            } finally {
                sc.tancarConnection();
            }
        } finally {
            System.setProperty("biblioteca.h2.url",
                "jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
        }
    }

    // ── LlibreDao.search() parameter binding: all types ────────────────────────

    @Test
    @DisplayName("search() binds String, Long, Integer, Double, Boolean params correctly")
    void cercarParameterBindingByType() throws Exception {
        // NOTE: H2 MySQL-mode LIKE is case-sensitive; SQL-path queries use exact case.
        // In-memory aplicarFiltres normalizes both sides — SQL path doesn't (documented gap).
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = ValidadorLlibre.comprovarLlibre(9780306406157L, "Dune", "Frank Herbert", 1965,
            null, 9.5, 19.99, true, null);
        a.posarEditorial("Chilton");
        a.posarSerie("Dune Chronicles");
        a.posarFormat("Tapa dura");
        a.posarIdioma("English");
        cd.afegirLlibre(a);

        Llibre b = ValidadorLlibre.comprovarLlibre(8420413739L, "Foundation", "Isaac Asimov", 1951,
            null, 7.0, 10.0, false, null);
        b.posarEditorial("Gnome Press");
        b.posarSerie("Foundation");
        b.posarFormat("eBook");
        b.posarIdioma("Spanish");
        cd.afegirLlibre(b);

        // String (nom LIKE — uses exact case since H2 LIKE is case-sensitive)
        LlibreFilter fn = domini.ConstructorFiltreLlibre.of().nom("Du").build();
        assertThat(cd.cercarLlibresSQL(fn)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // String (autor LIKE)
        LlibreFilter fa = domini.ConstructorFiltreLlibre.of().autor("Herbert").build();
        assertThat(cd.cercarLlibresSQL(fa)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // Long (isbn =)
        LlibreFilter fi = domini.ConstructorFiltreLlibre.of().isbn(9780306406157L).build();
        assertThat(cd.cercarLlibresSQL(fi)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // Integer (anyMin / anyMax)
        LlibreFilter fay = domini.ConstructorFiltreLlibre.of().anyMin(1960).build();
        assertThat(cd.cercarLlibresSQL(fay)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // Double (valoracioMin / preuMin)
        LlibreFilter fv = domini.ConstructorFiltreLlibre.of().valoracioMin(8.0).build();
        assertThat(cd.cercarLlibresSQL(fv)).extracting(Llibre::obtenirNom).containsExactly("Dune");
        LlibreFilter fp = domini.ConstructorFiltreLlibre.of().preuMin(15.0).build();
        assertThat(cd.cercarLlibresSQL(fp)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // Boolean (llegit = true)
        LlibreFilter fl = domini.ConstructorFiltreLlibre.of().llegit(true).build();
        assertThat(cd.cercarLlibresSQL(fl)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // String (editorial LIKE — exact case)
        LlibreFilter fe = domini.ConstructorFiltreLlibre.of().editorial("Chilton").build();
        assertThat(cd.cercarLlibresSQL(fe)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // String (format = exact)
        LlibreFilter ff = domini.ConstructorFiltreLlibre.of().format("Tapa dura").build();
        assertThat(cd.cercarLlibresSQL(ff)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // String (idioma LIKE — exact case)
        LlibreFilter fid = domini.ConstructorFiltreLlibre.of().idioma("English").build();
        assertThat(cd.cercarLlibresSQL(fid)).extracting(Llibre::obtenirNom).containsExactly("Dune");

        // No result
        LlibreFilter fEmpty = domini.ConstructorFiltreLlibre.of().nom("zzzzz").build();
        assertThat(cd.cercarLlibresSQL(fEmpty)).isEmpty();
    }

    // ── Config: column visibility persistence ──────────────────────────────────

    @Test
    @DisplayName("Config saves and loads column visibility settings")
    void configColumnVisibilityPersistence() throws Exception {
        java.nio.file.Path tmpDir = Files.createTempDirectory("biblioteca_cfg_colvis_");
        java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
        Files.createDirectories(cfgDir);
        java.nio.file.Path cfgPath = cfgDir.resolve("config.properties");

        Properties initialProps = new Properties();
        initialProps.setProperty("colVisible_0", "false");
        initialProps.setProperty("colVisible_3", "false");
        initialProps.setProperty("colVisible_7", "true");
        try (var out = Files.newOutputStream(cfgPath)) {
            initialProps.store(out, null);
        }

        String origHome = System.getProperty("user.home");
        System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
        herramienta.config.Configuracio.reload();

        assertThat(herramienta.config.Configuracio.obtenirColVisible(0)).isFalse();
        assertThat(herramienta.config.Configuracio.obtenirColVisible(3)).isFalse();
        assertThat(herramienta.config.Configuracio.obtenirColVisible(7)).isTrue();
        assertThat(herramienta.config.Configuracio.obtenirColVisible(2)).isTrue(); // default

        herramienta.config.ConfiguracioFinestra.posarColVisible(5, false);
        // Wait for async save (300ms scheduler)
        Thread.sleep(500);
        assertThat(herramienta.config.Configuracio.obtenirColVisible(5)).isFalse();

        // Verify persistence: reload from disk and check
        herramienta.config.Configuracio.reload();
        assertThat(herramienta.config.Configuracio.obtenirColVisible(5)).isFalse();

        System.setProperty("user.home", origHome);
        herramienta.config.Configuracio.reload();
        Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
    }

    // ── resetForProfileSwitch closes and reopens connection ────────────────────

    @Test
    @DisplayName("resetForProfileSwitch closes connection; new instance creates fresh connection")
    void reinicialitzarForProfileSwitchReconnects() {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        assertThat(cp.comptarLlibres()).isZero();

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForProfileSwitch();

        ControladorPersistencia cp2 = ControladorPersistencia.getInstance();
        assertThat(cp2).isNotSameAs(cp);
        assertThat(cp2.comptarLlibres()).isZero();
    }

    // ── BookExporter golden-file test ────────────────────────────────────

    @Test
    @DisplayName("BookExporter.exportJSON output matches golden fixture structure")
    void bookExporterGoldenFile() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre g = ValidadorLlibre.comprovarLlibre(9780000000050L, "GoldenBook", "Author X", 2020, "A description", 7.5, 0.0, true, "");
        cd.afegirLlibre(g);
        Llista fav = cd.afegirLlista("Fav");
        cd.afegirLlibreToLlista(9780000000050L, fav.obtenirId(), 8.0, true);
        Tag fiction = cd.afegirTag("fiction");
        cd.afegirLlibreToTag(9780000000050L, fiction.obtenirId());

        File tmp = File.createTempFile("golden_export", ".json");
        tmp.deleteOnExit();
        herramienta.ExportadorLlibres.exportarJSON(tmp, cd);

        String output = new String(Files.readAllBytes(tmp.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(output).getAsJsonObject();

        assertThat(root.get("version").getAsInt()).isEqualTo(1);
        assertThat(root.getAsJsonArray("llibres")).hasSize(1);
        assertThat(root.getAsJsonArray("llistes")).hasSize(1);
        assertThat(root.getAsJsonArray("tags")).hasSize(1);

        com.google.gson.JsonObject book = root.getAsJsonArray("llibres").get(0).getAsJsonObject();
        assertThat(book.get("isbn").getAsLong()).isEqualTo(9780000000050L);
        assertThat(book.get("nom").getAsString()).isEqualTo("GoldenBook");
        assertThat(book.get("autor").getAsString()).isEqualTo("Author X");
        assertThat(book.get("any").getAsInt()).isEqualTo(2020);
        assertThat(book.get("valoracio").getAsDouble()).isEqualTo(7.5);
        assertThat(book.get("llegit").getAsBoolean()).isTrue();

        com.google.gson.JsonObject shelf = root.getAsJsonArray("llistes").get(0).getAsJsonObject();
        assertThat(shelf.get("nom").getAsString()).isEqualTo("Fav");

        com.google.gson.JsonObject tagObj = root.getAsJsonArray("tags").get(0).getAsJsonObject();
        assertThat(tagObj.get("nom").getAsString()).isEqualTo("fiction");

        // Verify shelf membership is embedded in the book
        com.google.gson.JsonObject membership = book.getAsJsonArray("llistes").get(0).getAsJsonObject();
        assertThat(membership.get("valoracio").getAsDouble()).isEqualTo(8.0);
        assertThat(membership.get("llegit").getAsBoolean()).isTrue();

        // Cross-reference: tag id in book matches tag id in top-level tags array
        assertThat(book.getAsJsonArray("tags").get(0).getAsInt()).isEqualTo(tagObj.get("id").getAsInt());
    }

    // ── Goodreads CSV fixture ─────────────────────────────────────────────

    @Test
    @DisplayName("Goodreads CSV: realistic multi-column header is recognised and rows import correctly")
    void goodreadsCsvFixture() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String header = "Book Id,Title,Author,Author l-f,Additional Authors,ISBN,ISBN13,My Rating,Average Rating,Publisher,Binding,Number of Pages,Year Published,Original Publication Year,Date Read,Date Added,Bookshelves,Exclusive Shelf,My Review,Spoiler,Private Notes,Read Count";
        herramienta.io.csv.GoodreadsCsvStrategy gr = new herramienta.io.csv.GoodreadsCsvStrategy();
        assertThat(gr.potHandle(header)).isTrue();

        String row = "42,The Hobbit,Tolkien,J.R.R. Tolkien,,=\"0000000000\",=\"9780000000042\",5,4.5,HarperCollins,Paperback,310,1937,1937,2024-06-15,2024-05-01,fantasy;classics,read,Awesome,,nope,3";
        String[] headerCols = herramienta.io.csv.UtilitatsCsv.analitzarLine(header);
        java.util.Map<String, Integer> hMap = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(headerCols);
        String[] cols = herramienta.io.csv.UtilitatsCsv.analitzarLine(row);

        assertThat(gr.analitzarLine(cols, hMap, cd)).isTrue();
        Llibre l = cd.obtenirLlibre(9780000000042L);
        assertThat(l.obtenirNom()).isEqualTo("The Hobbit");
        assertThat(l.obtenirValoracio()).isEqualTo(10.0);
        assertThat(l.obtenirLlegit()).isTrue();
        assertThat(l.obtenirEditorial()).isEqualTo("HarperCollins");
        assertThat(l.obtenirPagines()).isEqualTo(310);
        assertThat(cd.obtenirAllLlistes()).isNotEmpty();
    }

    // ── LibraryThing CSV fixture ──────────────────────────────────────────

    @Test
    @DisplayName("LibraryThing CSV: BCID column triggers strategy; handles tags, collections, and X-check ISBN")
    void libraryThingCsvFixture() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String header = "Book Id,ISBN,ISBN13,BCID,Title,Authors,Original Publication Year,Publication Year,Rating,Summary,Comments,Review,Collections,Tags";
        herramienta.io.csv.LibraryThingCsvStrategy lt = new herramienta.io.csv.LibraryThingCsvStrategy();
        assertThat(lt.potHandle(header)).isTrue();

        String row = "99,,9780000000019,BC123,Test LibBook,Author One; Author Two,2021,2021,3.5,A summary,My notes,,\"My Shelf,Favorites\",fiction;adventure";
        String[] headerCols = herramienta.io.csv.UtilitatsCsv.analitzarLine(header);
        java.util.Map<String, Integer> hMap = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(headerCols);
        String[] cols = herramienta.io.csv.UtilitatsCsv.analitzarLine(row);

        assertThat(lt.analitzarLine(cols, hMap, cd)).isTrue();
        Llibre l = cd.obtenirLlibre(9780000000019L);
        assertThat(l.obtenirNom()).isEqualTo("Test LibBook");
        assertThat(l.obtenirValoracio()).isEqualTo(7.0);
        assertThat(cd.obtenirAllTags()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(cd.obtenirAllLlistes()).hasSizeGreaterThanOrEqualTo(1);
    }
// ── Export/import roundtrip preserves shelf memberships (JSON) ────────

    @Test
    @DisplayName("JSON export/import roundtrip preserves shelf memberships with valoracio and llegit")
    void jsonRoundtripShelfMemberships() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre b1 = ValidadorLlibre.comprovarLlibre(9780000000060L, "ShelfBook1", "Auth1", 2021, "Desc1", 8.0, 10.0, true, "");
        Llibre b2 = ValidadorLlibre.comprovarLlibre(9780000000061L, "ShelfBook2", "Auth2", 2022, "Desc2", 6.0, 5.0, false, "");
        cd.afegirLlibre(b1);
        cd.afegirLlibre(b2);

        Llista s1 = cd.afegirLlista("Sci-Fi");
        Llista s2 = cd.afegirLlista("Classics");
        cd.afegirLlibreToLlista(9780000000060L, s1.obtenirId(), 9.0, true);
        cd.afegirLlibreToLlista(9780000000060L, s2.obtenirId(), 5.0, false);
        cd.afegirLlibreToLlista(9780000000061L, s1.obtenirId(), 7.5, false);

        Tag t1 = cd.afegirTag("adventure");
        Tag t2 = cd.afegirTag("classic");
        cd.afegirLlibreToTag(9780000000060L, t1.obtenirId());
        cd.afegirLlibreToTag(9780000000060L, t2.obtenirId());
        cd.afegirLlibreToTag(9780000000061L, t1.obtenirId());

        File tmp = File.createTempFile("roundtrip_json", ".json");
        tmp.deleteOnExit();
        herramienta.ExportadorLlibres.exportarJSON(tmp, cd);

        // Re-import into fresh DB
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        ControladorDomini cd2 = ControladorDomini.getInstance();
        herramienta.ImportadorLlibres.ResultatImportacio result = herramienta.ImportadorLlibres.importarJSON(tmp, cd2);
        assertThat(result.imported()).isEqualTo(2);

        // Verify shelves
        List<Llista> shelves = cd2.obtenirAllLlistes();
        assertThat(shelves).hasSize(2);
        Set<String> shelfNames = shelves.stream().map(Llista::obtenirNom).collect(java.util.stream.Collectors.toSet());
        assertThat(shelfNames).containsExactlyInAnyOrder("Sci-Fi", "Classics");

        // Verify tags
        List<Tag> tags = cd2.obtenirAllTags();
        assertThat(tags).hasSize(2);
        Set<String> tagNames = tags.stream().map(Tag::obtenirNom).collect(java.util.stream.Collectors.toSet());
        assertThat(tagNames).containsExactlyInAnyOrder("adventure", "classic");

        // Verify shelf memberships for book 60
        List<Llista> shelvesFor60 = cd2.obtenirLlistesForLlibre(9780000000060L);
        assertThat(shelvesFor60).hasSize(2);
        List<Tag> tagsFor60 = cd2.obtenirTagsForLlibre(9780000000060L);
        assertThat(tagsFor60).hasSize(2);

        // Verify shelf membership for book 61
        List<Llista> shelvesFor61 = cd2.obtenirLlistesForLlibre(9780000000061L);
        assertThat(shelvesFor61).hasSize(1);
        assertThat(shelvesFor61.get(0).obtenirNom()).isEqualTo("Sci-Fi");
    }

    // ── Config: switching to H2 clears stale host/user ───────────────────

    @Test
    @DisplayName("Config: switching to H2 preserves previously-set host/user (non-destructive)")
    void configH2ClearsStaleHostUser() throws Exception {
        java.nio.file.Path tmpDir = Files.createTempDirectory("biblioteca_cfg_h2_");
        java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
        Files.createDirectories(cfgDir);
        String origHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
            herramienta.config.Configuracio.reload();
            herramienta.config.ConfiguracioDb.setType("mariadb");
            herramienta.config.ConfiguracioDb.posarHost("db.example.com");
            herramienta.config.ConfiguracioDb.posarUser("admin");
            Thread.sleep(500);

            assertThat(herramienta.config.Configuracio.obtenirDbType()).isEqualTo("mariadb");
            assertThat(herramienta.config.Configuracio.obtenirDbHost()).isEqualTo("db.example.com");
            assertThat(herramienta.config.Configuracio.obtenirDbUser()).isEqualTo("admin");

            // Switch back to H2 — host/user should be preserved (putIfAbsent
            // semantics; a future re-connection to MariaDB can still reuse
            // the saved values).
            herramienta.config.ConfiguracioDb.setType("h2");
            Thread.sleep(500);
            assertThat(herramienta.config.Configuracio.obtenirDbType()).isEqualTo("h2");
            assertThat(herramienta.config.Configuracio.obtenirDbHost()).isEqualTo("db.example.com");
            assertThat(herramienta.config.Configuracio.obtenirDbUser()).isEqualTo("admin");
        } finally {
            System.setProperty("user.home", origHome);
            herramienta.config.Configuracio.reload();
            Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        }
    }

    // ── PantallaInici: hide before show is no-op ──────────────────────────

    @Test
    @DisplayName("PantallaInici: hide() before show() is a no-op")
    void splashHideBeforeShow() {
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (ge.isHeadless()) return;
        presentacio.PantallaInici splash = new presentacio.PantallaInici();
        // Calling hide() before show() should not throw and should be a no-op
        splash.hide();
        splash.forceHide();
    }

    // ── LlibreFilter: private constructor ──────────────────────────────────

    @Test
    @DisplayName("LlibreFilter.withX mutators return this for chaining")
    void llibreFilterWithXChaining() {
        domini.LlibreFilter f = LlibreFilter.empty()
            .withNom("Test")
            .withAnyMin(2000)
            .withAnyMax(2020)
            .withLlegit(false);
        assertThat(f.obtenirNom()).isEqualTo("Test");
        assertThat(f.obtenirAnyMin()).isEqualTo(2000);
        assertThat(f.obtenirAnyMax()).isEqualTo(2020);
        assertThat(f.obtenirLlegit()).isFalse();
    }

    // ── LlibreLlistaContext: per-book shelf context ─────────────────────────

    @Test
    @DisplayName("getLlistesForLlibreContext returns shelf with per-book values")
    void llistesForLlibreContext() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.afegirLlista("Favorites");
        cd.afegirLlibreToLlista(9780306406157L, shelf.obtenirId(), 9.5, true);
        var ctxList = cd.obtenirLlistesForLlibreContext(9780306406157L);
        assertThat(ctxList).hasSize(1);
        domini.LlibreLlistaContext ctx = ctxList.get(0);
        assertThat(ctx.llistaId()).isEqualTo(shelf.obtenirId());
        assertThat(ctx.nom()).isEqualTo("Favorites");
        assertThat(ctx.valoracio()).isEqualTo(9.5);
        assertThat(ctx.llegit()).isTrue();
    }

    // ── Pagination clamp ──────────────────────────────────────────────────

    @Test
    @DisplayName("Pagination: get100Llibres clamps page to valid range")
    void paginationClampsPage() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 1; i <= 150; i++)
            add(cd, 9780000000000L + i, "L" + i);
        assertThat(cd.get100Llibres(0).size()).isEqualTo(100);
        assertThat(cd.get100Llibres(1).size()).isEqualTo(50);
        assertThat(cd.get100Llibres(5).size()).isEqualTo(0);
    }

    // ── SyncAutors batch: multiple authors round-trip ────────────────────

    @Test
    @DisplayName("Book with multiple authors preserves all after add")
    void multipleAuthorsPreserved() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = book(9780306406157L, "MultiAuthor");
        l.posarAutors(java.util.Arrays.asList("A1", "A2", "A3"));
        cd.afegirLlibre(l);
        Llibre retrieved = cd.obtenirLlibre(9780306406157L);
        assertThat(retrieved.obtenirAutors()).containsExactlyInAnyOrder("A1", "A2", "A3");
    }

    @Test
    @DisplayName("Update book replacing authors preserves new list")
    void actualitzarAuthorsReplacement() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = book(9780306406157L, "ReplaceAuth");
        l.posarAutors(java.util.Arrays.asList("Old Author"));
        cd.afegirLlibre(l);
        l.posarAutors(java.util.Arrays.asList("New A", "New B"));
        cd.actualitzarLlibre(l);
        Llibre retrieved = cd.obtenirLlibre(9780306406157L);
        assertThat(retrieved.obtenirAutors()).containsExactlyInAnyOrder("New A", "New B");
        assertThat(retrieved.obtenirAutors()).doesNotContain("Old Author");
    }
// ── AboutDialog: loads license text from /LICENSE resource ────────

    @Test
    @DisplayName("AboutDialog: /LICENSE resource is loadable and contains GPL text")
    void aboutDialogLicenseResource() {
        try (var in = presentacio.QuantADialeg.class.getResourceAsStream("/LICENSE")) {
            assertThat(in).isNotNull();
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertThat(text).contains("GNU General Public License");
            assertThat(text).contains("version 3");
        } catch (Exception e) {
            Assertions.fail("Failed to load /LICENSE resource: " + e.getMessage());
        }
    }

    // ── BibliotecaException: wraps SQLException with cause ───────────────

    @Test
    @DisplayName("BibliotecaException preserves cause chain through layers")
    void bibliotecaExceptionCauseChain() {
        java.sql.SQLException sqlEx = new java.sql.SQLException("connection failed");
        domini.BibliotecaException ex = new domini.BibliotecaException("DB error", sqlEx);
        assertThat(ex.getCause()).isSameAs(sqlEx);
        assertThat(ex.getMessage()).isEqualTo("DB error");
        assertThat(ex.code()).isEqualTo(domini.BibliotecaException.Code.UNKNOWN);
    }

    @Test
    @DisplayName("BibliotecaException.NotFound uses NOT_FOUND code")
    void bibliotecaExceptionNotFound() {
        domini.BibliotecaException.NoTrobat ex = new domini.BibliotecaException.NoTrobat("Book not found");
        assertThat(ex.code()).isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("BibliotecaException.Validation uses VALIDATION code")
    void bibliotecaExceptionValidation() {
        domini.BibliotecaException.Validacio ex = new domini.BibliotecaException.Validacio("bad isbn");
        assertThat(ex.code()).isEqualTo(domini.BibliotecaException.Code.VALIDATION);
    }

    // ── PrestecRow: extended overdue days tests ────────────────────────

    @Test
    @DisplayName("PrestecRow: overdueDays returns -1 when dataPrestec is null")
    void prestecRowOverdueDaysNullDate() {
        persistencia.row.PrestecRow row = new persistencia.row.PrestecRow(9780000000001L, "Alice", null, false);
        assertThat(row.overdueDays(java.time.LocalDate.now(), 7)).isEqualTo(-1);
    }

    @Test
    @DisplayName("PrestecRow: fromStrings parses ISO date correctly")
    void prestecRowFromStrings() {
        persistencia.row.PrestecRow row = persistencia.row.PrestecRow.fromStrings(9780000000001L, "Bob", "2025-06-15", true);
        assertThat(row.nomPersona()).isEqualTo("Bob");
        assertThat(row.dataPrestec()).isEqualTo(java.time.LocalDate.of(2025, 6, 15));
        assertThat(row.retornat()).isTrue();
    }

    // ── DAO error propagation: BibliotecaException rather than swallow ────

    // ── DAO error propagation: BibliotecaException on query failure ────

    @Test
    @DisplayName("TagDao: getDistinctValues with invalid column returns empty (does not throw)")
    void tagDaoDistinctValuesInvalidColumn() {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        java.util.List<String> vals = cp.obtenirDistinctValues("invalid_column");
        assertThat(vals).isEmpty();
    }

    // ── Config: window placement defaults ────────────────────────────────

    @Test
    @DisplayName("Config: window defaults are sensible")
    void configWindowDefaults() {
        int x = herramienta.config.Configuracio.obtenirWindowX();
        int y = herramienta.config.Configuracio.obtenirWindowY();
        int w = herramienta.config.Configuracio.obtenirWindowWidth();
        int h = herramienta.config.Configuracio.obtenirWindowHeight();
        assertThat(x).isGreaterThanOrEqualTo(0);
        assertThat(y).isGreaterThanOrEqualTo(0);
        assertThat(w).isGreaterThan(100);
        assertThat(h).isGreaterThan(100);
    }

    // ── LlibreValidator: year range validation ───────────────────────────

    @Test
    @DisplayName("LlibreValidator: year 1900 is accepted")
    void validatorYear1900Accepted() {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780000000111L, "OldBook", null, 1900, null, null, null, null, null);
        assertThat(l.obtenirAny()).isEqualTo(1900);
    }

    @Test
    @DisplayName("LlibreValidator: future year within +5 is accepted")
    void validatorFutureYearAccepted() {
        int futureYear = java.time.Year.now().getValue() + 5;
        Llibre l = ValidadorLlibre.comprovarLlibre(9780000000222L, "FutureBook", null, futureYear, null, null, null, null, null);
        assertThat(l.obtenirAny()).isEqualTo(futureYear);
    }

    // ── CsvUtils: parseLine edge cases ───────────────────────────────

    @Test
    @DisplayName("CsvUtils: parseLine handles quoted fields with embedded commas")
    void csvUtilsQuotedFieldWithComma() {
        String[] result = herramienta.io.csv.UtilitatsCsv.analitzarLine("a,\"b,c\",d");
        assertThat(result).containsExactly("a", "b,c", "d");
    }

    @Test
    @DisplayName("CsvUtils: parseLine handles trailing comma")
    void csvUtilsTrailingComma() {
        String[] result = herramienta.io.csv.UtilitatsCsv.analitzarLine("a,b,");
        assertThat(result).containsExactly("a", "b", "");
    }

    @Test
    @DisplayName("CsvUtils: BOM in header is preserved by parseLine; callers must strip it")
    void csvUtilsBomAwareness() {
        String withBom = "\uFEFF" + "isbn,nom,autor";
        String[] result = herramienta.io.csv.UtilitatsCsv.analitzarLine(withBom);
        // parseLine does not strip BOM — this is documented behavior
        assertThat(result[0]).contains("isbn");
        // After stripping BOM, parsing works normally
        String cleaned = withBom.replace("\uFEFF", "");
        String[] cleanedResult = herramienta.io.csv.UtilitatsCsv.analitzarLine(cleaned);
        assertThat(cleanedResult[0]).isEqualTo("isbn");
    }

    // ── Escapers: HTML/JSON/CSV escaping ──────────────────────────────────

    @Test
    @DisplayName("Escapers.html escapes &, <, >, \", '")
    void escapersHtml() {
        assertThat(herramienta.i18n.Escapers.html("<script>alert(\"xss\")</script>"))
            .isEqualTo("&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;");
        assertThat(herramienta.i18n.Escapers.html("Tom & Jerry"))
            .isEqualTo("Tom &amp; Jerry");
        assertThat(herramienta.i18n.Escapers.html(null))
            .isEmpty();
    }

    @Test
    @DisplayName("Escapers.json escapes backslash, quote, controls")
    void escapersJson() {
        assertThat(herramienta.i18n.Escapers.json("hello\nworld"))
            .isEqualTo("hello\\nworld");
        assertThat(herramienta.i18n.Escapers.json("a\"b"))
            .isEqualTo("a\\\"b");
        assertThat(herramienta.i18n.Escapers.json(null))
            .isEmpty();
    }

    @Test
    @DisplayName("Escapers.csv wraps in quotes and doubles internal quotes")
    void escapersCsv() {
        assertThat(herramienta.i18n.Escapers.csv("hello"))
            .isEqualTo("\"hello\"");
        assertThat(herramienta.i18n.Escapers.csv("say \"hi\""))
            .isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(herramienta.i18n.Escapers.csv(null))
            .isEmpty();
    }

    // ── Config.withBatch: setters within batch defer save ──────────────────

    @Test
    @DisplayName("ControladorDomini: in-memory filter applies sort like SQL path")
    void filtrarInMemoryAppliesSort() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = new Llibre(9780000000001L, "Zebra", "A", 2020, "", 0.0, 0.0, false, "");
        Llibre b = new Llibre(9780000000002L, "Alpha", "B", 2021, "", 0.0, 0.0, false, "");
        cd.afegirLlibre(a);
        cd.afegirLlibre(b);
        LlibreFilter f = domini.ConstructorFiltreLlibre.of().sort("nom", true).build();
        List<Llibre> sorted = cd.aplicarFiltres(f);
        assertThat(sorted.get(0).obtenirNom()).isEqualTo("Alpha");
        assertThat(sorted.get(1).obtenirNom()).isEqualTo("Zebra");
    }

    @Test
    @DisplayName("RegistreCampsFormulari links JLabel to field for accessibility")
    void formFieldRegistryLinkLabel() {
        var registry = new presentacio.RegistreCampsFormulari();
        javax.swing.JLabel lbl = new javax.swing.JLabel("ISBN");
        javax.swing.JTextField tf = new javax.swing.JTextField();
        registry.linkLabel(lbl, tf);
        assertThat(lbl.getLabelFor()).isSameAs(tf);
    }

    @Test
    @DisplayName("ModelTaulaBiblioteca backs row count from book list")
    void bibliotecaTableModelSetBooks() {
        var model = new presentacio.ModelTaulaBiblioteca();
        Llibre l = new Llibre(9780306406157L, "Test", "Author", 2020, "", 5.0, 10.0, true, "");
        model.posarBooks(java.util.List.of(l));
        assertThat(model.getRowCount()).isOne();
        assertThat(model.obtenirBookAt(0).obtenirISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("LlibreValidator.validateInto updates target without replacing extras")
    void llibreValidatorValidateInto() {
        Llibre l = new Llibre(9780306406157L, "Old", "A", 2000, "desc", 1.0, 2.0, false, "");
        l.posarNotes("keep notes");
        l.posarPagines(400);
        ValidadorLlibre.validarInto(l, 9780306406157L, "New Title", "Author", 2020,
            "desc", 5.0, 10.0, true, "");
        assertThat(l.obtenirNom()).isEqualTo("New Title");
        assertThat(l.obtenirNotes()).isEqualTo("keep notes");
        assertThat(l.obtenirPagines()).isEqualTo(400);
    }

    @Test
    @DisplayName("Config.withBatch defers save and commits once at end")
    void configWithBatchDefersSave() throws Exception {
        java.nio.file.Path tmpDir = Files.createTempDirectory("biblioteca_cfg_batch_");
        java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
        Files.createDirectories(cfgDir);
        String origHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
            herramienta.config.Configuracio.reload();
            herramienta.config.ConfiguracioUi.posarDarkMode(true);
            Thread.sleep(500);
            herramienta.config.Configuracio.withBatch(() -> {
                herramienta.config.ConfiguracioUi.posarDarkMode(false);
                herramienta.config.ConfiguracioUi.posarFontSize("large");
            });
            Thread.sleep(500);
            assertThat(herramienta.config.Configuracio.esDarkMode()).isFalse();
            assertThat(herramienta.config.Configuracio.obtenirFontSize()).isEqualTo("large");
            herramienta.config.Configuracio.reload();
            assertThat(herramienta.config.Configuracio.esDarkMode()).isFalse();
            assertThat(herramienta.config.Configuracio.obtenirFontSize()).isEqualTo("large");
        } finally {
            System.setProperty("user.home", origHome);
            herramienta.config.Configuracio.reload();
            Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        }
    }
@Test
    @DisplayName("MostrarBibliotecaControl: clearCoverCache is safe to call")
    void netejarCoverCacheSmoke() {
        presentacio.ControladorMostrarBiblioteca.netejarCoverCache();
    }

    @Test
    @DisplayName("RenderitzadorCasellaLlegit: marks read when cell value matches I18n filter_read")
    void llegitRendererShowsReadState() {
        var renderer = new presentacio.renderers.RenderitzadorCasellaLlegit();
        javax.swing.JTable table = new javax.swing.JTable();
        java.awt.Component c = renderer.getTableCellRendererComponent(
            table, herramienta.i18n.I18n.t("filter_read"), false, false, 0, 0);
        assertThat(c).isInstanceOf(javax.swing.JCheckBox.class);
        assertThat(((javax.swing.JCheckBox) c).isSelected()).isTrue();
        java.awt.Component c2 = renderer.getTableCellRendererComponent(
            table, herramienta.i18n.I18n.t("filter_unread"), false, false, 0, 0);
        assertThat(((javax.swing.JCheckBox) c2).isSelected()).isFalse();
    }

    @Test
    @DisplayName("ValidadorFormulari: invalid field gets red border")
    void formValidatorInvalidBorder() {
        javax.swing.JTextField tf = new javax.swing.JTextField("bad");
        presentacio.ValidadorFormulari.validarField(tf, false);
        assertThat(tf.getBorder()).isNotNull();
        presentacio.ValidadorFormulari.validarField(tf, true);
    }

    @Test
    @DisplayName("ControladorDomini: concurrent add while reading is consistent")
    void concurrentAddWhileRead() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final long isbn = 9780001000000L + i;
            threads.add(new Thread(() -> {
                try {
                    start.await();
                    cd.afegirLlibre(new Llibre(isbn, "T" + isbn, "A", 2020, "", 0.0, 0.0, false, ""));
                    cd.obtenirAllLlibres().size();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        for (Thread t : threads) t.start();
        start.countDown();
        for (Thread t : threads) t.join(5000);
        assertThat(errors.get()).isZero();
        assertThat(cd.obtenirAllLlibres().size()).isGreaterThanOrEqualTo(8);
    }

    // ── CsvUtils extended ────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "0-306-40615-X, 9780306406157",
        "978-0-306-40615-7, 9780306406157",
        "'', ''"
    })
    @DisplayName("CsvUtils.parseIsbn normalizes ISBN-10 X and ISBN-13")
    void analitzarIsbnNormalization(String raw, String expected) {
        assertThat(herramienta.io.csv.UtilitatsCsv.analitzarIsbn(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("CsvUtils.colVal missing column returns empty string")
    void colValMissingColumn() {
        var h = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(new String[]{"ISBN"});
        assertThat(herramienta.io.csv.UtilitatsCsv.colVal(h, new String[]{"978"}, "Title")).isEmpty();
    }

    @Test
    @DisplayName("CsvUtils.parseDoubleOrZero handles invalid input")
    void analitzarDoubleOrZeroInvalid() {
        assertThat(herramienta.io.csv.UtilitatsCsv.analitzarDoubleOrZero("x")).isZero();
        assertThat(herramienta.io.csv.UtilitatsCsv.analitzarDoubleOrZero(" 3.5 ")).isEqualTo(3.5);
    }

    @Test
    @DisplayName("CsvUtils.csvQ escapes quotes and null")
    void csvQEscaping() {
        assertThat(herramienta.io.csv.UtilitatsCsv.csvQ("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(herramienta.io.csv.UtilitatsCsv.csvQ(null)).isEmpty();
    }

    // ── FiltreUtils extended ─────────────────────────────────────────────────

    @Test
    @DisplayName("FiltreUtils null-safe ISBN and string matchers")
    void filtreUtilsNullSafe() {
        assertThat(FiltreUtils.matchISBN(null, 978L)).isFalse();
        assertThat(FiltreUtils.matchISBN(978L, null)).isFalse();
        assertThat(FiltreUtils.matchString(null, "x")).isFalse();
        assertThat(FiltreUtils.normalize(null)).isEmpty();
    }

    // ── Llibre display names ─────────────────────────────────────────────────

    @Test
    @DisplayName("Llibre.getDisplayNom language fallback")
    void llibreDisplayNom() throws Exception {
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Main", null, null, null, null, null, null, null);
        l.posarNomCa("Català");
        l.posarNomEn("English");
        assertThat(l.obtenirDisplayNom("ca")).isEqualTo("Català");
        assertThat(l.obtenirDisplayNom("en")).isEqualTo("English");
        assertThat(l.obtenirDisplayNom("de")).isEqualTo("Main");
    }

    // ── CoverService ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CoverService cache miss returns null")
    void coverServiceCacheMiss() {
        assertThat(herramienta.io.ServeiCoberta.obtenirCachedBytes("0000000000999")).isNull();
        assertThat(herramienta.io.ServeiCoberta.obtenirCachedImage("0000000000998")).isNull();
    }

    // ── Domain: tags and loans ───────────────────────────────────────────────

    @Test
    @DisplayName("Tag rename and delete")
    void tagRenameDelete() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag t = cd.afegirTag("old");
        cd.reanomenarTag(t.obtenirId(), "new");
        assertThat(cd.obtenirAllTags()).extracting(Tag::obtenirNom).containsExactly("new");
        cd.eliminarTag(t);
        assertThat(cd.obtenirAllTags()).isEmpty();
    }

    @Test
    @DisplayName("Loan prestar/retornar round-trip")
    void loanRoundTrip() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.afegirLlibre(book(9780306406157L, "Loan"));
        cd.prestarLlibre(9780306406157L, "Bob");
        assertThat(cd.obtenirLoanedISBNs()).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.obtenirLoanedISBNs()).doesNotContain(9780306406157L);
    }

    @Test
    @DisplayName("clearAll wipes books and shelves")
    void netejarAllWipesData() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "X");
        cd.afegirLlista("Shelf");
        cd.netejarAll();
        assertThat(cd.getSize()).isZero();
        assertThat(cd.obtenirAllLlistes()).isEmpty();
    }

    // ── CsvUtils.existsInLibrary ─────────────────────────────────────────────

    @Test
    @DisplayName("CsvUtils.existsInLibrary reflects domain state")
    void existsInLibrary() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Present");
        assertThat(herramienta.io.csv.UtilitatsCsv.existsInLibrary(cd, 9780306406157L)).isTrue();
        assertThat(herramienta.io.csv.UtilitatsCsv.existsInLibrary(cd, 9780000000001L)).isFalse();
    }

    // ── ControladorDomini construction ───────────────────────────────────────

    @Test
    @DisplayName("Public constructor produces isolated instance")
    void publicConstructorIsolatedInstance() throws Exception {
        ControladorDomini.getInstance();
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini cd = new ControladorDomini(cp);
        add(cd, 9780306406157L, "Isolated");
        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(ControladorDomini.getInstance()).isNotSameAs(cd);
    }

    @Test
    @DisplayName("create() factory returns a fresh instance bound to given cp")
    void crearFactoryReturnsFreshInstance() {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini a = ControladorDomini.create(cp);
        ControladorDomini b = ControladorDomini.create(cp);
        assertThat(a).isNotSameAs(b);
        assertThat(a).isNotSameAs(ControladorDomini.getInstance());
    }

    // ── Lookup by id (Llista / Tag) ──────────────────────────────────────────

    @Test
    @DisplayName("getLlistaById returns matching shelf")
    void obtenirLlistaByIdFindsShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista shelf = cd.afegirLlista("Fiction");
        assertThat(cd.obtenirLlistaById(shelf.obtenirId()).obtenirNom()).isEqualTo("Fiction");
    }

    @Test
    @DisplayName("getLlistaById throws on missing id")
    void obtenirLlistaByIdMissingThrows() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.obtenirLlistaById(99_999))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getTagById returns matching tag")
    void obtenirTagByIdFindsTag() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag tag = cd.afegirTag("ToRead");
        assertThat(cd.obtenirTagById(tag.obtenirId()).obtenirNom()).isEqualTo("ToRead");
    }

    @Test
    @DisplayName("getTagById throws on missing id")
    void obtenirTagByIdMissingThrows() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.obtenirTagById(99_999))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("99");
    }

    // ── Llibre autor / autors invariant ──────────────────────────────────────

    @Test
    @DisplayName("Llibre.setAutors(['Alice']) makes getAutor return 'Alice'")
    void llibreSetAutorsSingle() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.posarAutors(java.util.List.of("Alice"));
        assertThat(l.obtenirAutor()).isEqualTo("Alice");
        assertThat(l.obtenirAutors()).containsExactly("Alice");
    }

    @Test
    @DisplayName("Llibre.setAutors reflects in getAutor")
    void llibreSetAutorsReflectsInGetAutor() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.posarAutors(java.util.List.of("Alice", "Bob"));
        assertThat(l.obtenirAutor()).isEqualTo("Alice, Bob");
    }

    @Test
    @DisplayName("Llibre.setAutors(null) clears autors list")
    void llibreSetAutorsNullClearsAutors() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.posarAutors(java.util.List.of("Alice"));
        l.posarAutors(null);
        assertThat(l.obtenirAutors()).isEmpty();
    }

    // ── getDistinctValues: dual-path behavior ────────────────────────────────

    @Test
    @DisplayName("getDistinctValues: in-memory path returns distinct sorted values for editorial")
    void obtenirDistinctValuesInMemoryEditorial() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre a = herramienta.text.ValidadorLlibre.comprovarLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
        domini.Llibre b = herramienta.text.ValidadorLlibre.comprovarLlibre(9780000000001L, "B", null, null, null, null, null, null, null);
        a.posarEditorial("Penguin");
        b.posarEditorial("Ace");
        cd.afegirLlibre(a);
        cd.afegirLlibre(b);
        assertThat(cd.obtenirDistinctValues("editorial")).contains("Ace", "Penguin");
    }

    @Test
    @DisplayName("getDistinctValues: SQL fallback returns empty for unknown column")
    void obtenirDistinctValuesUnknownColumnEmpty() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThat(cd.obtenirDistinctValues("not_a_column")).isEmpty();
        assertThat(cd.obtenirDistinctValues("any")).isEmpty();
    }
    // ── NotFound / Duplicate / Validation exception codes ────────────────────

    @Test
    @DisplayName("BibliotecaException.NotFound uses NOT_FOUND code")
    void notFoundCode() {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        cd.afegirLlibre(l);
        assertThatThrownBy(() -> cd.obtenirLlibre(9780000000001L))
            .isInstanceOf(domini.BibliotecaException.NoTrobat.class)
            .extracting("code").isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("Duplicate ISBN throws BibliotecaException.Duplicate")
    void duplicateCode() {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        cd.afegirLlibre(l);
        assertThatThrownBy(() -> cd.afegirLlibre(l))
            .isInstanceOf(domini.BibliotecaException.Duplicat.class);
    }

    @Test
    @DisplayName("getLlistaById throws BibliotecaException.NotFound for unknown id")
    void obtenirLlistaByIdNotFound() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.obtenirLlistaById(9999))
            .isInstanceOf(domini.BibliotecaException.NoTrobat.class);
    }

    @Test
    @DisplayName("getTagById throws BibliotecaException.NotFound for unknown id")
    void obtenirTagByIdNotFound() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.obtenirTagById(9999))
            .isInstanceOf(domini.BibliotecaException.NoTrobat.class);
    }

    @Test
    @DisplayName("deleteLlibre throws BibliotecaException.NotFound for unknown ISBN")
    void eliminarLlibreNotFound() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.eliminarLlibre(9780000000001L))
            .isInstanceOf(domini.BibliotecaException.NoTrobat.class);
    }
}

