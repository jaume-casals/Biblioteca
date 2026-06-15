package persistencia;

import domini.ControladorDomini;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifies the {@link Schema#CLEAR_ORDER} contract — adding or removing a
 * table from the schema should also update the array, otherwise
 * {@code DELETE} statements on the parent table will hit FK violations
 * in strict mode. The tot.txt LOW finding flagged this as a unit-test
 * gap; this test catches the regression.
 */
class SchemaClearOrderTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:schema_clear;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorDomini.resetForTest();
        ControladorPersistencia.resetForTest();
    }

    @Test
    @DisplayName("CLEAR_ORDER has 9 tables — a guard against accidental removal")
    void clearOrderCount() {
        assertThat(Schema.CLEAR_ORDER)
            .as("CLEAR_ORDER must cover every table the schema creates; the tot.txt LOW finding flagged this as a unit-test gap")
            .hasSize(9)
            .containsExactly(
                "lectura", "prestec", "llibre_llista", "llista",
                "llibre_autor", "llibre_tag", "tag", "autor", "llibre");
    }

    @Test
    @DisplayName("DELETE in CLEAR_ORDER succeeds when every table has a row")
    void clearOrderHappyPath() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = extractConnection(cp);
        // Touch the schema by issuing a no-op query so the DDL has run.
        try (Statement s = c.createStatement()) {
            s.execute("SELECT 1");
        }
        // Insert one row into every table the CLEAR_ORDER covers.
        insertOneRowEverywhere(c);
        // Now clear in the documented order — must not throw.
        try (Statement s = c.createStatement()) {
            for (String table : Schema.CLEAR_ORDER) {
                s.executeUpdate("DELETE FROM " + table);
            }
        }
    }

    @Test
    @DisplayName("DELETE in REVERSE order (parents first) raises a FK violation")
    void clearOrderReverseFails() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = extractConnection(cp);
        try (Statement s = c.createStatement()) {
            s.execute("SELECT 1");
        }
        insertOneRowEverywhere(c);
        // Reverse the order — llibre is a parent of many tables, deleting
        // it first will fail with a referential integrity error.
        java.util.List<String> reverse = new java.util.ArrayList<>(java.util.Arrays.asList(Schema.CLEAR_ORDER));
        java.util.Collections.reverse(reverse);
        try (Statement s = c.createStatement()) {
            for (String table : reverse) {
                assertThatThrownBy(() -> s.executeUpdate("DELETE FROM " + table))
                    .as("DELETE FROM " + table + " (reversed) must fail with FK violation")
                    .isInstanceOf(java.sql.SQLException.class);
                return; // first failure is enough — proves order matters
            }
            fail("Reverse-order DELETE should have failed on the first statement");
        }
    }

    /** Insert one row in every table CLEAR_ORDER references, including the
     *  parent {@code llibre} that everything FKs into. */
    private static void insertOneRowEverywhere(Connection c) throws Exception {
        // llibre first (it's the parent of all the M2M tables)
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO llibre (ISBN, nom, `any`, descripcio, valoracio, preu, llegit, imatge, " +
                "imatge_blob, notes, pagines, pagines_llegides, editorial, serie, volum, " +
                "`data_compra`, `data_lectura`, idioma, pais_origen, format, exemplar, " +
                "llengua_original, desitjat, nom_ca, nom_es, nom_en) " +
                "VALUES (?, ?, 2020, '', 0.0, 0.0, false, NULL, NULL, '', 0, 0, '', '', 0, " +
                "NULL, NULL, '', '', '', 0, '', false, NULL, NULL, NULL)")) {
            ps.setLong(1, 9780306406157L);
            ps.setString(2, "Test Book");
            ps.executeUpdate();
        }
        // autor + tag (parents of M2M tables)
        try (Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO autor (id, nom) VALUES (1, 'Test Author')");
            s.executeUpdate("INSERT INTO tag (id, nom) VALUES (1, 'Test Tag')");
            s.executeUpdate("INSERT INTO llista (id, nom, ordre, color) VALUES (1, 'Test List', 0, '#3498DB')");
        }
        // M2M rows
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO llibre_autor (ISBN, autor_id) VALUES (?, ?)")) {
            ps.setLong(1, 9780306406157L); ps.setInt(2, 1); ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO llibre_tag (ISBN, tag_id) VALUES (?, ?)")) {
            ps.setLong(1, 9780306406157L); ps.setInt(2, 1); ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO llibre_llista (ISBN, llista_id, valoracio, llegit) VALUES (?, ?, 0.0, false)")) {
            ps.setLong(1, 9780306406157L); ps.setInt(2, 1); ps.executeUpdate();
        }
        // prestec + lectura (no FK into the M2M tables but kept for completeness)
        try (Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO prestec (ISBN, nom_persona, data_prestec, retornat) " +
                "VALUES (9780306406157, 'Alice', '2024-01-01', false)");
            s.executeUpdate("INSERT INTO lectura (ISBN, data_inici, data_fi, pagines_llegides) " +
                "VALUES (9780306406157, '2024-01-01', NULL, 0)");
        }
    }

    private static Connection extractConnection(ControladorPersistencia cp) throws Exception {
        var scField = ControladorPersistencia.class.getDeclaredField("sc");
        scField.setAccessible(true);
        return ((ServerConect) scField.get(cp)).getConnection();
    }
}
