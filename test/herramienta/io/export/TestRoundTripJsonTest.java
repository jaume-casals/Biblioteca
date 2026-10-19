package herramienta.io.export;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import domini.ControladorDomini;
import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.ExportadorLlibres;
import herramienta.ImportadorLlibres;
import herramienta.io.JsonImporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trip JSON: exportar, buidar la base, importar i comparar el contingut
 * semàntic.
 *
 * <p>El risc que cobreix aquest test és l'exportació silenciosament
 * incompleta: el DAO retorna la vista lleugera (sense descripció ni notes), de
 * manera que sense carregar els camps pesats abans de serialitzar, el fitxer
 * JSON sortia aparentment correcte però amb el contingut perdut.
 */
class TestRoundTripJsonTest {

    private static final long ISBN_A = 9780306406157L;
    private static final long ISBN_B = 9788467034145L;

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:roundtrip_json;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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

    // ── Escenaris ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("L'exportació JSON conté la descripció i les notes: els camps pesats es carreguen abans de serialitzar")
    void exportacioInclouElsCampsPesats() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = llibreComplet(ISBN_A, "Dune", "Descripció amb accents àèïòú · 漢字 · 🧪");
        l.posarNotes("Notes amb salts\nde línia i\ttabulador");
        cd.afegirLlibre(l);

        // Es reinicia la façana de domini perquè torni a llegir la
        // biblioteca des de la base de dades: així els llibres són els de la
        // vista lleugera (sense descripció ni notes), que és la situació real
        // en què s'exporta.
        ControladorDomini.reinicialitzarForTest();
        cd = ControladorDomini.getInstance();

        File tmp = File.createTempFile("roundtrip_export_", ".json");
        tmp.deleteOnExit();
        ExportadorLlibres.exportarJSON(tmp, cd);

