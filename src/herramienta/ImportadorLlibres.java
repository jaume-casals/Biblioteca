package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.io.JsonImporter;
import herramienta.io.csv.UtilitatsCsv;
import herramienta.text.UtilitatsData;
import herramienta.text.ValidadorLlibre;
import persistencia.contract.EscritorBiblioteca;

public class ImportadorLlibres {

    /**
     * Resultat d'una importació CSV / Calibre / JSON. {@code errorDetails}
     * és una llista il·limitada de missatges d'una línia (sense la
     * truncació silenciosa de 400 caràcters) perquè la UI els pugui
     * renderitzar en una àrea de text amb scroll. La llista pot estar
     * buida fins i tot quan {@code errors > 0} (p. ex. una excepció per
     * fila buida sense missatge) — el recompte és la font de veritat.
     */
    public record ResultatImportacio(int imported, int skipped, int errors, java.util.List<String> errorDetails) {}

    /** La columna `comment` de Calibre sol contenir notes d'usuari de
     *  diversos KB; persistir-les directament a {@code notes VARCHAR(2048)}
     *  (migració 24: TEXT) és correcte per a H2, però l'informe original
     *  va assenyalar un desquadrament de tipus de columna a MariaDB quan
     *  aquest límit es va moure. 10 KB manté el llibre típic amb notes
     *  per sota del sostre amb un petit marge de seguretat; les notes
     *  més llargues es trunquen amb un marcador perquè l'usuari pugui
     *  veure el tall. */
    private static final int MAX_NOTES_CHARS = 10_000;

