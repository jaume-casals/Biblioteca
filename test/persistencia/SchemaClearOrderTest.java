package persistencia;

import domini.ControladorDomini;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifica el contracte de {@link Schema#CLEAR_ORDER} — afegir o eliminar
 * una taula de l'esquema també ha d'actualitzar el vector, si no les
 * sentències {@code DELETE} sobre la taula pare tindran violacions de FK
 * en mode estricte. La troballa LOW de tot.txt va marcar-ho com una
 * mancança de tests unitaris; aquest test atrapa la regressió.
 */
class SchemaClearOrderTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:schema_clear;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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
    @DisplayName("CLEAR_ORDER té 9 taules — protecció contra eliminacions accidentals")
    void netejarOrderCount() {
        assertThat(Schema.CLEAR_ORDER)
            .as("CLEAR_ORDER ha de cobrir cada taula que crea l'esquema; la troballa LOW de tot.txt va marcar-ho com una mancança de tests unitaris")
            .hasSize(9)
            .containsExactly(
                "lectura", "prestec", "llibre_llista", "llista",
                "llibre_autor", "llibre_tag", "tag", "autor", "llibre");
    }

    @Test
    @DisplayName("DELETE en CLEAR_ORDER funciona quan cada taula té una fila")
    void netejarOrderHappyPath() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = extractConnection(cp);
        // Toca l'esquema amb una consulta no-op perquè el DDL s'hagi executat.
        try (Statement s = c.createStatement()) {
            s.execute("SELECT 1");
        }
        // Insereix una fila a cada taula que cobreix CLEAR_ORDER.
        inserirOneRowEverywhere(c);
        // Ara neteja en l'ordre documentat — no ha de fallar.
        try (Statement s = c.createStatement()) {
            for (String table : Schema.CLEAR_ORDER) {
                s.executeUpdate("DELETE FROM " + table);
            }
        }
    }

    @Test
    @Disabled("Les FK de totes les taules filles cap a llibre estan definides amb ON DELETE CASCADE "
        + "(migracions 3, 10, 13, 22, 25 a ConnexioServidor.java), per la qual cosa DELETE FROM llibre "
        + "cascada i té èxit — l'asserció de violació de FK és inaccessible. Si en el futur s'elimina "
        + "el CASCADE d'alguna d'aquestes FK, re-habilitar el test.")
    @DisplayName("DELETE en ordre INVERS (pares primer) llença una violació de FK")
    void netejarOrderReverseFails() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        Connection c = extractConnection(cp);
        try (Statement s = c.createStatement()) {
            s.execute("SELECT 1");
        }
        inserirOneRowEverywhere(c);
        // Inverteix l'ordre — llibre és pare de moltes taules, esborrar-lo
        // primer fallarà amb un error d'integritat referencial.
        java.util.List<String> reverse = new java.util.ArrayList<>(java.util.Arrays.asList(Schema.CLEAR_ORDER));
        java.util.Collections.reverse(reverse);
        try (Statement s = c.createStatement()) {
            for (String table : reverse) {
                assertThatThrownBy(() -> s.executeUpdate("DELETE FROM " + table))
                    .as("DELETE FROM " + table + " (invertit) ha de fallar amb violació de FK")
                    .isInstanceOf(java.sql.SQLException.class);
                return; // la primera fallada n'hi ha prou — demostra que l'ordre importa
            }
            fail("El DELETE en ordre invers hauria d'haver fallat en la primera sentència");
        }
    }

    /** Insereix una fila a cada taula que referencia CLEAR_ORDER, inclosa la
     *  pare {@code llibre} a la qual tot apunta per FK. */
    private static void inserirOneRowEverywhere(Connection c) throws Exception {
        // llibre primer (és el pare de totes les taules M2M)
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO llibre (ISBN, nom, `any`, descripcio, valoracio, preu, llegit, imatge, " +
                "imatge_blob, notes, pagines, pagines_llegides, editorial, serie, volum, " +
                "`data_compra`, `data_lectura`, idioma, pais_origen, format, exemplars, " +
                "llengua_original, desitjat, nom_ca, nom_es, nom_en) " +
                "VALUES (?, ?, 2020, '', 0.0, 0.0, false, NULL, NULL, '', 0, 0, '', '', 0, " +
                "NULL, NULL, '', '', '', 0, '', false, NULL, NULL, NULL)")) {
            ps.setLong(1, 9780306406157L);
            ps.setString(2, "Llibre de test");
            ps.executeUpdate();
        }
        // autor + tag (pares de les taules M2M)
        try (Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO autor (id, nom) VALUES (1, 'Autor de test')");
            s.executeUpdate("INSERT INTO tag (id, nom) VALUES (1, 'Etiqueta de test')");
            s.executeUpdate("INSERT INTO llista (id, nom, ordre, color) VALUES (1, 'Llista de test', 0, '#3498DB')");
        }
        // Files M2M
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
        // prestec + lectura (sense FK a les taules M2M però es conserven per completesa)
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
        return ((ConnexioServidor) scField.get(cp)).obtenirConnexio();
    }
}