        JsonObject root = JsonParser.parseString(
            Files.readString(tmp.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject exportat = root.getAsJsonArray("llibres").get(0).getAsJsonObject();

        assertThat(exportat.get("descripcio").getAsString())
            .as("la descripció no es pot perdre en l'exportació")
            .isEqualTo("Descripció amb accents àèïòú · 漢字 · 🧪");
        assertThat(exportat.get("notes").getAsString())
            .as("les notes no es poden perdre en l'exportació")
            .isEqualTo("Notes amb salts\nde línia i\ttabulador");
    }

    @Test
    @DisplayName("Round-trip: exportar, buidar la base, importar i comparar el contingut semàntic")
    void roundTripCompleta() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();

        Llibre a = llibreComplet(ISBN_A, "Dune", "Descripció llarga: " + "x".repeat(400));
        a.posarNotes("notes\namb\nsalts " + "y".repeat(5000));
        a.posarEditorial("Chilton Books");
        a.posarSerie("Dune");
        a.posarVolum(1);
        a.posarIdioma("ca");
        a.posarFormat("Paperback");
        a.posarPaisOrigen("US");
        a.posarEstat("Nous");
        a.posarExemplars(3);
        a.posarLlenguaOriginal("en");
        a.posarNomCa("Dune (ca)");
        a.posarNomEs("Dune (es)");
        a.posarNomEn("Dune (en)");
        a.posarPagines(412);
        a.posarPaginesLlegides(120);
        a.posarDesitjat(true);
        a.posarImatge("c:/biblioteca/dune.png");
        cd.afegirLlibre(a);

        Llibre b = llibreComplet(ISBN_B, "Neuva", "");
        b.posarNotes("");
        cd.afegirLlibre(b);

        Tag tag = cd.afegirTag("Ciència-ficció");
        cd.afegirLlibreToTag(ISBN_A, tag.obtenirId());
        Llista llista = cd.afegirLlista("Favorits");
        cd.afegirLlibreToLlista(ISBN_A, llista.obtenirId(), 7.5, true);

        // La coberta (blob) viu fora del contracte JSON: l'exportació no la
        // inclou, però tampoc no la pot alterar.
        byte[] blob = new byte[]{0, 1, 2, (byte) 0xFF, 65, 66};
        cd.posarLlibreBlob(ISBN_A, blob);

        File tmp = File.createTempFile("roundtrip_full_", ".json");
        tmp.deleteOnExit();
        ExportadorLlibres.exportarJSON(tmp, cd);

        // Buidem la base: l'importació ha de reconstruir-la.
        for (Llibre l : new ArrayList<>(cd.obtenirAllLlibres())) cd.eliminarLlibre(l.obtenirISBN());
        cd.eliminarTag(tag);
        cd.eliminarLlista(llista);
        assertThat(cd.obtenirAllLlibres()).isEmpty();

        ImportadorLlibres.ResultatImportacio res = JsonImporter.run(tmp, cd);
        assertThat(res.imported()).as("tots els llibres s'han d'importar").isEqualTo(2);
        assertThat(res.errors()).as("cap error d'importació").isZero();

        Llibre a2 = cd.obtenirLlibre(ISBN_A);
        assertThat(a2.obtenirNom()).isEqualTo("Dune");
        assertThat(a2.obtenirAutor()).isEqualTo("Herbert");
        assertThat(a2.obtenirAny()).isEqualTo(1965);
        assertThat(a2.obtenirDescripcio()).isEqualTo("Descripció llarga: " + "x".repeat(400));
        assertThat(a2.obtenirNotes()).isEqualTo("notes\namb\nsalts " + "y".repeat(5000));
        assertThat(a2.obtenirEditorial()).isEqualTo("Chilton Books");
        assertThat(a2.obtenirSerie()).isEqualTo("Dune");
        assertThat(a2.obtenirVolum()).isEqualTo(1);
        assertThat(a2.obtenirIdioma()).isEqualTo("ca");
        assertThat(a2.obtenirFormat()).isEqualTo("Paperback");
        assertThat(a2.obtenirPaisOrigen()).isEqualTo("US");
        assertThat(a2.obtenirEstat()).isEqualTo("Nous");
        assertThat(a2.obtenirExemplars()).isEqualTo(3);
        assertThat(a2.obtenirLlenguaOriginal()).isEqualTo("en");
        assertThat(a2.obtenirNomCa()).isEqualTo("Dune (ca)");
        assertThat(a2.obtenirNomEs()).isEqualTo("Dune (es)");
        assertThat(a2.obtenirNomEn()).isEqualTo("Dune (en)");
        assertThat(a2.obtenirPagines()).isEqualTo(412);
        assertThat(a2.obtenirPaginesLlegides()).isEqualTo(120);
        assertThat(a2.esDesitjat()).isTrue();
        assertThat(a2.obtenirImatge()).isEqualTo("c:/biblioteca/dune.png");
        assertThat(a2.obtenirValoracio()).isEqualTo(9.0);
        assertThat(a2.obtenirPreu()).isEqualTo(19.99);
        assertThat(a2.obtenirLlegit()).isTrue();

        // Camps buits: el llibre B no tenia descripció ni notes.
        Llibre b2 = cd.obtenirLlibre(ISBN_B);
        assertThat(b2.obtenirDescripcio()).isEmpty();
        assertThat(b2.obtenirNotes()).isEmpty();

        // Relacions: es comparen per identitat lògica (el nom), no per l'id,
        // perquè la importació remapeja els ids.
        assertThat(cd.obtenirTagsForLlibre(ISBN_A))
            .extracting(Tag::obtenirNom).containsExactly("Ciència-ficció");
        assertThat(cd.obtenirLlistesForLlibre(ISBN_A))
            .extracting(Llista::obtenirNom).containsExactly("Favorits");

        // El blob no forma part del contracte JSON: la importació no el crea,
        // però l'exportació tampoc no ha d'haver alterat l'original.
        assertThat(cd.obtenirLlibreBlob(ISBN_A))
            .as("el BLOB no viatja al JSON; la importació no el crea")
            .isNull();
    }

    @Test
    @DisplayName("El BLOB de coberta de la font no s'altera en exportar")
    void elBlobDeLaFontNoSAlteraEnExportar() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = llibreComplet(ISBN_A, "Dune", "desc");
        cd.afegirLlibre(l);
        byte[] blob = new byte[]{9, 8, 7, 6, 5};
        cd.posarLlibreBlob(ISBN_A, blob);

        File tmp = File.createTempFile("roundtrip_blob_", ".json");
        tmp.deleteOnExit();
        ExportadorLlibres.exportarJSON(tmp, cd);