    public static ResultatImportacio importarCSV(java.io.File file, EscritorBiblioteca cd) {
        int ok = 0, skipped = 0, err = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<herramienta.io.csv.CsvImportStrategy> strategies = java.util.List.of(
            new herramienta.io.csv.LibraryThingCsvStrategy(),
            new herramienta.io.csv.GoodreadsCsvStrategy(),
            new herramienta.io.csv.NativeCsvStrategy());
        try (java.io.Reader reader = new java.io.BufferedReader(
                new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            herramienta.io.csv.Rfc4180Reader br = new herramienta.io.csv.Rfc4180Reader(reader);
            String[] headerRow = br.next();
            if (headerRow == null) return new ResultatImportacio(0, 0, 0, java.util.Collections.emptyList());
            // Elimina un BOM UTF-8 de la PRIMERA cel·la de la capçalera
            // perquè la coincidència d'estratègia (que concatena la fila)
            // no vegi un "﻿" inicial.
            if (headerRow.length > 0 && headerRow[0].startsWith("\uFEFF")) {
                headerRow[0] = headerRow[0].substring(1);
            }
            String headerLine = String.join(",", headerRow);
            herramienta.io.csv.CsvImportStrategy strategy = null;
            for (herramienta.io.csv.CsvImportStrategy s : strategies) {
                if (s.potHandle(headerLine)) { strategy = s; break; }
            }
            if (strategy == null) strategy = new herramienta.io.csv.NativeCsvStrategy();
            // Reutilitza el headerRow ja analitzat — no cal tornar a
            // concatenar i dividir les mateixes dades a través de
            // CsvUtils.parseLine (malbaratament d'una passada de 5-10 ms
            // per a CSV grans).
            java.util.Map<String, Integer> hMap = herramienta.io.csv.UtilitatsCsv.buildHeaderMap(headerRow);
            while (br.hasNext()) {
                String[] row = br.next();
                if (row == null || row.length == 0 || (row.length == 1 && row[0].isBlank())) continue;
                try {
                    if (strategy.analitzarLine(row, hMap, cd)) ok++;
                    else skipped++;
                } catch (Exception ex) {
                    err++;
                    errors.add(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                }
            }
        } catch (Exception e) {
            err++;
            errors.add(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        return new ResultatImportacio(ok, skipped, err, errors);
    }

    public static ResultatImportacio importarJSON(java.io.File file, EscritorBiblioteca cd) throws Exception {
        return JsonImporter.run(file, cd);
    }

    public static ResultatImportacio importarCalibre(java.io.File dbFile, String sqlite3, EscritorBiblioteca cd) throws Exception {
        String sql = "SELECT b.id, b.title, GROUP_CONCAT(a.name, ', '), i.val, p.name, b.pubdate, b.rating, b.comment, b.series_index, s.name FROM books b LEFT JOIN books_authors_link ba ON b.id=ba.book LEFT JOIN authors a ON ba.author=a.id LEFT JOIN identifiers i ON b.id=i.book AND i.type='isbn' LEFT JOIN publishers p ON b.id=(SELECT book FROM books_publishers_link WHERE book=b.id LIMIT 1) LEFT JOIN books_series_link bs ON b.id=bs.book LEFT JOIN series s ON bs.series=s.id GROUP BY b.id;";
        Process proc = Runtime.getRuntime().exec(new String[]{sqlite3, "-separator", "\t", dbFile.getAbsolutePath(), sql});
        // Inicia el drenatge de stderr ABANS de llegir stdout perquè
        // el buffer (64 KB a Linux) no es pugui omplir i bloquejar el
        // procés fill mentre el fil encara no s'ha programat.
        Thread stderrDrain = new Thread(() -> {
            try { proc.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream()); }
            catch (Exception ignored) {}
        });
        stderrDrain.setDaemon(true);
        stderrDrain.start();
        int ok = 0, skipped = 0, err = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    String[] c = line.split("\t", -1);
                    if (c.length < 10) {
                        throw new IllegalArgumentException("La fila de Calibre té " + c.length
                            + " camp(s) separats per tabulació (n'ha de tenir 10). El títol del llibre o un "
                            + "camp GROUP_CONCAT probablement conté un tabulador incrustat — "
                            + "la fila no es pot analitzar de manera segura.");
                    }
                    String isbnRaw = c.length > 3 ? c[3].replaceAll("[^0-9]", "") : "";
                    if (isbnRaw.isEmpty() || isbnRaw.length() < 10) { skipped++; continue; }
                    long isbn = Long.parseLong(isbnRaw);
                    try { cd.obtenirLlibre(isbn); skipped++; continue; } catch (Exception ignored) {}
                    String nom   = c.length > 1 ? c[1].trim() : "?";
                    String autor = c.length > 2 ? c[2].trim() : "";
                    String editorial = c.length > 4 ? c[4].trim() : "";
                    int any = 0;
                    if (c.length > 5 && !c[5].isBlank()) any = UtilitatsData.analitzarYear(c[5]).orElse(0);
                    double valoracio = c.length > 6 ? UtilitatsCsv.analitzarDoubleOrZero(c[6]) * 2.0 : 0.0;
                    String notes = c.length > 7 ? c[7].trim() : "";
                    if (notes.length() > MAX_NOTES_CHARS) {
                        notes = notes.substring(0, MAX_NOTES_CHARS) + "… [truncat]";
                    }
                    String serie = c.length > 9 ? c[9].trim() : "";
                    int volum = 0;
                    if (c.length > 8) { try { volum = (int) Double.parseDouble(c[8].trim()); } catch (Exception ignored) {} }
                    Llibre l = ValidadorLlibre.comprovarLlibre(isbn, nom, autor, any, "", valoracio, 0.0, false, "");
                    l.posarEditorial(editorial);
                    if (!notes.isEmpty()) l.posarNotes(notes);
                    if (!serie.isEmpty()) { l.posarSerie(serie); l.posarVolum(volum); }
                    cd.afegirLlibre(l);
                    ok++;
                } catch (Exception ex) {
                    err++;
                    errors.add(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                }
            }
        }
        if (!proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            // destroyForcibly és SIGKILL a Unix i TerminateProcess a
            // Windows; totes dues desbloquegen waitFor però deixen un
            // bloqueig transitori sobre el metadata.db de Calibre a
            // Windows. El bloqueig s'allibera tan aviat com el driver
            // SQLite del procés mort descarrega el seu mmap, però una
            // importació posterior dins d'~1 s pot fallar amb
            // "database is locked" — els consumidors poden reintentar.
            // Linux/macOS no es veuen afectats (el fitxer s'obre en
            // només lectura i es desbloqueja immediatament en sortir
            // del procés).
            proc.destroyForcibly();
            throw new Exception("El procés sqlite3 ha superat el temps d'espera de 30 segons");
        }
        // Comprova el codi de sortida del procés — la versió anterior
        // ignorava el valor i informava èxit encara que sqlite3 hagués
        // fallat (per exemple, metadades corruptes), fent que el
        // consumidor processés una sortida parcial sense saber-ho.
        if (proc.exitValue() != 0)
            throw new Exception("sqlite3 ha sortit amb codi " + proc.exitValue());
        return new ResultatImportacio(ok, skipped, err, errors);
    }

    public static String cercarSqlite3() {
        for (String candidate : new String[]{"sqlite3", "/usr/bin/sqlite3", "/usr/local/bin/sqlite3", "/opt/homebrew/bin/sqlite3"}) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{candidate, "--version"});
                if (p.waitFor() == 0) return candidate;
            } catch (Exception ignored) {}
        }
        return null;
    }
}