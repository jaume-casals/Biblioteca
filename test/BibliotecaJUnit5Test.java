import domini.ControladorDomini;
import domini.Llibre;
import domini.LlibreFilter;
import domini.Llista;
import domini.SortSpec;
import domini.Tag;
import herramienta.FiltreUtils;
import herramienta.LlibreValidator;
import persistencia.ControladorPersistencia;

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
    void resetDb() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Llibre book(long isbn, String nom) {
        return LlibreValidator.checkLlibre(isbn, nom, null, null, null, null, null, null, null);
    }

    private Llibre book(long isbn, String nom, String autor, Integer any) {
        return LlibreValidator.checkLlibre(isbn, nom, autor, any, null, null, null, null, null);
    }

    private void add(ControladorDomini cd, long isbn, String nom) throws Exception {
        cd.addLlibre(book(isbn, nom));
    }

    private void add(ControladorDomini cd, long isbn, String nom, String autor, Integer any) throws Exception {
        cd.addLlibre(book(isbn, nom, autor, any));
    }

    // ── LlibreValidator ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid ISBN-13 accepted")
    void validIsbn13() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Title", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("Valid ISBN-10 accepted")
    void validIsbn10() {
        Llibre l = LlibreValidator.checkLlibre(8420413739L, "Title", null, null, null, null, null, null, null);
        assertThat(l.getISBN()).isEqualTo(8420413739L);
    }

    @ParameterizedTest(name = "ISBN {0} rejected")
    @ValueSource(longs = {12345678901234L, 12345L, 0L})
    @DisplayName("Invalid ISBN lengths rejected")
    void invalidIsbnLengths(long isbn) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(isbn, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null ISBN rejected")
    void nullIsbnRejected() {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(null, "X", null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Blank nom ''{0}'' rejected")
    @ValueSource(strings = {"", "  ", "\t"})
    @DisplayName("Blank/empty nom rejected")
    void blankNomRejected(String nom) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, nom, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Valoracio {0} rejected")
    @ValueSource(doubles = {-0.1, -1.0, 10.1, 11.0})
    @DisplayName("Out-of-range valoració rejected")
    void outOfRangeValoracio(double v) {
        assertThatThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, v, null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Valoracio {0} accepted")
    @ValueSource(doubles = {0.0, 5.0, 10.0})
    @DisplayName("Boundary valoració values accepted")
    void boundaryValoracioAccepted(double v) {
        assertThatNoException().isThrownBy(() ->
            LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, v, null, null, null));
    }

    @Test
    @DisplayName("Default field values applied when nulls passed")
    void defaultFieldValues() {
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Test", null, null, null, null, null, null, null);
        assertThat(l.getAutor()).isEqualTo("");
        assertThat(l.getAny()).isEqualTo(0);
        assertThat(l.getValoracio()).isEqualTo(0.0);
        assertThat(l.getPreu()).isEqualTo(0.0);
        assertThat(l.getLlegit()).isFalse();
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
        LlibreFilter f = domini.LlibreFilterBuilder.of()
            .isbn(978L).nom("test").autor("author").anyMin(1900).llegit(true)
            .sort("nom", false).build();
        LlibreFilter c = f.copy();
        assertThat(c.getIsbn()).isEqualTo(978L);
        assertThat(c.getNom()).isEqualTo("test");
        assertThat(c.getAutor()).isEqualTo("author");
        assertThat(c.getAnyMin()).isEqualTo(1900);
        assertThat(c.getLlegit()).isTrue();
        assertThat(c.getSort().column()).isEqualTo("nom");
        assertThat(c.getSort().ascending()).isFalse();
    }

    @Test
    @DisplayName("Add book: size increments and retrieval works")
    void addBookIncreasesSize() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        assertThat(cd.getSize()).isEqualTo(1);
        Llibre l = cd.getLlibre(9780306406157L);
        assertThat(l.getNom()).isEqualTo("Dune");
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
    void deleteBookRemovesIt() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.deleteLlibre(9780306406157L);
        assertThat(cd.getSize()).isZero();
        assertThatThrownBy(() -> cd.getLlibre(9780306406157L))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Multiple books sorted by ISBN ascending")
    void booksAscendingIsbnOrder() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "C");
        add(cd, 8420413739L, "A");
        add(cd, 9780141439518L, "B");
        List<Llibre> all = cd.getAllLlibres();
        assertThat(all).extracting(Llibre::getISBN)
            .isSortedAccordingTo(Long::compareTo);
    }

    @Test
    @DisplayName("getLlibre throws for non-existent ISBN")
    void getLlibreThrowsForMissing() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.getLlibre(1111111111111L))
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
    void updateLlibrePersists() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llibre updated = LlibreValidator.checkLlibre(9780306406157L, "Dune Messiah",
            null, null, null, 9.0, null, null, null);
        cd.updateLlibre(updated);
        assertThat(cd.getLlibre(9780306406157L).getNom()).isEqualTo("Dune Messiah");
        assertThat(cd.getLlibre(9780306406157L).getValoracio()).isEqualTo(9.0);
    }

    // ── Domain: Shelves (Llistes) ────────────────────────────────────────────

    @Test
    @DisplayName("Create shelf: appears in shelf list")
    void createShelfAppearsInList() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.addLlista("Favorits");
        assertThat(cd.getAllLlistes()).extracting(Llista::getNom).contains("Favorits");
    }

    @Test
    @DisplayName("Add book to shelf: count is 1")
    void addBookToShelfCount() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.addLlista("Sci-Fi");
        cd.addLlibreToLlista(9780306406157L, shelf.getId(), 8.0, false);
        assertThat(cd.getCountInLlista(shelf.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Delete shelf cascades: no orphan rows, book survives")
    void deleteShelfCascades() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.addLlista("Temp");
        cd.addLlibreToLlista(9780306406157L, shelf.getId(), 7.0, false);
        cd.deleteLlista(shelf);
        assertThat(cd.getAllLlistes()).isEmpty();
        assertThat(cd.getSize()).isEqualTo(1); // book still exists
    }

    // ── Domain: Tags ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Create tag and assign to book")
    void createTagAndAssign() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Tag tag = cd.addTag("Sci-Fi");
        cd.addLlibreToTag(9780306406157L, tag.getId());
        assertThat(cd.getTagsForLlibre(9780306406157L))
            .extracting(Tag::getNom).containsExactly("Sci-Fi");
    }

    @Test
    @DisplayName("Filter by tag via aplicarFiltres")
    void filterByTag() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        add(cd, 8420413739L, "Other");
        Tag tag = cd.addTag("Sci-Fi");
        cd.addLlibreToTag(9780306406157L, tag.getId());
        LlibreFilter ft = domini.LlibreFilterBuilder.of().tagId(tag.getId()).build();
        List<Llibre> results = cd.aplicarFiltres(ft);
        assertThat(results).extracting(Llibre::getNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("Remove tag from book")
    void removeTagFromBook() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Tag tag = cd.addTag("Fantasy");
        cd.addLlibreToTag(9780306406157L, tag.getId());
        cd.removeLlibreFromTag(9780306406157L, tag.getId());
        assertThat(cd.getTagsForLlibre(9780306406157L)).isEmpty();
    }

    // ── Domain: Filters ─────────────────────────────────────────────────────

    @Test
    @DisplayName("aplicarFiltres by nom returns matching books")
    void filterByNom() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        LlibreFilter fn = domini.LlibreFilterBuilder.of().nom("dune").build();
        List<Llibre> results = cd.aplicarFiltres(fn);
        assertThat(results).extracting(Llibre::getNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("aplicarFiltres by year range")
    void filterByYearRange() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Asimov", 1951);
        LlibreFilter fy = domini.LlibreFilterBuilder.of().anyMin(1960).anyMax(1970).build();
        List<Llibre> results = cd.aplicarFiltres(fy);
        assertThat(results).extracting(Llibre::getNom).containsExactly("Dune");
    }

    @Test
    @DisplayName("aplicarFiltres by autor")
    void filterByAutor() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        LlibreFilter fa = domini.LlibreFilterBuilder.of().autor("Asimov").build();
        List<Llibre> results = cd.aplicarFiltres(fa);
        assertThat(results).extracting(Llibre::getNom).containsExactly("Foundation");
    }

    @Test
    @DisplayName("aplicarFiltres with no match returns empty")
    void filterNoMatch() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        LlibreFilter fne = domini.LlibreFilterBuilder.of().nom("NonExistent").build();
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
        Set<Long> loaned = cd.getLoanedISBNs();
        assertThat(loaned).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.getLoanedISBNs()).doesNotContain(9780306406157L);
    }

    // ── Domain: Backup / Restore ─────────────────────────────────────────────

    @Test
    @DisplayName("Backup and restore preserves all books")
    void backupRestorePreservesBooks() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);

        File tmp = File.createTempFile("backup_junit5_", ".sql");
        tmp.deleteOnExit();
        cd.backupToSQL(tmp);

        cd.deleteLlibre(9780306406157L);
        cd.deleteLlibre(8420413739L);
        assertThat(cd.getSize()).isZero();

        cd.restoreFromSQL(tmp);
        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.getAllLlibres())
            .extracting(Llibre::getNom)
            .containsExactlyInAnyOrder("Dune", "Foundation");
    }

    @Test
    @DisplayName("Backup preserves shelf membership")
    void backupPreservesShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.addLlista("Favorits");
        cd.addLlibreToLlista(9780306406157L, shelf.getId(), 9.0, true);

        File tmp = File.createTempFile("backup_shelf_", ".sql");
        tmp.deleteOnExit();
        cd.backupToSQL(tmp);

        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
        cd = ControladorDomini.getInstance();
        cd.restoreFromSQL(tmp);

        assertThat(cd.getAllLlistes()).extracting(Llista::getNom).contains("Favorits");
        Llista restored = cd.getAllLlistes().get(0);
        assertThat(cd.getCountInLlista(restored.getId())).isEqualTo(1);
    }

    // ── executeSQLFile: defensive parser ─────────────────────────────────────

    @Test
    @DisplayName("executeSQLFile: ignores DROP TABLE / USE / ALTER in tampered file")
    void executeSQLFileDropsAreIgnored() throws Exception {
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
        ControladorPersistencia.getInstance().executeSQLFile(tampered);
        // Books survive
        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(cd.getLlibre(9780306406157L).getNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Backup/restore round-trip preserves ';' in book notes (parser doesn't split mid-string)")
    void backupRestorePreservesSemicolonInNotes() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre book = LlibreValidator.checkLlibre(9780306406157L, "Semicolon book", null, null, null, null, null, null, null);
        book.setNotes("first note; second note; third note");
        cd.addLlibre(book);

        File tmp = File.createTempFile("sql_semi_roundtrip_", ".sql");
        tmp.deleteOnExit();
        cd.backupToSQL(tmp);

        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
        cd = ControladorDomini.getInstance();
        cd.restoreFromSQL(tmp);
        assertThat(cd.getLlibre(9780306406157L).getNotes())
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
        assertThat(cd.getRecentlyAdded()).isNotEmpty();
    }

    // ── Domain: Distinct values ───────────────────────────────────────────────

    @Test
    @DisplayName("getDistinctValues returns unique editorials")
    void distinctEditorials() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
        a.setEditorial("Penguin");
        Llibre b = LlibreValidator.checkLlibre(8420413739L, "B", null, null, null, null, null, null, null);
        b.setEditorial("Penguin");
        Llibre c = LlibreValidator.checkLlibre(9780141439518L, "C", null, null, null, null, null, null, null);
        c.setEditorial("HarperCollins");
        cd.addLlibre(a);
        cd.addLlibre(b);
        cd.addLlibre(c);
        List<String> vals = cd.getDistinctValues("editorial");
        assertThat(vals).containsExactlyInAnyOrder("Penguin", "HarperCollins");
    }

    @Test
    @DisplayName("getDistinctValues rejects unknown column: returns empty (not executes SQL)")
    void distinctValuesRejectsUnknownColumn() {
        ControladorDomini cd = ControladorDomini.getInstance();
        // AUTOCOMPLETE_COLUMNS whitelist blocks unknown columns → empty list, no SQL executed
        assertThat(cd.getDistinctValues("DROP TABLE llibre; --")).isEmpty();
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
            for (Llibre l : cd.getAllLlibres()) {
                pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\",%.1f,%.2f,%b,\"%s\",\"%s\"%n",
                    l.getISBN(), l.getNom().replace("\"","\"\""),
                    l.getAutor().replace("\"","\"\""), l.getAny(),
                    "", l.getValoracio(), l.getPreu(), l.getLlegit(), "", "");
            }
        }

        // Clear library
        cd.deleteLlibre(9780306406157L);
        cd.deleteLlibre(8420413739L);
        assertThat(cd.getSize()).isZero();

        // Re-import by parsing CSV lines (same logic as MostrarBibliotecaControl)
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader(tmp, java.nio.charset.StandardCharsets.UTF_8))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] c = parseCsvLine(line);
                long isbn = Long.parseLong(c[0].trim());
                Llibre l = LlibreValidator.checkLlibre(isbn, c[1], c[2],
                    Integer.parseInt(c[3].trim()), "", 0.0, 0.0, false, "");
                cd.addLlibre(l);
            }
        }

        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.getAllLlibres())
            .extracting(Llibre::getNom)
            .containsExactlyInAnyOrder("Dune", "Foundation");
    }

    /** Minimal CSV parser matching MostrarBibliotecaControl.parseCSVLine. */
    private static String[] parseCsvLine(String line) {
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
    void clearAllRemovesEverything() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.addLlista("Sci-Fi");
        cd.addTag("Space");
        cd.clearAll();
        assertThat(cd.getSize()).isZero();
        assertThat(cd.getAllLlistes()).isEmpty();
        assertThat(cd.getAllTags()).isEmpty();
    }

    // ── CSV strategy canHandle ───────────────────────────────────────────────

    @Test
    @DisplayName("Goodreads canHandle accepts Goodreads header (full), rejects native")
    void goodreadsCanHandle() {
        var s = new herramienta.csv.GoodreadsCsvStrategy();
        String full = "Book Id,Title,Author,ISBN13,Exclusive Shelf,A,B,C,D,E,F";
        assertThat(s.canHandle(full)).isTrue();
        assertThat(s.canHandle("isbn,nom,autor")).isFalse();
    }

    @Test
    @DisplayName("Native canHandle is true (fallback)")
    void nativeCanHandle() {
        var s = new herramienta.csv.NativeCsvStrategy();
        assertThat(s.canHandle("random,header")).isTrue();
        assertThat(s.canHandle("a,b,c,d")).isTrue();
        assertThat(s.canHandle("")).isFalse();
        assertThat(s.canHandle("a,b")).isTrue();
    }

    // ── CSV edge cases ───────────────────────────────────────────────────────

    @Test
    @DisplayName("CsvUtils.parseLine: trailing comma, embedded quote, BOM tolerated")
    void csvParseLineEdgeCases() {
        String[] r1 = herramienta.csv.CsvUtils.parseLine("a,b,c,");
        assertThat(r1).hasSize(4).contains("a", "b", "c", "");
        String[] r2 = herramienta.csv.CsvUtils.parseLine("\"a\"\"b\",c");
        assertThat(r2).contains("a\"b", "c");
        String[] r3 = herramienta.csv.CsvUtils.parseLine("﻿ISBN,Nom");
        assertThat(r3[0]).endsWith("ISBN");
    }

    @Test
    @DisplayName("Rfc4180Reader streams multi-line rows")
    void rfc4180ReaderStreaming() throws Exception {
        String csv = "a,b\nfoo,bar\n\"qu\"\"oted\",\"line\nbreak\"\n";
        try (var r = new herramienta.csv.Rfc4180Reader(new java.io.StringReader(csv))) {
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
        ControladorPersistencia.resetForTest();
        persistencia.ControladorPersistencia.getInstance();
        persistencia.ControladorPersistencia.resetForTest();
        // No exception means schema_version + skip-logic worked
        persistencia.ControladorPersistencia.getInstance();
    }

    // ── DialogoError headless mode ───────────────────────────────────────────

    @Test
    @DisplayName("DialogoError.showErrorMessage is no-op in headless/test mode")
    void dialogoErrorHeadlessSafe() {
        new herramienta.DialogoError("test", new Exception("err")).showErrorMessage();
        // No exception, no GUI; passes by virtue of biblioteca.test=true
    }

    // ── Integration: fixture library CRUD ─────────────────────────────────────

    @Test
    @DisplayName("Fixture: 20 books, 3 shelves, 5 tags, 2 loans — basic CRUD survives")
    void integrationFixtureLibrary() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        for (int i = 0; i < 20; i++) {
            long isbn = 9780306400000L + i;
            cd.addLlibre(book(isbn, "Book " + i, "Author " + i, 1990 + i));
        }
        Llista a = cd.addLlista("Read");
        cd.addLlista("Wishlist");
        cd.addLlista("Reference");
        for (int i = 0; i < 5; i++) cd.addTag("Tag" + i);

        cd.addLlibreToLlista(9780306400000L, a.getId(), 8.0, true);
        cd.addLlibreToLlista(9780306400001L, a.getId(), 7.0, true);

        cd.prestarLlibre(9780306400000L, "Alice");
        cd.prestarLlibre(9780306400001L, "Bob");

        assertThat(cd.getSize()).isEqualTo(20);
        assertThat(cd.getAllLlistes()).hasSize(3);
        assertThat(cd.getAllTags()).hasSize(5);
        assertThat(cd.getLoanedISBNs()).hasSize(2);
        assertThat(cd.getLlibresInLlista(a.getId())).hasSize(2);
    }

    // ── Catalan diacritics autocomplete ──────────────────────────────────────

    @Test
    @DisplayName("FiltreUtils.normalize strips Catalan diacritics for prefix match")
    void catalanDiacriticsNormalize() {
        assertThat(herramienta.FiltreUtils.normalize("Català")).isEqualTo("catala");
        assertThat(herramienta.FiltreUtils.normalize("àéíòú")).isEqualTo("aeiou");
    }

    // ── Tag setNom blank throws ──────────────────────────────────────────────

    @Test
    @DisplayName("Tag.setNom rejects null and blank")
    void tagSetNomBlank() {
        Tag t = new Tag(1, "ok");
        assertThatThrownBy(() -> t.setNom(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> t.setNom("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    // ── LibraryEvents/listener tests ─────────────────────────────────────────

    @Test
    @DisplayName("LibraryEvents interface composes update/delete/blob/membership callbacks")
    void libraryEventsInterfaceComposes() {
        java.util.concurrent.atomic.AtomicInteger updates = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger inserts = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger deletes = new java.util.concurrent.atomic.AtomicInteger();
        interficie.LibraryEvents le = new interficie.LibraryEvents() {
            @Override public void onBookUpdated(Llibre l, boolean isNew) { if (isNew) inserts.incrementAndGet(); else updates.incrementAndGet(); }
            @Override public void onBookDeleted(Llibre l) { deletes.incrementAndGet(); }
        };
        Llibre book = book(9780306406157L, "Dune");
        le.onBookUpdated(book, true);
        le.onBookUpdated(book, false);
        le.onBookDeleted(book);
        // default no-op blob/membership reachable
        le.onBlobChanged(123L, true);
        le.onMembershipChanged(123L, 1, true);

        assertThat(inserts).hasValue(1);
        assertThat(updates).hasValue(1);
        assertThat(deletes).hasValue(1);
    }

    // ── ConnectionConfig: password masking ───────────────────────────────────

    @Test
    @DisplayName("ConnectionConfig.toString masks password")
    void connectionConfigToStringMasksPassword() {
        var c = new persistencia.ConnectionConfig("mariadb", "localhost", "user", "topsecret", "default", null);
        assertThat(c.toString()).doesNotContain("topsecret").contains("***");
    }

    // ── LlibreLlistaRow: NaN/Infinity sanity ─────────────────────────────────

    @Test
    @DisplayName("LlibreLlistaRow handles NaN/Infinity valoracio sanely")
    void llibreLlistaRowExtremeValues() {
        var nan = new persistencia.LlibreLlistaRow(1L, 1, Double.NaN, false);
        var inf = new persistencia.LlibreLlistaRow(1L, 1, Double.POSITIVE_INFINITY, true);
        assertThat(nan.valoracio()).isNaN();
        assertThat(inf.valoracio()).isPositive();
        // record equals() with NaN: NaN != NaN by IEEE, but Double.equals uses Double.doubleToLongBits → equal
        assertThat(nan).isEqualTo(new persistencia.LlibreLlistaRow(1L, 1, Double.NaN, false));
    }

    // ── UITheme.rebuildFonts updates UIManager.defaultFont ───────────────────

    @Test
    @DisplayName("UITheme.rebuildFonts puts new font in UIManager")
    void uiThemeRebuildFontsUpdatesUIManager() {
        herramienta.UITheme.rebuildFonts("small");
        java.awt.Font f1 = (java.awt.Font) javax.swing.UIManager.get("defaultFont");
        assertThat(f1.getSize()).isEqualTo(11);
        herramienta.UITheme.rebuildFonts("large");
        java.awt.Font f2 = (java.awt.Font) javax.swing.UIManager.get("defaultFont");
        assertThat(f2.getSize()).isEqualTo(16);
    }

    // ── Isbn13Normalizer: ISBN-10 X-check → ISBN-13 ──────────────────────────

    @Test
    @DisplayName("Isbn13Normalizer converts ISBN-10 with X-check digit")
    void isbn13NormalizerXCheck() {
        assertThat(herramienta.Isbn13Normalizer.toIsbn13("019853110X"))
            .isEqualTo("9780198531104");
        assertThat(herramienta.Isbn13Normalizer.toIsbn13("0306406152"))
            .startsWith("978");
        assertThat(herramienta.Isbn13Normalizer.toIsbn13("9780306406157"))
            .isEqualTo("9780306406157");
    }

    // ── DateUtils.parseIsoDate ───────────────────────────────────────────────

    @Test
    @DisplayName("parseIsoDate: ISO accepted, garbage → null")
    void parseIsoDate() {
        assertThat(herramienta.DateUtils.parseIsoDate("2024-03-15"))
            .isEqualTo(java.time.LocalDate.of(2024, 3, 15));
        assertThat(herramienta.DateUtils.parseIsoDate(null)).isNull();
        assertThat(herramienta.DateUtils.parseIsoDate("")).isNull();
        assertThat(herramienta.DateUtils.parseIsoDate("not a date")).isNull();
    }

    // ── PrestecRow.overdueDays + toDisplayMap ────────────────────────────────

    @Test
    @DisplayName("PrestecRow.overdueDays counts days past threshold")
    void prestecRowOverdueDays() {
        persistencia.PrestecRow r = new persistencia.PrestecRow(1L, "X", java.time.LocalDate.of(2024, 1, 1), false);
        assertThat(r.overdueDays(java.time.LocalDate.parse("2024-02-15"), 30)).isEqualTo(15);
        assertThat(r.overdueDays(java.time.LocalDate.parse("2024-01-15"), 30)).isZero();
        persistencia.PrestecRow bad = persistencia.PrestecRow.fromStrings(1L, "X", "not a date", false);
        assertThat(bad.overdueDays(java.time.LocalDate.parse("2024-01-01"), 30)).isEqualTo(-1L);
    }

    @Test
    @DisplayName("PrestecRow.toDisplayMap exposes all 4 columns")
    void prestecRowToDisplayMap() {
        var m = new persistencia.PrestecRow(123L, "Paul", java.time.LocalDate.of(2024, 1, 1), false).toDisplayMap();
        assertThat(m).containsKeys("isbn", "persona", "dataPrestec", "retornat");
    }

    // ── I18n: every key has 3 translations ───────────────────────────────────

    @Test
    @DisplayName("I18n: every registered key has all 3 language entries")
    void i18nKeyCompleteness() throws Exception {
        java.lang.reflect.Field f = herramienta.I18n.class.getDeclaredField("TABLE");
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
        src.setNotes("note"); src.setPagines(900); src.setPaginesLlegides(300);
        src.setEditorial("Chilton"); src.setSerie("Dune"); src.setVolum(1);
        src.setDataCompra("2020-01-05"); src.setDataLectura("2021-02-10");
        src.setIdioma("en"); src.setFormat("Tapa dura"); src.setDesitjat(true);
        src.setPaisOrigen("US"); src.setEstat("ok"); src.setExemplars(2);
        src.setLlenguaOriginal("en");
        src.setNomCa("Duna"); src.setNomEs("Dune"); src.setNomEn("Dune");
        src.setHasBlob(true);
        src.setAutors(java.util.List.of("Frank Herbert", "Brian Herbert"));

        Llibre c = Llibre.copyOf(src);
        assertThat(c.getISBN()).isEqualTo(src.getISBN());
        assertThat(c.getNom()).isEqualTo(src.getNom());
        assertThat(c.getAutors()).isEqualTo(src.getAutors());
        assertThat(c.getAny()).isEqualTo(src.getAny());
        assertThat(c.getDescripcio()).isEqualTo(src.getDescripcio());
        assertThat(c.getValoracio()).isEqualTo(src.getValoracio());
        assertThat(c.getPreu()).isEqualTo(src.getPreu());
        assertThat(c.getLlegit()).isEqualTo(src.getLlegit());
        assertThat(c.getNotes()).isEqualTo(src.getNotes());
        assertThat(c.getPagines()).isEqualTo(src.getPagines());
        assertThat(c.getPaginesLlegides()).isEqualTo(src.getPaginesLlegides());
        assertThat(c.getEditorial()).isEqualTo(src.getEditorial());
        assertThat(c.getSerie()).isEqualTo(src.getSerie());
        assertThat(c.getVolum()).isEqualTo(src.getVolum());
        assertThat(c.getIdioma()).isEqualTo(src.getIdioma());
        assertThat(c.getFormat()).isEqualTo(src.getFormat());
        assertThat(c.getDesitjat()).isEqualTo(src.getDesitjat());
        assertThat(c.getPaisOrigen()).isEqualTo(src.getPaisOrigen());
        assertThat(c.getEstat()).isEqualTo(src.getEstat());
        assertThat(c.getExemplars()).isEqualTo(src.getExemplars());
        assertThat(c.getLlenguaOriginal()).isEqualTo(src.getLlenguaOriginal());
        assertThat(c.getNomCa()).isEqualTo(src.getNomCa());
        assertThat(c.getNomEs()).isEqualTo(src.getNomEs());
        assertThat(c.getNomEn()).isEqualTo(src.getNomEn());
        assertThat(c.hasBlob()).isEqualTo(src.hasBlob());
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
    void filterHasAnyFilterPerField() {
        LlibreFilter f = LlibreFilter.empty();
        assertThat(f.hasAnyFilter()).isFalse();
        f.withAutor("x"); assertThat(f.hasAnyFilter()).isTrue(); f.withAutor(null);
        f.withNom("x"); assertThat(f.hasAnyFilter()).isTrue(); f.withNom(null);
        f.withIsbn(1L); assertThat(f.hasAnyFilter()).isTrue(); f.withIsbn(null);
        f.withAnyMin(1); assertThat(f.hasAnyFilter()).isTrue(); f.withAnyMin(null);
        f.withLlegit(true); assertThat(f.hasAnyFilter()).isTrue(); f.withLlegit(null);
        f.withSort(new SortSpec("nom", true)); assertThat(f.hasAnyFilter()).isFalse(); // sort excluded
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
    @DisplayName("DateUtils.parseYear accepts plausible years, returns 0 on garbage")
    void dateUtilsParseYear() {
        assertThat(herramienta.DateUtils.parseYear("1984")).isEqualTo(1984);
        assertThat(herramienta.DateUtils.parseYear("Published 1984")).isEqualTo(1984);
        assertThat(herramienta.DateUtils.parseYear("not a year")).isZero();
        assertThat(herramienta.DateUtils.parseYear(null)).isZero();
    }

    // ── LlibreValidator: thrown branches ─────────────────────────────────────

    @Test
    @DisplayName("LlibreValidator rejects bad ISBN, empty nom, out-of-range valoracio/preu")
    void validatorTableDriven() {
        assertThatThrownBy(() -> LlibreValidator.checkLlibre(1L, "Title", null, null, null, null, null, null, null)).isNotNull();
        assertThatThrownBy(() -> LlibreValidator.checkLlibre(9780306406157L, "", null, null, null, null, null, null, null)).isNotNull();
        assertThatThrownBy(() -> LlibreValidator.checkLlibre(9780306406157L, "T", null, null, null, 11.0, null, null, null)).isNotNull();
        assertThatThrownBy(() -> LlibreValidator.checkLlibre(9780306406157L, "T", null, null, null, null, -1.0, null, null)).isNotNull();
    }

    // ── Llista: setNom blank throws ──────────────────────────────────────────

    @Test
    @DisplayName("Llista.setNom rejects null and blank")
    void llistaSetNomRejectsBlank() {
        Llista l = new Llista(1, "ok");
        assertThatThrownBy(() -> l.setNom(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.setNom("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    // ── Llista: setNom blank throws ──────────────────────────────────────────

    @Test
    @DisplayName("Llista.setNom blank throws IllegalArgumentException")
    void setNomBlankThrows() {
        Llista l = new Llista(1, "valid");
        assertThatThrownBy(() -> l.setNom("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.setNom(null)).isInstanceOf(IllegalArgumentException.class);
    }

    // ── Llista: setColor hex validation ─────────────────────────────────────

    @Test
    @DisplayName("Llista.isValidColor accepts #abc / #aabbcc / null, rejects garbage; setColor is a trust-the-caller setter")
    void setColorHex3DigitAccepted() {
        // Validation is centralised in Llista.isValidColor; the setter trusts the caller
        // (DAO load paths use it with values that were validated on write).
        assertThat(Llista.isValidColor("#abc")).isTrue();
        assertThat(Llista.isValidColor("#aabbcc")).isTrue();
        assertThat(Llista.isValidColor(null)).isTrue();
        assertThat(Llista.isValidColor("red")).isFalse();
        assertThat(Llista.isValidColor("#zzzzzz")).isFalse();
        Llista l = new Llista(1, "ok");
        l.setColor("#abc");
        assertThat(l.getColor()).isEqualTo("#abc");
        l.setColor(null);
        assertThat(l.getColor()).isNull();
    }

    @Test
    @DisplayName("ControladorDomini.setLlistaColor rejects invalid color with Validation")
    void setLlistaColorInvalid() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.addLlista("Shelf");
        assertThatThrownBy(() -> cd.setLlistaColor(l.getId(), "not-a-color"))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
        assertThatThrownBy(() -> cd.setLlistaColor(l.getId(), "#zzzzzz"))
            .isInstanceOf(domini.BibliotecaException.Validation.class);
        // Valid colors are accepted
        cd.setLlistaColor(l.getId(), "#aabbcc");
        assertThat(cd.getAllLlistes().get(0).getColor()).isEqualTo("#aabbcc");
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
        assertThat(new domini.BibliotecaException.NotFound("x").code()).isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
        assertThat(new domini.BibliotecaException.Duplicate("x").code()).isEqualTo(domini.BibliotecaException.Code.DUPLICATE);
        assertThat(new domini.BibliotecaException.Validation("x").code()).isEqualTo(domini.BibliotecaException.Code.VALIDATION);
    }

    // ── Loan: lend → return → lend roundtrip ─────────────────────────────────

    @Test
    @DisplayName("loan → return → loan succeeds")
    void loanReturnLoanRoundtrip() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        cd.prestarLlibre(9780306406157L, "Paul");
        assertThat(cd.getLoanedISBNs()).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.getLoanedISBNs()).doesNotContain(9780306406157L);
        cd.prestarLlibre(9780306406157L, "Jessica");
        assertThat(cd.getLoanedISBNs()).contains(9780306406157L);
    }

    // ── Llista: color validation ─────────────────────────────────────────────

    @Test
    @DisplayName("setLlistaColor: null clears, hex accepted, garbage rejected")
    void setLlistaColorValidates() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.addLlista("Test");
        cd.setLlistaColor(l.getId(), null);
        cd.setLlistaColor(l.getId(), "#abc");
        cd.setLlistaColor(l.getId(), "#aabbcc");
        assertThatThrownBy(() -> cd.setLlistaColor(l.getId(), "red"))
            .hasMessage(herramienta.I18n.t("val_color_invalid", "red"));
        assertThatThrownBy(() -> cd.setLlistaColor(l.getId(), "#zzzzzz"))
            .hasMessage(herramienta.I18n.t("val_color_invalid", "#zzzzzz"));
    }

    // ── Tag: rename to existing throws ───────────────────────────────────────

    @Test
    @DisplayName("renameTag to existing name throws (unique constraint)")
    void renameTagToExistingThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag a = cd.addTag("alpha");
        cd.addTag("beta");
        assertThatThrownBy(() -> cd.renameTag(a.getId(), "beta")).isNotNull();
    }


    @Test
    @DisplayName("clearAll on empty DB still writes a pre_clear backup")
    void clearAllOnEmptyDbWritesBackup() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("biblioteca_test_backup");
        try {
            ControladorDomini cd = ControladorDomini.getInstance();
            File out = new File(tmp.toFile(), "pre_clear_empty.sql");
            synchronized (cd) {
                cd.backupToSQL(out);
                cd.clearAll();
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
    void renameTagToExistingNomThrows() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag a = cd.addTag("alpha");
        cd.addTag("beta");
        assertThatThrownBy(() -> cd.renameTag(a.getId(), "beta")).isNotNull();
    }

    // ── LlistaDao: color null clears and hex roundtrips ────────────────────────

    @Test
    @DisplayName("Llista color: null clears, hex roundtrips via setLlistaColor")
    void llistaColorNullClearsAndHexRoundtrips() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista l = cd.addLlista("Colored");
        cd.setLlistaColor(l.getId(), "#aabbcc");
        Llista found = cd.getAllLlistes().stream()
            .filter(x -> x.getId() == l.getId()).findFirst().orElseThrow();
        assertThat(found.getColor()).isEqualTo("#aabbcc");

        cd.setLlistaColor(l.getId(), null);
        Llista afterClear = cd.getAllLlistes().stream()
            .filter(x -> x.getId() == l.getId()).findFirst().orElseThrow();
        assertThat(afterClear.getColor()).isNull();
    }

    // ── AutorDao: parameterized 0/1/many authors ──────────────────────────────

    @ParameterizedTest(name = "getDistinctAutorNames with {0} books returns expected count")
    @CsvSource({"0, 0", "1, 1", "3, 2"})
    @DisplayName("AutorDao: distinct author names for 0/1/many authors")
    void autorDaoDistinctNamesCount(int bookCount, int expectedAuthorCount) throws Exception {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
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
        assertThat(cd.getDistinctAutorNames()).hasSize(expectedAuthorCount);
    }

    // ── PrestecRow: toDisplayMap ISO date string ──────────────────────────────

    @Test
    @DisplayName("PrestecRow.toDisplayMap encodes dataPrestec as formatted date string")
    void prestecRowToDisplayMapDataPrestec() {
        java.time.LocalDate date = java.time.LocalDate.of(2024, 3, 15);
        persistencia.PrestecRow row = new persistencia.PrestecRow(9780306406157L, "Alice", date, false);
        java.util.Map<String, Object> map = row.toDisplayMap();
        assertThat(map.get("dataPrestec")).isEqualTo("15/03/2024");
    }

    @Test
    @DisplayName("PrestecRow.toDisplayMap: null date yields null")
    void prestecRowToDisplayMapNullDate() {
        persistencia.PrestecRow row = new persistencia.PrestecRow(9780306406157L, "Bob", null, true);
        java.util.Map<String, Object> map = row.toDisplayMap();
        assertThat(map.get("dataPrestec")).isNull();
    }


    @Test
    @DisplayName("DeleteEvent: cancellable=true, veto marks as vetoed")
    void deleteEventCancellableVeto() {
        domini.Llibre book = book(9780306406157L, "Dune");
        presentacio.listener.OnLlibreDelete.DeleteEvent ev = new presentacio.listener.OnLlibreDelete.DeleteEvent(book, true);
        assertThat(ev.isCancellable()).isTrue();
        assertThat(ev.isVetoed()).isFalse();
        ev.veto();
        assertThat(ev.isVetoed()).isTrue();
    }

    @Test
    @DisplayName("DeleteEvent: cancellable=false, veto has no practical effect")
    void deleteEventNonCancellable() {
        domini.Llibre book = book(9780306406157L, "Dune");
        presentacio.listener.OnLlibreDelete.DeleteEvent ev = new presentacio.listener.OnLlibreDelete.DeleteEvent(book, false);
        assertThat(ev.isCancellable()).isFalse();
    }

    // ── H2 in-memory: full CRUD roundtrip ──────────────────────────────────────

    @Test
    @DisplayName("H2 in-memory: full CRUD lifecycle for books, shelves, tags, loans")
    void h2CrudLifecycle() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();

        // Create
        add(cd, 9780306406157L, "Dune", "Frank Herbert", 1965);
        add(cd, 8420413739L, "Foundation", "Isaac Asimov", 1951);
        Llista shelf = cd.addLlista("Sci-Fi");
        Tag tag = cd.addTag("Classic");
        cd.addLlibreToLlista(9780306406157L, shelf.getId(), 9.0, true);
        cd.addLlibreToTag(9780306406157L, tag.getId());
        cd.prestarLlibre(9780306406157L, "Alice");

        // Read
        assertThat(cd.getSize()).isEqualTo(2);
        assertThat(cd.getLlibre(9780306406157L).getNom()).isEqualTo("Dune");
        assertThat(cd.getAllLlistes()).hasSize(1);
        assertThat(cd.getAllTags()).hasSize(1);
        assertThat(cd.getCountInLlista(shelf.getId())).isEqualTo(1);
        assertThat(cd.getTagsForLlibre(9780306406157L)).hasSize(1);
        assertThat(cd.getLoanedISBNs()).contains(9780306406157L);

        // Update
        Llibre updated = LlibreValidator.checkLlibre(9780306406157L, "Dune Updated", "Frank Herbert", 1965, null, 9.5, null, null, null);
        cd.updateLlibre(updated);
        assertThat(cd.getLlibre(9780306406157L).getNom()).isEqualTo("Dune Updated");

        // Delete
        cd.retornarLlibre(9780306406157L);
        cd.removeLlibreFromLlista(9780306406157L, shelf.getId());
        cd.removeLlibreFromTag(9780306406157L, tag.getId());
        cd.deleteLlibre(9780306406157L);
        assertThat(cd.getSize()).isEqualTo(1);
        assertThat(cd.getLoanedISBNs()).isEmpty();
    }

    // ── ServerConect: migration produces complete schema ──────────────────────

    @Test
    @DisplayName("ServerConect: CREATE_TABLE + all migrations produce complete schema")
    void migrationProducesCompleteSchema() throws Exception {
        String url = "jdbc:h2:mem:schema_" + System.nanoTime() + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";
        System.setProperty("biblioteca.h2.url", url);
        try {
            persistencia.ServerConect sc = new persistencia.ServerConect();
            sc.createDatabase();
            try (java.sql.Connection conn = sc.getConnection()) {
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
                sc.closeConection();
            }
        } finally {
            System.setProperty("biblioteca.h2.url",
                "jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
        }
    }

    // ── LlibreDao.search() parameter binding: all types ────────────────────────

    @Test
    @DisplayName("search() binds String, Long, Integer, Double, Boolean params correctly")
    void searchParameterBindingByType() throws Exception {
        // NOTE: H2 MySQL-mode LIKE is case-sensitive; SQL-path queries use exact case.
        // In-memory aplicarFiltres normalizes both sides — SQL path doesn't (documented gap).
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = LlibreValidator.checkLlibre(9780306406157L, "Dune", "Frank Herbert", 1965,
            null, 9.5, 19.99, true, null);
        a.setEditorial("Chilton");
        a.setSerie("Dune Chronicles");
        a.setFormat("Tapa dura");
        a.setIdioma("English");
        cd.addLlibre(a);

        Llibre b = LlibreValidator.checkLlibre(8420413739L, "Foundation", "Isaac Asimov", 1951,
            null, 7.0, 10.0, false, null);
        b.setEditorial("Gnome Press");
        b.setSerie("Foundation");
        b.setFormat("eBook");
        b.setIdioma("Spanish");
        cd.addLlibre(b);

        // String (nom LIKE — uses exact case since H2 LIKE is case-sensitive)
        LlibreFilter fn = domini.LlibreFilterBuilder.of().nom("Du").build();
        assertThat(cd.searchLlibresSQL(fn)).extracting(Llibre::getNom).containsExactly("Dune");

        // String (autor LIKE)
        LlibreFilter fa = domini.LlibreFilterBuilder.of().autor("Herbert").build();
        assertThat(cd.searchLlibresSQL(fa)).extracting(Llibre::getNom).containsExactly("Dune");

        // Long (isbn =)
        LlibreFilter fi = domini.LlibreFilterBuilder.of().isbn(9780306406157L).build();
        assertThat(cd.searchLlibresSQL(fi)).extracting(Llibre::getNom).containsExactly("Dune");

        // Integer (anyMin / anyMax)
        LlibreFilter fay = domini.LlibreFilterBuilder.of().anyMin(1960).build();
        assertThat(cd.searchLlibresSQL(fay)).extracting(Llibre::getNom).containsExactly("Dune");

        // Double (valoracioMin / preuMin)
        LlibreFilter fv = domini.LlibreFilterBuilder.of().valoracioMin(8.0).build();
        assertThat(cd.searchLlibresSQL(fv)).extracting(Llibre::getNom).containsExactly("Dune");
        LlibreFilter fp = domini.LlibreFilterBuilder.of().preuMin(15.0).build();
        assertThat(cd.searchLlibresSQL(fp)).extracting(Llibre::getNom).containsExactly("Dune");

        // Boolean (llegit = true)
        LlibreFilter fl = domini.LlibreFilterBuilder.of().llegit(true).build();
        assertThat(cd.searchLlibresSQL(fl)).extracting(Llibre::getNom).containsExactly("Dune");

        // String (editorial LIKE — exact case)
        LlibreFilter fe = domini.LlibreFilterBuilder.of().editorial("Chilton").build();
        assertThat(cd.searchLlibresSQL(fe)).extracting(Llibre::getNom).containsExactly("Dune");

        // String (format = exact)
        LlibreFilter ff = domini.LlibreFilterBuilder.of().format("Tapa dura").build();
        assertThat(cd.searchLlibresSQL(ff)).extracting(Llibre::getNom).containsExactly("Dune");

        // String (idioma LIKE — exact case)
        LlibreFilter fid = domini.LlibreFilterBuilder.of().idioma("English").build();
        assertThat(cd.searchLlibresSQL(fid)).extracting(Llibre::getNom).containsExactly("Dune");

        // No result
        LlibreFilter fEmpty = domini.LlibreFilterBuilder.of().nom("zzzzz").build();
        assertThat(cd.searchLlibresSQL(fEmpty)).isEmpty();
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
        herramienta.Config.reload();

        assertThat(herramienta.Config.getColVisible(0)).isFalse();
        assertThat(herramienta.Config.getColVisible(3)).isFalse();
        assertThat(herramienta.Config.getColVisible(7)).isTrue();
        assertThat(herramienta.Config.getColVisible(2)).isTrue(); // default

        herramienta.Config.setColVisible(5, false);
        // Wait for async save (300ms scheduler)
        Thread.sleep(500);
        assertThat(herramienta.Config.getColVisible(5)).isFalse();

        // Verify persistence: reload from disk and check
        herramienta.Config.reload();
        assertThat(herramienta.Config.getColVisible(5)).isFalse();

        System.setProperty("user.home", origHome);
        herramienta.Config.reload();
        Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
    }

    // ── resetForProfileSwitch closes and reopens connection ────────────────────

    @Test
    @DisplayName("resetForProfileSwitch closes connection; new instance creates fresh connection")
    void resetForProfileSwitchReconnects() {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        assertThat(cp.countLlibres()).isZero();

        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForProfileSwitch();

        ControladorPersistencia cp2 = ControladorPersistencia.getInstance();
        assertThat(cp2).isNotSameAs(cp);
        assertThat(cp2.countLlibres()).isZero();
    }

    // ── BookExporter golden-file test ────────────────────────────────────

    @Test
    @DisplayName("BookExporter.exportJSON output matches golden fixture structure")
    void bookExporterGoldenFile() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre g = LlibreValidator.checkLlibre(9780000000050L, "GoldenBook", "Author X", 2020, "A description", 7.5, 0.0, true, "");
        cd.addLlibre(g);
        Llista fav = cd.addLlista("Fav");
        cd.addLlibreToLlista(9780000000050L, fav.getId(), 8.0, true);
        Tag fiction = cd.addTag("fiction");
        cd.addLlibreToTag(9780000000050L, fiction.getId());

        File tmp = File.createTempFile("golden_export", ".json");
        tmp.deleteOnExit();
        herramienta.BookExporter.exportJSON(tmp, cd);

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
        herramienta.csv.GoodreadsCsvStrategy gr = new herramienta.csv.GoodreadsCsvStrategy();
        assertThat(gr.canHandle(header)).isTrue();

        String row = "42,The Hobbit,Tolkien,J.R.R. Tolkien,,=\"0000000000\",=\"9780000000042\",5,4.5,HarperCollins,Paperback,310,1937,1937,2024-06-15,2024-05-01,fantasy;classics,read,Awesome,,nope,3";
        String[] headerCols = herramienta.csv.CsvUtils.parseLine(header);
        java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerCols);
        String[] cols = herramienta.csv.CsvUtils.parseLine(row);

        assertThat(gr.parseLine(cols, hMap, cd)).isTrue();
        Llibre l = cd.getLlibre(9780000000042L);
        assertThat(l.getNom()).isEqualTo("The Hobbit");
        assertThat(l.getValoracio()).isEqualTo(10.0);
        assertThat(l.getLlegit()).isTrue();
        assertThat(l.getEditorial()).isEqualTo("HarperCollins");
        assertThat(l.getPagines()).isEqualTo(310);
        assertThat(cd.getAllLlistes()).isNotEmpty();
    }

    // ── LibraryThing CSV fixture ──────────────────────────────────────────

    @Test
    @DisplayName("LibraryThing CSV: BCID column triggers strategy; handles tags, collections, and X-check ISBN")
    void libraryThingCsvFixture() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String header = "Book Id,ISBN,ISBN13,BCID,Title,Authors,Original Publication Year,Publication Year,Rating,Summary,Comments,Review,Collections,Tags";
        herramienta.csv.LibraryThingCsvStrategy lt = new herramienta.csv.LibraryThingCsvStrategy();
        assertThat(lt.canHandle(header)).isTrue();

        String row = "99,,9780000000019,BC123,Test LibBook,Author One; Author Two,2021,2021,3.5,A summary,My notes,,\"My Shelf,Favorites\",fiction;adventure";
        String[] headerCols = herramienta.csv.CsvUtils.parseLine(header);
        java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerCols);
        String[] cols = herramienta.csv.CsvUtils.parseLine(row);

        assertThat(lt.parseLine(cols, hMap, cd)).isTrue();
        Llibre l = cd.getLlibre(9780000000019L);
        assertThat(l.getNom()).isEqualTo("Test LibBook");
        assertThat(l.getValoracio()).isEqualTo(7.0);
        assertThat(cd.getAllTags()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(cd.getAllLlistes()).hasSizeGreaterThanOrEqualTo(1);
    }
// ── Export/import roundtrip preserves shelf memberships (JSON) ────────

    @Test
    @DisplayName("JSON export/import roundtrip preserves shelf memberships with valoracio and llegit")
    void jsonRoundtripShelfMemberships() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre b1 = LlibreValidator.checkLlibre(9780000000060L, "ShelfBook1", "Auth1", 2021, "Desc1", 8.0, 10.0, true, "");
        Llibre b2 = LlibreValidator.checkLlibre(9780000000061L, "ShelfBook2", "Auth2", 2022, "Desc2", 6.0, 5.0, false, "");
        cd.addLlibre(b1);
        cd.addLlibre(b2);

        Llista s1 = cd.addLlista("Sci-Fi");
        Llista s2 = cd.addLlista("Classics");
        cd.addLlibreToLlista(9780000000060L, s1.getId(), 9.0, true);
        cd.addLlibreToLlista(9780000000060L, s2.getId(), 5.0, false);
        cd.addLlibreToLlista(9780000000061L, s1.getId(), 7.5, false);

        Tag t1 = cd.addTag("adventure");
        Tag t2 = cd.addTag("classic");
        cd.addLlibreToTag(9780000000060L, t1.getId());
        cd.addLlibreToTag(9780000000060L, t2.getId());
        cd.addLlibreToTag(9780000000061L, t1.getId());

        File tmp = File.createTempFile("roundtrip_json", ".json");
        tmp.deleteOnExit();
        herramienta.BookExporter.exportJSON(tmp, cd);

        // Re-import into fresh DB
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
        ControladorDomini cd2 = ControladorDomini.getInstance();
        herramienta.BookImporter.ImportResult result = herramienta.BookImporter.importJSON(tmp, cd2);
        assertThat(result.imported()).isEqualTo(2);

        // Verify shelves
        List<Llista> shelves = cd2.getAllLlistes();
        assertThat(shelves).hasSize(2);
        Set<String> shelfNames = shelves.stream().map(Llista::getNom).collect(java.util.stream.Collectors.toSet());
        assertThat(shelfNames).containsExactlyInAnyOrder("Sci-Fi", "Classics");

        // Verify tags
        List<Tag> tags = cd2.getAllTags();
        assertThat(tags).hasSize(2);
        Set<String> tagNames = tags.stream().map(Tag::getNom).collect(java.util.stream.Collectors.toSet());
        assertThat(tagNames).containsExactlyInAnyOrder("adventure", "classic");

        // Verify shelf memberships for book 60
        List<Llista> shelvesFor60 = cd2.getLlistesForLlibre(9780000000060L);
        assertThat(shelvesFor60).hasSize(2);
        List<Tag> tagsFor60 = cd2.getTagsForLlibre(9780000000060L);
        assertThat(tagsFor60).hasSize(2);

        // Verify shelf membership for book 61
        List<Llista> shelvesFor61 = cd2.getLlistesForLlibre(9780000000061L);
        assertThat(shelvesFor61).hasSize(1);
        assertThat(shelvesFor61.get(0).getNom()).isEqualTo("Sci-Fi");
    }

    // ── Config: switching to H2 clears stale host/user ───────────────────

    @Test
    @DisplayName("Config: switching to H2 returns default host/user instead of stale values")
    void configH2ClearsStaleHostUser() throws Exception {
        java.nio.file.Path tmpDir = Files.createTempDirectory("biblioteca_cfg_h2_");
        java.nio.file.Path cfgDir = tmpDir.resolve(".biblioteca");
        Files.createDirectories(cfgDir);
        String origHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tmpDir.toFile().getAbsolutePath());
            herramienta.Config.reload();
            herramienta.Config.setDbType("mariadb");
            herramienta.Config.setDbHost("db.example.com");
            herramienta.Config.setDbUser("admin");
            Thread.sleep(500);

            assertThat(herramienta.Config.getDbType()).isEqualTo("mariadb");
            assertThat(herramienta.Config.getDbHost()).isEqualTo("db.example.com");
            assertThat(herramienta.Config.getDbUser()).isEqualTo("admin");

            // Switch back to H2 — host/user should return defaults
            herramienta.Config.setDbType("h2");
            Thread.sleep(500);
            assertThat(herramienta.Config.getDbType()).isEqualTo("h2");
            assertThat(herramienta.Config.getDbHost()).isEqualTo("localhost");
            assertThat(herramienta.Config.getDbUser()).isEqualTo("user");
        } finally {
            System.setProperty("user.home", origHome);
            herramienta.Config.reload();
            Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        }
    }

    // ── SplashScreen: hide before show is no-op ──────────────────────────

    @Test
    @DisplayName("SplashScreen: hide() before show() is a no-op")
    void splashHideBeforeShow() {
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (ge.isHeadless()) return;
        presentacio.SplashScreen splash = new presentacio.SplashScreen();
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
        assertThat(f.getNom()).isEqualTo("Test");
        assertThat(f.getAnyMin()).isEqualTo(2000);
        assertThat(f.getAnyMax()).isEqualTo(2020);
        assertThat(f.getLlegit()).isFalse();
    }

    // ── LlibreLlistaContext: per-book shelf context ─────────────────────────

    @Test
    @DisplayName("getLlistesForLlibreContext returns shelf with per-book values")
    void llistesForLlibreContext() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Dune");
        Llista shelf = cd.addLlista("Favorites");
        cd.addLlibreToLlista(9780306406157L, shelf.getId(), 9.5, true);
        var ctxList = cd.getLlistesForLlibreContext(9780306406157L);
        assertThat(ctxList).hasSize(1);
        domini.LlibreLlistaContext ctx = ctxList.get(0);
        assertThat(ctx.llistaId()).isEqualTo(shelf.getId());
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
        l.setAutors(java.util.Arrays.asList("A1", "A2", "A3"));
        cd.addLlibre(l);
        Llibre retrieved = cd.getLlibre(9780306406157L);
        assertThat(retrieved.getAutors()).containsExactlyInAnyOrder("A1", "A2", "A3");
    }

    @Test
    @DisplayName("Update book replacing authors preserves new list")
    void updateAuthorsReplacement() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = book(9780306406157L, "ReplaceAuth");
        l.setAutors(java.util.Arrays.asList("Old Author"));
        cd.addLlibre(l);
        l.setAutors(java.util.Arrays.asList("New A", "New B"));
        cd.updateLlibre(l);
        Llibre retrieved = cd.getLlibre(9780306406157L);
        assertThat(retrieved.getAutors()).containsExactlyInAnyOrder("New A", "New B");
        assertThat(retrieved.getAutors()).doesNotContain("Old Author");
    }
