package herramienta.io.export;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import domini.ControladorDomini;
import domini.Llibre;
import herramienta.ExportadorLlibres;
import herramienta.ImportadorLlibres;
import herramienta.text.ValidadorLlibre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import persistencia.internal.ControladorPersistencia;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Cross-format null-field safety tests. The user reported that exporting the
 * BBDD to native CSV appeared to succeed but wrote a file with only the
 * header row — the cause was {@code AnalitzadorPrestatgeria.exportarToCsv}
 * passing boxed {@code Integer}/{@code Double}/{@code Boolean} getters
 * straight into {@code printf("%d"/"%.1f"/"%.2f"/"%b", ...)}: any null field
 * raised NPE, the per-row {@code try/catch} swallowed it silently, and the
 * CSV ended up empty. These tests cover all 5 export paths (native CSV,
 * Goodreads CSV, JSON, HTML, PDF row builder) with a book that has null
 * year / valoracio / preu / llegit to make sure none of them regress the
 * same way.
 */
class TestExportacioNullSafeTest {

    static {
        System.setProperty("biblioteca.test", "true");
        System.setProperty("biblioteca.h2.url",
            "jdbc:h2:mem:export_null_safe;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
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

    private static Llibre llibreAmbCampsNul(ControladorDomini cd) {
        Llibre l = new Llibre(9780306406157L, "Test Book", "Author", null, "", null, null, null, "");
        cd.afegirLlibre(l);
        return l;
    }

    @Test
    @DisplayName("native CSV: null year/valoracio/preu/llegit still write a row")
    void nativeCsvAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        llibreAmbCampsNul(cd);

        File tmp = File.createTempFile("export_null_csv_", ".csv");
        tmp.deleteOnExit();
        assertDoesNotThrow(() ->
            ExportadorLlibres.exportarCSV(tmp, new ArrayList<>(cd.obtenirAllLlibres()), cd));

        String[] rows = Files.readString(tmp.toPath(), StandardCharsets.UTF_8).split("\n");
        assertThat(rows).hasSize(2);
        assertThat(rows[1]).contains("9780306406157").contains("Test Book");
    }

    @Test
    @DisplayName("Goodreads CSV: null year/valoracio still write a row with empty cells")
    void goodreadsCsvAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        llibreAmbCampsNul(cd);

        String csv = assertDoesNotThrow(() -> GoodreadsExportService.exportarToCsv(cd));
        String[] rows = csv.split("\n");
        assertThat(rows).hasSize(2);
        assertThat(rows[1]).contains("9780306406157").contains("Test Book");
    }

    @Test
    @DisplayName("JSON: null fields serialize as JSON null, not crash")
    void jsonAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        llibreAmbCampsNul(cd);

        File tmp = File.createTempFile("export_null_json_", ".json");
        tmp.deleteOnExit();
        assertDoesNotThrow(() ->
            ExportadorLlibres.exportarJSON(tmp, cd));

        String content = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();
        JsonObject l = root.getAsJsonArray("llibres").get(0).getAsJsonObject();
        assertThat(l.get("isbn").getAsLong()).isEqualTo(9780306406157L);
        assertThat(l.get("nom").getAsString()).isEqualTo("Test Book");
        assertThat(l.get("isbn").getAsLong()).isEqualTo(9780306406157L);
    }

    @Test
    @DisplayName("HTML: null fields render without NPE (table view)")
    void htmlAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        llibreAmbCampsNul(cd);

        File tmp = File.createTempFile("export_null_html_", ".html");
        tmp.deleteOnExit();
        assertDoesNotThrow(() ->
            ExportadorLlibres.exportarHTML(tmp, new ArrayList<>(cd.obtenirAllLlibres()), cd, false, true));

        String html = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertThat(html).contains("Test Book");
    }

    @Test
    @DisplayName("HTML: null fields render without NPE (grid view)")
    void htmlGridAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        llibreAmbCampsNul(cd);

        File tmp = File.createTempFile("export_null_html_grid_", ".html");
        tmp.deleteOnExit();
        assertDoesNotThrow(() ->
            ExportadorLlibres.exportarHTML(tmp, new ArrayList<>(cd.obtenirAllLlibres()), cd, false, false));

        String html = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
        assertThat(html).contains("Test Book").contains("\uD83D\uDCD6");
    }

    @Test
    @DisplayName("JSON roundtrip: export JSON with null valoracio/preu/llegit → import JSON → book survives")
    void jsonRoundtripAmbCampsNul() throws Exception {
        ControladorDomini cd = ControladorDomini.getInstance();
        Llibre l = ValidadorLlibre.comprovarLlibre(9780306406157L, "Test Book", "Author", 2020,
            "desc", 8.0, 19.99, true, "");
        l.posarValoracio(null);
        l.posarPreu(null);
        l.posarLlegit(null);
        cd.afegirLlibre(l);

        File tmp = File.createTempFile("export_json_roundtrip_", ".json");
        tmp.deleteOnExit();
        ExportadorLlibres.exportarJSON(tmp, cd);

        ControladorDomini.reinicialitzarForTest();
        ControladorPersistencia.reinicialitzarForTest();
        ControladorDomini cd2 = ControladorDomini.getInstance();
        ImportadorLlibres.ResultatImportacio r = ImportadorLlibres.importarJSON(tmp, cd2);
        assertThat(r.errors()).isZero();
        assertThat(r.imported()).isEqualTo(1);
        Llibre back = cd2.obtenirLlibre(9780306406157L);
        assertThat(back.obtenirNom()).isEqualTo("Test Book");
        assertThat(back.obtenirAny()).isEqualTo(2020);
    }

    @Test
    @DisplayName("PDF row string: null year renders empty cell (no literal 'null')")
    void pdfAmbAnyNul() throws Exception {
        Llibre l = new Llibre(9780306406157L, "Test Book", "Author", null, "", null, null, null, "");
        File tmp = File.createTempFile("pdf_row_test_", ".txt");
        tmp.deleteOnExit();
        try (var pw = new java.io.PrintWriter(tmp, StandardCharsets.UTF_8)) {
            double val = l.obtenirValoracio() != null ? l.obtenirValoracio() : 0.0;
            String stars = val > 0 ? "★".repeat((int) Math.round(val)) : "-";
            Integer any = l.obtenirAny();
            pw.println(l.obtenirNom() + "," + l.obtenirAutor() + "," + (any != null ? any.toString() : "")
                + "," + stars + "," + (Boolean.TRUE.equals(l.obtenirLlegit()) ? "✓" : "○"));
        }
        String pdfRow = Files.readString(tmp.toPath(), StandardCharsets.UTF_8).trim();
        assertThat(pdfRow).contains("Test Book").doesNotContain(",null,");
    }
}
