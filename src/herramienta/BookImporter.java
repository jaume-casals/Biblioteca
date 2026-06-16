package herramienta;

import domini.Llibre;
import domini.Llista;
import domini.Tag;
import herramienta.csv.CsvUtils;
import interficie.BibliotecaWriter;

public class BookImporter {

    /**
     * Result of a CSV / Calibre / JSON import. {@code errorDetails} is
     * an unbounded list of one-liners (no more silent 400-char truncation)
     * so the UI can render them in a scrollable text area. The list may
     * be empty even when {@code errors > 0} (e.g. an empty per-row
     * exception with no message) — the count is the source of truth.
     */
    public record ImportResult(int imported, int skipped, int errors, java.util.List<String> errorDetails) {}

    /** Calibre's `comment` column routinely holds multi-KB user notes;
     *  persisting them straight to {@code notes VARCHAR(2048)} (migration
     *  24: TEXT) is fine for H2, but the original report flagged a
     *  MariaDB column-type mismatch once that limit moved. 10 KB keeps
     *  the typical reading-note book under the cap with a small safety
     *  margin; longer notes are truncated with a marker so the user can
     *  spot the cut. */
    private static final int MAX_NOTES_CHARS = 10_000;

    public static ImportResult importCSV(java.io.File file, BibliotecaWriter cd) {
        int ok = 0, skipped = 0, err = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<herramienta.csv.CsvImportStrategy> strategies = java.util.List.of(
            new herramienta.csv.LibraryThingCsvStrategy(),
            new herramienta.csv.GoodreadsCsvStrategy(),
            new herramienta.csv.NativeCsvStrategy());
        try (java.io.Reader reader = new java.io.BufferedReader(
                new java.io.FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            herramienta.csv.Rfc4180Reader br = new herramienta.csv.Rfc4180Reader(reader);
            if (!br.hasNext()) return new ImportResult(0, 0, 0, java.util.Collections.emptyList());
            String[] headerRow = br.next();
            // Strip a UTF-8 BOM from the FIRST header cell so strategy
            // matching (which joins the row) doesn't see a leading "﻿".
            if (headerRow.length > 0 && headerRow[0].startsWith("\uFEFF")) {
                headerRow[0] = headerRow[0].substring(1);
            }
            String headerLine = String.join(",", headerRow);
            herramienta.csv.CsvImportStrategy strategy = null;
            for (herramienta.csv.CsvImportStrategy s : strategies) {
                if (s.canHandle(headerLine)) { strategy = s; break; }
            }
            if (strategy == null) strategy = new herramienta.csv.NativeCsvStrategy();
            // Reuse the already-parsed headerRow — no need to re-join and
            // re-split the same data through CsvUtils.parseLine (waste of
            // a 5–10 ms pass for large CSVs).
            java.util.Map<String, Integer> hMap = herramienta.csv.CsvUtils.buildHeaderMap(headerRow);
            while (br.hasNext()) {
                String[] row = br.next();
                if (row == null || row.length == 0 || (row.length == 1 && row[0].isBlank())) continue;
                try {
                    if (strategy.parseLine(row, hMap, cd)) ok++;
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
        return new ImportResult(ok, skipped, err, errors);
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
        java.util.List<String> errors = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(proc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
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
                    if (c.length > 5 && !c[5].isBlank()) any = DateUtils.parseYear(c[5]).orElse(0);
                    double valoracio = c.length > 6 ? CsvUtils.parseDoubleOrZero(c[6]) * 2.0 : 0.0;
                    String notes = c.length > 7 ? c[7].trim() : "";
                    if (notes.length() > MAX_NOTES_CHARS) {
                        notes = notes.substring(0, MAX_NOTES_CHARS) + "… [truncated]";
                    }
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
                    errors.add(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                }
            }
        }
        if (!proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            // destroyForcibly is SIGKILL on Unix and TerminateProcess on
            // Windows; both unblock waitFor but leave a transient lock on
            // the Calibre metadata.db on Windows. The lock is released as
            // soon as the SQLite driver in the dead process unloads its
            // mmap, but a subsequent import within ~1 s may fail with
            // "database is locked" — callers can retry. Linux/macOS are
            // unaffected (the file is opened read-only and unlocked
            // immediately on process exit).
            proc.destroyForcibly();
            throw new Exception("sqlite3 process timed out after 30 seconds");
        }
        return new ImportResult(ok, skipped, err, errors);
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