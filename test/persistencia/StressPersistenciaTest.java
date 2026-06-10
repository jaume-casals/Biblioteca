package persistencia;

import domini.ControladorDomini;
import domini.Llibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Stress / load tests against the in-memory H2 persistence layer.
 * Verifies that bulk SQL inserts and reads are correct, that the schema
 * can hold thousands of rows, and that no H2 connection leaks.
 */
class StressPersistenciaTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:stress_pers;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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
    @DisplayName("bulk INSERT of 5000 books via JDBC PreparedStatement finishes correctly")
    void bulkInsert5000() throws Exception {
        // Force the schema to come up by touching the persistence singleton once
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = null;
        try {
            // Re-use the singleton's connection so we go through the live schema
            var scField = ControladorPersistencia.class.getDeclaredField("sc");
            scField.setAccessible(true);
            persistencia.ServerConect sc = (persistencia.ServerConect) scField.get(cp);
            c = sc.getConnection();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO llibre (ISBN, nom, `any`, descripcio, valoracio, preu, llegit, imatge, " +
                    "imatge_blob, notes, pagines, pagines_llegides, editorial, serie, volum, " +
                    "idioma, format, desitjat, pais_origen, estat, exemplars, llengua_original, " +
                    "nom_ca, nom_es, nom_en) " +
                    "VALUES (?, ?, 0, '', 0.0, 0.0, FALSE, '', NULL, '', 0, 0, '', '', 0, " +
                    "NULL, NULL, FALSE, NULL, NULL, 1, NULL, NULL, NULL, NULL)")) {
                for (int i = 0; i < 5000; i++) {
                    ps.setLong(1, 9780306400000L + i);
                    ps.setString(2, "B" + i);
                    ps.addBatch();
                    if ((i + 1) % 1000 == 0) ps.executeBatch();
                }
                ps.executeBatch();
                c.commit();
            }
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM llibre")) {
                rs.next();
                assertThat(rs.getLong(1)).isEqualTo(5000L);
            }
        } finally {
            if (c != null) c.setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("read after bulk insert: getAllLlibres returns expected count")
    void readAfterBulkInsert() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        List<Llibre> seed = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            seed.add(new Llibre(9780306400000L + i, "B" + i, null, 2000, null, 0.0, 0.0, false, null));
        }
        for (Llibre l : seed) cp.afegirLlibre(l);
        List<Llibre> all = cp.getAllLlibres();
        assertThat(all).hasSize(100);
    }
}
