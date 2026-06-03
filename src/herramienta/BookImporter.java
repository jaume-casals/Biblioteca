package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.csv.CsvUtils;
import interficie.BibliotecaWriter;

public class BookImporter {

    public record ImportResult(int imported, int skipped, int errors, String errorDetails) {}

    public static ImportResult importCSV(java.io.File file, BibliotecaWriter cd) {
        int ok = 0, skipped = 0, err = 0;
        StringBuilder errors = new StringBuilder();
        java.util.List<herramienta.csv.CsvImportStrategy> strategies = java.util.List.of(
            new herramienta.csv.LibraryThingCsvStrategy(),
            new herramienta.csv.GoodreadsCsvStrategy(),
            new herramienta.csv.NativeCsvStrategy());
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return new ImportResult(0, 0, 0, "");
            if (headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
            final String header = headerLine;
            herramienta.csv.CsvImportStrategy strategy = strategies.stream()
                .filter(s -> s.canHandle(header)).findFirst()
                .orElse(new herramienta.csv.NativeCsvStrategy());
            String[] headerCols = herramienta.csv.CsvUtils.parseLine(header);
            java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerCols);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    if (strategy.parseLine(herramienta.csv.CsvUtils.parseLine(line), hMap, cd)) ok++;
                    else skipped++;
                } catch (Exception ex) {
                    err++;
                    if (errors.length() < 400) errors.append("\n• ").append(ex.getMessage());
                }
            }
        } catch (Exception e) {
            return new ImportResult(ok, skipped, err + 1, e.getMessage());
        }
        return new ImportResult(ok, skipped, err, errors.toString());
    }

    public static ImportResult importJSON(java.io.File file, BibliotecaWriter cd) throws Exception {
        return JsonImporter.run(file, cd);
    }

    public static ImportResult importCalibre(java.io.File dbFile, String sqlite3, BibliotecaWriter cd) throws Exception {
        String sql = "SELECT b.id, b.title, GROUP_CONCAT(a.name, ', '), i.val, p.name, b.pubdate, b.rating, b.comment, b.series_index, s.name FROM books b LEFT JOIN books_authors_link ba ON b.id=ba.book LEFT JOIN authors a ON ba.author=a.id LEFT JOIN identifiers i ON b.id=i.book AND i.type='isbn' LEFT JOIN publishers p ON b.id=(SELECT book FROM books_publishers_link WHERE book=b.id LIMIT 1) LEFT JOIN books_series_link bs ON b.id=bs.book LEFT JOIN series s ON bs.series=s.id GROUP BY b.id;";
        Process proc = Runtime.getRuntime().exec(new String[]{sqlite3, dbFile.getAbsolutePath(), sql});
        Thread stderrDrain = new Thread(() -> {
            try { proc.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream()); }
            catch (Exception ignored) {}
        });
        stderrDrain.setDaemon(true);
        stderrDrain.start();
        int ok = 0, skipped = 0, err = 0;
        StringBuilder errors = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    String[] c = line.split("\\|", -1);
                    String isbnRaw = c.length > 3 ? c[3].replaceAll("[^0-9]", "") : "";
                    if (isbnRaw.isEmpty() || isbnRaw.length() < 10) { skipped++; continue; }
                    long isbn = Long.parseLong(isbnRaw);
                    try { cd.getLlibre(isbn); skipped++; continue; } catch (Exception ignored) {}
                    String nom   = c.length > 1 ? c[1].trim() : "?";
                    String autor = c.length > 2 ? c[2].trim() : "";
                    String editorial = c.length > 4 ? c[4].trim() : "";
                    int any = 0;
                    if (c.length > 5 && !c[5].isBlank()) any = DateUtils.parseYear(c[5]);
                    double valoracio = c.length > 6 ? CsvUtils.parseDoubleOrZero(c[6]) * 2.0 : 0.0;
                    String notes = c.length > 7 ? c[7].trim() : "";
                    String serie = c.length > 9 ? c[9].trim() : "";
                    int volum = 0;
                    if (c.length > 8) { try { volum = (int) Double.parseDouble(c[8].trim()); } catch (Exception ignored) {} }
                    Llibre l = LlibreValidator.checkLlibre(isbn, nom, autor, any, "", valoracio, 0.0, false, "");
                    l.setEditorial(editorial);
                    if (!notes.isEmpty()) l.setNotes(notes);
                    if (!serie.isEmpty()) { l.setSerie(serie); l.setVolum(volum); }
                    cd.addLlibre(l);
                    ok++;
                } catch (Exception ex) {
                    err++;
                    if (errors.length() < 400) errors.append("\n• ").append(ex.getMessage());
                }
            }
        }
        if (!proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new Exception("sqlite3 process timed out after 30 seconds");
        }
        return new ImportResult(ok, skipped, err, errors.toString());
    }

    public static String findSqlite3() {
        for (String candidate : new String[]{"sqlite3", "/usr/bin/sqlite3", "/usr/local/bin/sqlite3", "/opt/homebrew/bin/sqlite3"}) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{candidate, "--version"});
                if (p.waitFor() == 0) return candidate;
            } catch (Exception ignored) {}
        }
        return null;
    }
}