        assertThat(cd.obtenirLlibreBlob(ISBN_A))
            .as("exportar no ha d'alterar el blob de coberta de la font")
            .containsExactly(9, 8, 7, 6, 5);
    }

    @Test
    @DisplayName("Si la càrrega dels camps pesats falla, no es crea cap exportació")
    void errorDeCarregaNoProdueixUnaExportacioIncompleta() throws Exception {
        ControladorDomini real = ControladorDomini.getInstance();
        Llibre l = llibreComplet(ISBN_A, "Dune", "descripcio");
        // Llibre de la vista lleugera: els camps pesats no estan carregats i
        // l'exportació els ha de carregar abans de serialitzar.
        l.posarCampsPesatsCarregats(false);
        real.afegirLlibre(l);

        persistencia.contract.LectorBiblioteca queFalla = (persistencia.contract.LectorBiblioteca) Proxy.newProxyInstance(
            persistencia.contract.LectorBiblioteca.class.getClassLoader(),
            new Class<?>[]{persistencia.contract.LectorBiblioteca.class},
            new InvocationHandler() {
                @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (method.getName().startsWith("carregarHeavyFields")) {
                        throw new IllegalStateException("fallada simulada de càrrega");
                    }
                    return method.invoke(real, args);
                }
            });

        File tmp = File.createTempFile("roundtrip_error_", ".json");
        tmp.delete();
        assertThatThrownBy(() -> ExportadorLlibres.exportarJSON(tmp, queFalla))
            .as("la càrrega fallida ha de fer fallar l'exportació")
            .isInstanceOf(Exception.class);
        assertThat(tmp.exists())
            .as("no es pot deixar cap fitxer que sembli una exportació correcta")
            .isFalse();
    }

    @Test
    @DisplayName("Un JSON antic sense camps opcionals s'importa sense problemes")
    void jsonAnticSenseCampsOpcionals() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        String jsonAntic = "{\"version\":1,\"llibres\":[{\"isbn\":9780306406157,\"nom\":\"Dune\","
            + "\"autor\":\"Herbert\",\"any\":1965,\"descripcio\":\"descripcio antiga\","
            + "\"valoracio\":9.0,\"preu\":19.99,\"llegit\":true,\"desitjat\":false,"
            + "\"imatge\":\"dune.png\",\"notes\":\"notes antigues\",\"llistes\":[],\"tags\":[]}],"
            + "\"llistes\":[],\"tags\":[]}";

        File tmp = File.createTempFile("roundtrip_old_", ".json");
        tmp.deleteOnExit();
        Files.writeString(tmp.toPath(), jsonAntic, StandardCharsets.UTF_8);

        ImportadorLlibres.ResultatImportacio res = JsonImporter.run(tmp, cd);
        assertThat(res.imported()).isEqualTo(1);
        assertThat(res.errors()).isZero();

        Llibre l = cd.obtenirLlibre(ISBN_A);
        assertThat(l.obtenirNom()).isEqualTo("Dune");
        assertThat(l.obtenirDescripcio()).isEqualTo("descripcio antiga");
        assertThat(l.obtenirNotes()).isEqualTo("notes antigues");
        // Els camps que el JSON antic no porta queden amb el valor per defecte.
        assertThat(l.obtenirEditorial()).isEmpty();
        assertThat(l.obtenirVolum()).isZero();
        assertThat(l.esDesitjat()).isFalse();
    }

    @Test
    @DisplayName("Round-trip: la descripció nul·la i la buida es distingeixen del 'no carregat'")
    void distincioEntreNoCarregatICarregatBuit() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre senseDesc = llibreComplet(ISBN_A, "Dune", "");
        cd.afegirLlibre(senseDesc);
        Llibre ambDesc = llibreComplet(ISBN_B, "Neuva", "descripció real");
        cd.afegirLlibre(ambDesc);

        File tmp = File.createTempFile("roundtrip_distincio_", ".json");
        tmp.deleteOnExit();
        ExportadorLlibres.exportarJSON(tmp, cd);

        JsonObject root = JsonParser.parseString(
            Files.readString(tmp.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
        List<com.google.gson.JsonObject> llibres = new ArrayList<>();
        root.getAsJsonArray("llibres").forEach(e -> llibres.add(e.getAsJsonObject()));

        com.google.gson.JsonObject a = llibres.stream()
            .filter(o -> o.get("isbn").getAsLong() == ISBN_A).findFirst().orElseThrow();
        com.google.gson.JsonObject b = llibres.stream()
            .filter(o -> o.get("isbn").getAsLong() == ISBN_B).findFirst().orElseThrow();

        assertThat(a.get("descripcio").getAsString())
            .as("un llibre sense descripció s'exporta com a cadena buida, no com a 'no carregat'")
            .isEmpty();
        assertThat(b.get("descripcio").getAsString()).isEqualTo("descripció real");
    }

    // ── Utilitats ────────────────────────────────────────────────────────────

    private static Llibre llibreComplet(long isbn, String nom, String descripcio) {
        return new Llibre(isbn, nom, "Herbert", 1965, descripcio, 9.0, 19.99, true, "");
    }
}
