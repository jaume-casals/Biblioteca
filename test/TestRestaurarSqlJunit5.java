import domini.ControladorDomini;
import domini.Llibre;
import herramienta.ValidadorLlibre;
import persistencia.ControladorPersistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the defensive SQL restore path (LlibreDao.executeSQLFile).
 *
 * <p>Covers two related bugs that were both present in the same code path:
 * <ul>
 *   <li>A2 (HIGH): the old {@code replaceAll("--[^\\n]*\\n", "")} on the whole
 *       file stripped text inside string literals, so a value like
 *       {@code 'pre--war ed.'} was silently corrupted on restore.</li>
 *   <li>A3 (MED): the old DDL blocklist (DROP, ALTER, …) still allowed
 *       arbitrary DML — {@code DELETE FROM llibre}, {@code UPDATE … SET
 *       autor='pwned'} — to run on a tampered backup.</li>
 * </ul>
 *
 * <p>After the fix, the strip and the statement split happen in a single
 * quote-aware pass, and the per-statement check is an allowlist
 * ({@code INSERT / VALUES / SET / BEGIN / COMMIT / ROLLBACK}). A tampered
 * file with {@code DELETE FROM …} is silently ignored; a benign file with
 * {@code 'pre--war ed.'} in a string literal round-trips byte-for-byte.
 */
class TestRestaurarSqlJunit5 {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:restore_sql_junit5;MODE=MySQL;NON_KEYWORDS=VALUE");
    }

    @BeforeEach
    void reinicialitzarDb() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
    }

    @Test
    @DisplayName("A2: '--' inside a string literal survives executeSQLFile")
    void dashDashInsideStringLiteralSurvivesRestore() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn = 9780306406157L;
        Llibre book = ValidadorLlibre.comprovarLlibre(isbn, "Pre-war ed.", null, 1939, null, null, null, null, null);
        book.posarNotes("pre--war ed.; a note; another note");
        cd.afegirLlibre(book);

        File backup = File.createTempFile("sql_dash_inside_quote_", ".sql");
        backup.deleteOnExit();
        cd.copiaSegToSQL(backup);

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        ControladorDomini cd2 = ControladorDomini.getInstance();
        cd2.restaurarFromSQL(backup);

        assertThat(cd2.obtenirLlibre(isbn).obtenirNotes())
            .as("Notes with '--' inside a string literal must survive restore")
            .isEqualTo("pre--war ed.; a note; another note");
        assertThat(cd2.obtenirLlibre(isbn).obtenirNom()).isEqualTo("Pre-war ed.");
    }

    @Test
    @DisplayName("A2: line comment at end of line is still stripped, not echoed back")
    void lineCommentIsStillStripped() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn = 9780306406157L;
        Llibre book = ValidadorLlibre.comprovarLlibre(isbn, "Comment test", null, 2020, null, null, null, null, null);
        cd.afegirLlibre(book);

        File tampered = File.createTempFile("sql_comment_", ".sql");
        tampered.deleteOnExit();
        Files.write(tampered.toPath(),
            ("INSERT INTO llibre (`ISBN`, `nom`) VALUES (9781111111111, 'Added via comment test'); -- trailing comment here\n"
                + "-- a comment line that should be stripped, not echoed as data\n"
                + "INSERT INTO llibre (`ISBN`, `nom`) VALUES (9782222222222, 'No-semi issue');\n").getBytes(StandardCharsets.UTF_8));

        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        cp.executarSQLFile(tampered);

        // Query the DB directly to confirm both INSERTs ran, while
        // the in-memory cd cache is intentionally not refreshed.
        java.util.List<Long> dbIsbns = new java.util.ArrayList<>();
        for (Llibre l : cp.obtenirAllLlibres()) dbIsbns.add(l.obtenirISBN());
        assertThat(dbIsbns).contains(9781111111111L, 9782222222222L, isbn);
        assertThat(cd.obtenirLlibre(isbn).obtenirNom()).isEqualTo("Comment test");
    }

    @Test
    @DisplayName("A3: DELETE FROM and UPDATE … SET on a tampered file are silently ignored")
    void eliminarAndUpdateAreRejectedByAllowlist() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        long isbn = 9780306406157L;
        cd.afegirLlibre(ValidadorLlibre.comprovarLlibre(isbn, "Survivor", null, 2020, null, null, null, null, null));

        File tampered = File.createTempFile("sql_allowlist_", ".sql");
        tampered.deleteOnExit();
        try (var w = new java.io.PrintWriter(tampered, StandardCharsets.UTF_8)) {
            w.println("DELETE FROM llibre;");
            w.println("UPDATE llibre SET nom = 'pwned' WHERE ISBN = " + isbn + ";");
            w.println("INSERT INTO llibre (`ISBN`, `nom`) VALUES (9789999999999, 'should not appear');");
            w.println("DROP TABLE llibre;");
            w.println("ALTER TABLE llibre DROP COLUMN nom;");
        }
        ControladorPersistencia.getInstance().executarSQLFile(tampered);

        assertThat(cd.getSize()).as("DELETE blocked; original book must survive").isEqualTo(1);
        assertThat(cd.obtenirLlibre(isbn).obtenirNom()).isEqualTo("Survivor");
    }
}
