package herramienta.io;

import domini.Llibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.*;

import persistencia.internal.ControladorPersistencia;

/**
 * Còpia de seguretat SQL: la restaura ha de preservar el contingut i una
 * restauració amb un fitxer que no és una còpia de seguretat no ha de deixar
 * la base de dades buida.
 */
class TestCopiaSeguretatRoundTrip {

    private static final long ISBN = 9780306406157L;

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:copia_seguretat;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    @AfterEach
    void tearDown() {
        ControladorPersistencia.reinicialitzarForTest();
    }

    private void emmagatzemarLlibre(ControladorPersistencia cp) throws Exception {
        Llibre l = new Llibre(ISBN, "Dune", "Herbert", 1965, "descripcio original", 9.0, 19.99, true, "");
        l.posarNotes("notes originals");
        cp.afegirLlibre(l);
    }

    @Test
    @DisplayName("La còpia de seguretat inclou la descripció i les notes")
    void copiaInclouCampsPesats() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        File tmp = File.createTempFile("copia_camps_pesats_", ".sql");
        tmp.deleteOnExit();
        new ServeiCopiaSeguretat(cp).copiaSegToSQL(tmp,
            cp.obtenirAllLlibres(), cp.obtenirAllLlistes(), cp.obtenirAllTags());

        String contingut = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertThat(contingut).contains("descripcio original");
        assertThat(contingut).contains("notes originals");
    }

    @Test
    @DisplayName("Restaurar una còpia de seguretat recupera la descripció i les notes")
    void restauracioRecuperaCampsPesats() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        File tmp = File.createTempFile("copia_roundtrip_", ".sql");
        tmp.deleteOnExit();
        new ServeiCopiaSeguretat(cp).copiaSegToSQL(tmp,
            cp.obtenirAllLlibres(), cp.obtenirAllLlistes(), cp.obtenirAllTags());

        ControladorPersistencia.reinicialitzarForTest();
        ControladorPersistencia cp2 = ControladorPersistencia.getInstance();
        cp2.restaurarFromSQLFile(tmp);

        Llibre restaurat = cp2.obtenirAllLlibres().get(0);
        Llibre comprovacio = cp2.obtenirAllLlibres().get(0);
        cp2.carregarHeavyFields(ISBN, comprovacio);
        assertThat(comprovacio.obtenirDescripcio()).isEqualTo("descripcio original");
        assertThat(comprovacio.obtenirNotes()).isEqualTo("notes originals");
        assertThat(restaurat.obtenirNom()).isEqualTo("Dune");
    }

    @Test
    @DisplayName("Restaurar un fitxer que no és una còpia de seguretat no buida la base de dades")
    void restauracioDeFitxerInvalidNoBuidaLaBaseDeDades() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        File garbage = File.createTempFile("copia_invalida_", ".sql");
        garbage.deleteOnExit();
        Files.writeString(garbage.toPath(), "-- això no és una còpia de seguretat\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> cp.restaurarFromSQLFile(garbage))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("no s'ha restaurat res");

        assertThat(cp.obtenirAllLlibres())
            .as("la base de dades ha de quedar intacta després d'un intent de restauració invàlid")
            .hasSize(1);
    }

    @Test
    @DisplayName("Restaurar un fitxer buit no buida la base de dades")
    void restauracioDeFitxerBuitNoBuidaLaBaseDeDades() throws Exception {
        ControladorPersistencia cp = ControladorPersistencia.getInstance();
        emmagatzemarLlibre(cp);

        File buit = File.createTempFile("copia_buida_", ".sql");
        buit.deleteOnExit();
        Files.writeString(buit.toPath(), "", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> cp.restaurarFromSQLFile(buit))
            .isInstanceOf(java.io.IOException.class);

        assertThat(cp.obtenirAllLlibres()).hasSize(1);
    }
}