// ── AboutDialog: loads license text from /LICENSE resource ────────

    @Test
    @DisplayName("AboutDialog: /LICENSE resource is loadable and contains GPL text")
    void aboutDialogLicenseResource() {
        try (var in = presentacio.AboutDialog.class.getResourceAsStream("/LICENSE")) {
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
        domini.BibliotecaException.NotFound ex = new domini.BibliotecaException.NotFound("Book not found");
        assertThat(ex.code()).isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("BibliotecaException.Validation uses VALIDATION code")
    void bibliotecaExceptionValidation() {
        domini.BibliotecaException.Validation ex = new domini.BibliotecaException.Validation("bad isbn");
        assertThat(ex.code()).isEqualTo(domini.BibliotecaException.Code.VALIDATION);
    }

    // ── PrestecRow: extended overdue days tests ────────────────────────

    @Test
    @DisplayName("PrestecRow: overdueDays returns -1 when dataPrestec is null")
    void prestecRowOverdueDaysNullDate() {
        persistencia.PrestecRow row = new persistencia.PrestecRow(9780000000001L, "Alice", null, false);
        assertThat(row.overdueDays(java.time.LocalDate.now(), 7)).isEqualTo(-1);
    }

    @Test
    @DisplayName("PrestecRow: fromStrings parses ISO date correctly")
    void prestecRowFromStrings() {
        persistencia.PrestecRow row = persistencia.PrestecRow.fromStrings(9780000000001L, "Bob", "2025-06-15", true);
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
        java.util.List<String> vals = cp.getDistinctValues("invalid_column");
        assertThat(vals).isEmpty();
    }

    // ── Config: window placement defaults ────────────────────────────────

    @Test
    @DisplayName("Config: window defaults are sensible")
    void configWindowDefaults() {
        int x = herramienta.Config.getWindowX();
        int y = herramienta.Config.getWindowY();
        int w = herramienta.Config.getWindowWidth();
        int h = herramienta.Config.getWindowHeight();
        assertThat(x).isGreaterThanOrEqualTo(0);
        assertThat(y).isGreaterThanOrEqualTo(0);
        assertThat(w).isGreaterThan(100);
        assertThat(h).isGreaterThan(100);
    }

    // ── LlibreValidator: year range validation ───────────────────────────

    @Test
    @DisplayName("LlibreValidator: year 1900 is accepted")
    void validatorYear1900Accepted() {
        Llibre l = LlibreValidator.checkLlibre(9780000000111L, "OldBook", null, 1900, null, null, null, null, null);
        assertThat(l.getAny()).isEqualTo(1900);
    }

    @Test
    @DisplayName("LlibreValidator: future year within +5 is accepted")
    void validatorFutureYearAccepted() {
        int futureYear = java.time.Year.now().getValue() + 5;
        Llibre l = LlibreValidator.checkLlibre(9780000000222L, "FutureBook", null, futureYear, null, null, null, null, null);
        assertThat(l.getAny()).isEqualTo(futureYear);
    }

    // ── CsvUtils: parseLine edge cases ───────────────────────────────

    @Test
    @DisplayName("CsvUtils: parseLine handles quoted fields with embedded commas")
    void csvUtilsQuotedFieldWithComma() {
        String[] result = herramienta.csv.CsvUtils.parseLine("a,\"b,c\",d");
        assertThat(result).containsExactly("a", "b,c", "d");
    }

    @Test
    @DisplayName("CsvUtils: parseLine handles trailing comma")
    void csvUtilsTrailingComma() {
        String[] result = herramienta.csv.CsvUtils.parseLine("a,b,");
        assertThat(result).containsExactly("a", "b", "");
    }

    @Test
    @DisplayName("CsvUtils: BOM in header is preserved by parseLine; callers must strip it")
    void csvUtilsBomAwareness() {
        String withBom = "\uFEFF" + "isbn,nom,autor";
        String[] result = herramienta.csv.CsvUtils.parseLine(withBom);
        // parseLine does not strip BOM — this is documented behavior
        assertThat(result[0]).contains("isbn");
        // After stripping BOM, parsing works normally
        String cleaned = withBom.replace("\uFEFF", "");
        String[] cleanedResult = herramienta.csv.CsvUtils.parseLine(cleaned);
        assertThat(cleanedResult[0]).isEqualTo("isbn");
    }

    // ── Escapers: HTML/JSON/CSV escaping ──────────────────────────────────

    @Test
    @DisplayName("Escapers.html escapes &, <, >, \", '")
    void escapersHtml() {
        assertThat(herramienta.Escapers.html("<script>alert(\"xss\")</script>"))
            .isEqualTo("&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;");
        assertThat(herramienta.Escapers.html("Tom & Jerry"))
            .isEqualTo("Tom &amp; Jerry");
        assertThat(herramienta.Escapers.html(null))
            .isEmpty();
    }

    @Test
    @DisplayName("Escapers.json escapes backslash, quote, controls")
    void escapersJson() {
        assertThat(herramienta.Escapers.json("hello\nworld"))
            .isEqualTo("hello\\nworld");
        assertThat(herramienta.Escapers.json("a\"b"))
            .isEqualTo("a\\\"b");
        assertThat(herramienta.Escapers.json(null))
            .isEmpty();
    }

    @Test
    @DisplayName("Escapers.csv wraps in quotes and doubles internal quotes")
    void escapersCsv() {
        assertThat(herramienta.Escapers.csv("hello"))
            .isEqualTo("\"hello\"");
        assertThat(herramienta.Escapers.csv("say \"hi\""))
            .isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(herramienta.Escapers.csv(null))
            .isEmpty();
    }

    // ── Config.withBatch: setters within batch defer save ──────────────────

    @Test
    @DisplayName("ControladorDomini: in-memory filter applies sort like SQL path")
    void filterInMemoryAppliesSort() {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre a = new Llibre(9780000000001L, "Zebra", "A", 2020, "", 0.0, 0.0, false, "");
        Llibre b = new Llibre(9780000000002L, "Alpha", "B", 2021, "", 0.0, 0.0, false, "");
        cd.addLlibre(a);
        cd.addLlibre(b);
        LlibreFilter f = domini.LlibreFilterBuilder.of().sort("nom", true).build();
        List<Llibre> sorted = cd.aplicarFiltres(f);
        assertThat(sorted.get(0).getNom()).isEqualTo("Alpha");
        assertThat(sorted.get(1).getNom()).isEqualTo("Zebra");
    }

    @Test
    @DisplayName("FormFieldRegistry links JLabel to field for accessibility")
    void formFieldRegistryLinkLabel() {
        var registry = new presentacio.FormFieldRegistry();
        javax.swing.JLabel lbl = new javax.swing.JLabel("ISBN");
        javax.swing.JTextField tf = new javax.swing.JTextField();
        registry.linkLabel(lbl, tf);
        assertThat(lbl.getLabelFor()).isSameAs(tf);
    }

    @Test
    @DisplayName("BibliotecaTableModel backs row count from book list")
    void bibliotecaTableModelSetBooks() {
        var model = new presentacio.BibliotecaTableModel();
        Llibre l = new Llibre(9780306406157L, "Test", "Author", 2020, "", 5.0, 10.0, true, "");
        model.setBooks(java.util.List.of(l));
        assertThat(model.getRowCount()).isOne();
        assertThat(model.getBookAt(0).getISBN()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("LlibreValidator.validateInto updates target without replacing extras")
    void llibreValidatorValidateInto() {
        Llibre l = new Llibre(9780306406157L, "Old", "A", 2000, "desc", 1.0, 2.0, false, "");
        l.setNotes("keep notes");
        l.setPagines(400);
        LlibreValidator.validateInto(l, 9780306406157L, "New Title", "Author", 2020,
            "desc", 5.0, 10.0, true, "");
        assertThat(l.getNom()).isEqualTo("New Title");
        assertThat(l.getNotes()).isEqualTo("keep notes");
        assertThat(l.getPagines()).isEqualTo(400);
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
            herramienta.Config.reload();
            herramienta.Config.setDarkMode(true);
            Thread.sleep(500);
            herramienta.Config.withBatch(() -> {
                herramienta.Config.setDarkMode(false);
                herramienta.Config.setFontSize("large");
            });
            Thread.sleep(500);
            assertThat(herramienta.Config.isDarkMode()).isFalse();
            assertThat(herramienta.Config.getFontSize()).isEqualTo("large");
            herramienta.Config.reload();
            assertThat(herramienta.Config.isDarkMode()).isFalse();
            assertThat(herramienta.Config.getFontSize()).isEqualTo("large");
        } finally {
            System.setProperty("user.home", origHome);
            herramienta.Config.reload();
            Files.walk(tmpDir).sorted(java.util.Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        }
    }
@Test
    @DisplayName("MostrarBibliotecaControl: clearCoverCache is safe to call")
    void clearCoverCacheSmoke() {
        presentacio.MostrarBibliotecaControl.clearCoverCache();
    }

    @Test
    @DisplayName("LlegitCheckBoxRenderer: marks read when cell value matches I18n filter_read")
    void llegitRendererShowsReadState() {
        var renderer = new presentacio.renderers.LlegitCheckBoxRenderer();
        javax.swing.JTable table = new javax.swing.JTable();
        java.awt.Component c = renderer.getTableCellRendererComponent(
            table, herramienta.I18n.t("filter_read"), false, false, 0, 0);
        assertThat(c).isInstanceOf(javax.swing.JCheckBox.class);
        assertThat(((javax.swing.JCheckBox) c).isSelected()).isTrue();
        java.awt.Component c2 = renderer.getTableCellRendererComponent(
            table, herramienta.I18n.t("filter_unread"), false, false, 0, 0);
        assertThat(((javax.swing.JCheckBox) c2).isSelected()).isFalse();
    }

    @Test
    @DisplayName("FormValidator: invalid field gets red border")
    void formValidatorInvalidBorder() {
        javax.swing.JTextField tf = new javax.swing.JTextField("bad");
        presentacio.FormValidator.validateField(tf, false);
        assertThat(tf.getBorder()).isNotNull();
        presentacio.FormValidator.validateField(tf, true);
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
                    cd.addLlibre(new Llibre(isbn, "T" + isbn, "A", 2020, "", 0.0, 0.0, false, ""));
                    cd.getAllLlibres().size();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }));
        }
        for (Thread t : threads) t.start();
        start.countDown();
        for (Thread t : threads) t.join(5000);
        assertThat(errors.get()).isZero();
        assertThat(cd.getAllLlibres().size()).isGreaterThanOrEqualTo(8);
    }

    // ── CsvUtils extended ────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "0-306-40615-X, 9780306406157",
        "978-0-306-40615-7, 9780306406157",
        "'', ''"
    })
    @DisplayName("CsvUtils.parseIsbn normalizes ISBN-10 X and ISBN-13")
    void parseIsbnNormalization(String raw, String expected) {
        assertThat(herramienta.csv.CsvUtils.parseIsbn(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("CsvUtils.colVal missing column returns empty string")
    void colValMissingColumn() {
        var h = herramienta.csv.CsvUtils.buildHeaderMap(new String[]{"ISBN"});
        assertThat(herramienta.csv.CsvUtils.colVal(h, new String[]{"978"}, "Title")).isEmpty();
    }

    @Test
    @DisplayName("CsvUtils.parseDoubleOrZero handles invalid input")
    void parseDoubleOrZeroInvalid() {
        assertThat(herramienta.csv.CsvUtils.parseDoubleOrZero("x")).isZero();
        assertThat(herramienta.csv.CsvUtils.parseDoubleOrZero(" 3.5 ")).isEqualTo(3.5);
    }

    @Test
    @DisplayName("CsvUtils.csvQ escapes quotes and null")
    void csvQEscaping() {
        assertThat(herramienta.csv.CsvUtils.csvQ("a\"b")).isEqualTo("\"a\"\"b\"");
        assertThat(herramienta.csv.CsvUtils.csvQ(null)).isEmpty();
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
        Llibre l = LlibreValidator.checkLlibre(9780306406157L, "Main", null, null, null, null, null, null, null);
        l.setNomCa("Català");
        l.setNomEn("English");
        assertThat(l.getDisplayNom("ca")).isEqualTo("Català");
        assertThat(l.getDisplayNom("en")).isEqualTo("English");
        assertThat(l.getDisplayNom("de")).isEqualTo("Main");
    }

    // ── CoverService ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("CoverService cache miss returns null")
    void coverServiceCacheMiss() {
        assertThat(herramienta.CoverService.getCachedBytes("0000000000999")).isNull();
        assertThat(herramienta.CoverService.getCachedImage("0000000000998")).isNull();
    }

    // ── Domain: tags and loans ───────────────────────────────────────────────

    @Test
    @DisplayName("Tag rename and delete")
    void tagRenameDelete() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag t = cd.addTag("old");
        cd.renameTag(t.getId(), "new");
        assertThat(cd.getAllTags()).extracting(Tag::getNom).containsExactly("new");
        cd.deleteTag(t);
        assertThat(cd.getAllTags()).isEmpty();
    }

    @Test
    @DisplayName("Loan prestar/retornar round-trip")
    void loanRoundTrip() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        cd.addLlibre(book(9780306406157L, "Loan"));
        cd.prestarLlibre(9780306406157L, "Bob");
        assertThat(cd.getLoanedISBNs()).contains(9780306406157L);
        cd.retornarLlibre(9780306406157L);
        assertThat(cd.getLoanedISBNs()).doesNotContain(9780306406157L);
    }

    @Test
    @DisplayName("clearAll wipes books and shelves")
    void clearAllWipesData() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "X");
        cd.addLlista("Shelf");
        cd.clearAll();
        assertThat(cd.getSize()).isZero();
        assertThat(cd.getAllLlistes()).isEmpty();
    }

    // ── CsvUtils.existsInLibrary ─────────────────────────────────────────────

    @Test
    @DisplayName("CsvUtils.existsInLibrary reflects domain state")
    void existsInLibrary() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        add(cd, 9780306406157L, "Present");
        assertThat(herramienta.csv.CsvUtils.existsInLibrary(cd, 9780306406157L)).isTrue();
        assertThat(herramienta.csv.CsvUtils.existsInLibrary(cd, 9780000000001L)).isFalse();
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
    void createFactoryReturnsFreshInstance() {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        ControladorDomini a = ControladorDomini.create(cp);
        ControladorDomini b = ControladorDomini.create(cp);
        assertThat(a).isNotSameAs(b);
        assertThat(a).isNotSameAs(ControladorDomini.getInstance());
    }

    // ── Lookup by id (Llista / Tag) ──────────────────────────────────────────

    @Test
    @DisplayName("getLlistaById returns matching shelf")
    void getLlistaByIdFindsShelf() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llista shelf = cd.addLlista("Fiction");
        assertThat(cd.getLlistaById(shelf.getId()).getNom()).isEqualTo("Fiction");
    }

    @Test
    @DisplayName("getLlistaById throws on missing id")
    void getLlistaByIdMissingThrows() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.getLlistaById(99_999))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getTagById returns matching tag")
    void getTagByIdFindsTag() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Tag tag = cd.addTag("ToRead");
        assertThat(cd.getTagById(tag.getId()).getNom()).isEqualTo("ToRead");
    }

    @Test
    @DisplayName("getTagById throws on missing id")
    void getTagByIdMissingThrows() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.getTagById(99_999))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("99");
    }

    // ── Llibre autor / autors invariant ──────────────────────────────────────

    @Test
    @DisplayName("Llibre.setAutor keeps autors list in sync")
    void llibreSetAutorKeepsAutorsInSync() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.setAutor("Alice");
        assertThat(l.getAutor()).isEqualTo("Alice");
        assertThat(l.getAutors()).containsExactly("Alice");
    }

    @Test
    @DisplayName("Llibre.setAutors reflects in getAutor")
    void llibreSetAutorsReflectsInGetAutor() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.setAutors(java.util.List.of("Alice", "Bob"));
        assertThat(l.getAutor()).isEqualTo("Alice, Bob");
    }

    @Test
    @DisplayName("Llibre.setAutor(null) clears autors list")
    void llibreSetAutorNullClearsAutors() {
        domini.Llibre l = new domini.Llibre(9780306406157L, "T", null, null, null, null, null, null, null);
        l.setAutors(java.util.List.of("Alice"));
        l.setAutor(null);
        assertThat(l.getAutors()).isEmpty();
        assertThat(l.getAutor()).isEmpty();
    }

    // ── getDistinctValues: dual-path behavior ────────────────────────────────

    @Test
    @DisplayName("getDistinctValues: in-memory path returns distinct sorted values for editorial")
    void getDistinctValuesInMemoryEditorial() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre a = herramienta.LlibreValidator.checkLlibre(9780306406157L, "A", null, null, null, null, null, null, null);
        domini.Llibre b = herramienta.LlibreValidator.checkLlibre(9780000000001L, "B", null, null, null, null, null, null, null);
        a.setEditorial("Penguin");
        b.setEditorial("Ace");
        cd.addLlibre(a);
        cd.addLlibre(b);
        assertThat(cd.getDistinctValues("editorial")).contains("Ace", "Penguin");
    }

    @Test
    @DisplayName("getDistinctValues: SQL fallback returns empty for unknown column")
    void getDistinctValuesUnknownColumnEmpty() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThat(cd.getDistinctValues("not_a_column")).isEmpty();
        assertThat(cd.getDistinctValues("any")).isEmpty();
    }

    // ── OpenLibraryClient: number_of_pages=null must not NPE ──────────────────

    @Test
    @DisplayName("OpenLibraryClient.lookupByISBN tolerates JSON null for number_of_pages")
    void openLibraryNullNumberOfPagesTolerated() throws Exception {
        com.sun.net.httpserver.HttpServer srv = com.sun.net.httpserver.HttpServer.create(
            new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0), 0);
        srv.createContext("/", ex -> {
            String body = "{\"ISBN:9780000000001\":{\"title\":\"X\",\"number_of_pages\":null,\"publish_date\":\"2001\"}}";
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            try (var os = ex.getResponseBody()) { os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        });
        srv.start();
        try {
            String prev = herramienta.OpenLibraryClient.testBaseUrl;
            int prevRetries = herramienta.OpenLibraryClient.testMaxRetries;
            long prevBase = herramienta.OpenLibraryClient.testRetryBaseMs;
            herramienta.OpenLibraryClient.testBaseUrl = "http://localhost:" + srv.getAddress().getPort();
            herramienta.OpenLibraryClient.testMaxRetries = 0;
            herramienta.OpenLibraryClient.testRetryBaseMs = 0;
            try {
                java.util.Map<String, String> r = herramienta.OpenLibraryClient.lookupByISBN("9780000000001");
                // title present, no NPE thrown, no "error" key
                assertThat(r).containsKey("title");
                assertThat(r).doesNotContainKey("error");
                assertThat(r).doesNotContainKey("pagines");
            } finally {
                herramienta.OpenLibraryClient.testBaseUrl = prev;
                herramienta.OpenLibraryClient.testMaxRetries = prevRetries;
                herramienta.OpenLibraryClient.testRetryBaseMs = prevBase;
            }
        } finally {
            srv.stop(0);
        }
    }

    // ── NotFound / Duplicate / Validation exception codes ────────────────────

    @Test
    @DisplayName("BibliotecaException.NotFound uses NOT_FOUND code")
    void notFoundCode() {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        cd.addLlibre(l);
        assertThatThrownBy(() -> cd.getLlibre(9780000000001L))
            .isInstanceOf(domini.BibliotecaException.NotFound.class)
            .extracting("code").isEqualTo(domini.BibliotecaException.Code.NOT_FOUND);
    }

    @Test
    @DisplayName("Duplicate ISBN throws BibliotecaException.Duplicate")
    void duplicateCode() {
        ControladorDomini cd = ControladorDomini.getInstance();
        domini.Llibre l = LlibreValidator.checkLlibre(9780306406157L, "X", null, null, null, null, null, null, null);
        cd.addLlibre(l);
        assertThatThrownBy(() -> cd.addLlibre(l))
            .isInstanceOf(domini.BibliotecaException.Duplicate.class);
    }

    @Test
    @DisplayName("getLlistaById throws BibliotecaException.NotFound for unknown id")
    void getLlistaByIdNotFound() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.getLlistaById(9999))
            .isInstanceOf(domini.BibliotecaException.NotFound.class);
    }

    @Test
    @DisplayName("getTagById throws BibliotecaException.NotFound for unknown id")
    void getTagByIdNotFound() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.getTagById(9999))
            .isInstanceOf(domini.BibliotecaException.NotFound.class);
    }

    @Test
    @DisplayName("deleteLlibre throws BibliotecaException.NotFound for unknown ISBN")
    void deleteLlibreNotFound() {
        ControladorDomini cd = ControladorDomini.getInstance();
        assertThatThrownBy(() -> cd.deleteLlibre(9780000000001L))
            .isInstanceOf(domini.BibliotecaException.NotFound.class);
    }
}

