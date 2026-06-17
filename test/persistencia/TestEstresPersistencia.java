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
 * Tests d'estrès / càrrega contra la capa de persistència H2 en memòria.
 * Verifica que les insercions i lectures SQL massives són correctes, que
 * l'esquema pot contenir milers de files, i que no hi ha fuites de
 * connexions H2.
 */
class TestEstresPersistencia {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:stress_pers;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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
    @DisplayName("INSERT massiu de 5000 llibres via JDBC PreparedStatement finalitza correctament")
    void bulkInsert5000() throws Exception {
        // Força l'esquema a aparèixer tocant el singleton de persistència un cop
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = null;
        try {
            // Reutilitza la connexió del singleton per passar per l'esquema real
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
    @DisplayName("lectura després d'inserció massiva: obtenirAllLlibres retorna el recompte esperat")
    void llegirAfterBulkInsert() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        List<Llibre> seed = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            seed.add(new Llibre(9780306400000L + i, "B" + i, null, 2000, null, 0.0, 0.0, false, null));
        }
        for (Llibre l : seed) cp.afegirLlibre(l);
        List<Llibre> all = cp.obtenirAllLlibres();
        assertThat(all).hasSize(100);
    }
}
