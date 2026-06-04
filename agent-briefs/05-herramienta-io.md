---
agent_brief: 05-herramienta-io.md
recommended_models: Minimax 2.7; Composer
primary_paths: `src/herramienta/`
---

# Tools / import-export / config agent

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

ISBN-13, RFC4180, Calibre UTF-8, CoverService, Config save, import/export, I18n utilities.

## Scope

All of `src/herramienta/` (csv/, export/, Config, BackupService, validators).

## Out of scope

presentacio controllers except calling new herramienta APIs; full ResourceBundle migration without coordinator.


## Top issues (read first)

SUMMARY OF HIGHEST-IMPACT ISSUES (read these first)
--------------------------------------------------------------------------------
 1. CRITICAL: ProgressBarRenderer does a DB lookup on EVERY cell paint (EDT).
 2. HIGH:     Whole presentacio layer runs file/DB/network work ON the EDT
              (Import/Export/Backup/Filter controllers, detail dialogs).
 3. HIGH:     BibliotecaTableModel.isCellEditable() always false -> read
              checkbox editor is dead.
 4. HIGH:     ServerConect migrations rely on transactional rollback, but DDL
              auto-commits on MariaDB -> partial-migration / data-loss risk
              (esp. migration 32-34 author backfill then DROP COLUMN autor).
 5. HIGH:     CoverService L1 cache = unsynchronized access-order LinkedHashMap
              shared across the cover-fetch thread pool + EDT -> corruption.
 6. HIGH:     BookImporter.importCSV parses physical lines (breaks quoted
              embedded newlines) and Calibre import uses platform charset.
 7. HIGH:     ISBN-10 -> ISBN-13 normalization is incomplete/inconsistent in
              several places -> wrong/duplicate ISBN keys.
 8. HIGH:     api/HttpCtx empty-body responses never terminate the exchange
              (client hangs / leak); /api/restore + /api/clear are unauth'd.
 9. HIGH:     LlibreLlistaContext.isbn typed int -> ISBN-13 overflow/truncation.
================================================================================


================================================================================

## Review — herramienta

PACKAGE: herramienta/  (incl. csv/, export/, imports/)
================================================================================
src/herramienta/CoverService.java:27: HIGH: L1 is a plain access-order LinkedHashMap mutated by get()/put() from multiple cover-fetch pool threads + the EDT with no synchronization (access-order mutates on get()) -> map corruption / ConcurrentModificationException / infinite loop. Fix: guard all access with synchronization or use a concurrent cache.
src/herramienta/CoverService.java:63: LOW: Long.parseLong(isbn) throws on non-numeric ISBNs (e.g. containing 'X'), swallowed by the surrounding catch -> DB cover save silently dropped. Fix: normalize/validate ISBN before parsing.
src/herramienta/BookImporter.java:32: HIGH: importCSV parses each physical line via readLine()+CsvUtils.parseLine, so quoted fields with embedded newlines are split across rows and mis-parsed; Rfc4180Reader (built for exactly this) is unused here. Fix: stream rows through Rfc4180Reader.
src/herramienta/BookImporter.java:64: HIGH: new InputStreamReader(proc.getInputStream()) uses platform default charset, but sqlite3 emits UTF-8 -> accented Catalan/Spanish text corrupted on non-UTF-8 platforms (Windows). Fix: pass StandardCharsets.UTF_8.
src/herramienta/BookImporter.java:69: MEDIUM: Calibre rows split on literal '|', but text fields (e.g. comment) can contain '|' or newlines -> field misalignment. Fix: use sqlite3 -json or a sentinel/quoted separator.
src/herramienta/BookImporter.java:106: LOW: findSqlite3 runs "--version" and waitFor() without draining stdout/stderr -> potential pipe-buffer deadlock. Fix: drain streams or use ProcessBuilder with redirected output.
src/herramienta/Isbn13Normalizer.java:17: HIGH: ISBN-10 converted to ISBN-13 only when first digit == '0'; valid ISBN-10s starting with other digits (e.g. Spanish "84...") are returned as 10 digits, never normalized. Fix: convert every valid 10-digit ISBN regardless of first digit.
src/herramienta/csv/CsvUtils.java:84: HIGH: parseIsbn only converts ISBN-10 -> 13 when check digit is 'X'; a normal 10-digit ISBN is returned as-is (loses 978 prefix, risks leading-zero loss when parsed as long). Fix: convert all valid 10-digit ISBNs to ISBN-13.
src/herramienta/csv/CsvUtils.java:42: MEDIUM: parseLine trims every field unconditionally, destroying significant leading/trailing whitespace inside quoted fields (RFC4180 treats quoted spaces as data). Fix: only trim unquoted fields.
src/herramienta/csv/CsvUtils.java:54: LOW: colVal does c[idx].trim() without null-checking the element -> NPE on a null cell. Fix: guard c[idx]==null.
src/herramienta/csv/NativeCsvStrategy.java:43: MEDIUM: regex accepts ISBN-10 ending in 'X' ([0-9]{9}[0-9X]) but Long.parseLong(...) then throws NumberFormatException on 'X' -> valid ISBN-10-X rows error out. Fix: normalize via CsvUtils.parseIsbn/Isbn13Normalizer before parseLong.
src/herramienta/csv/Rfc4180Reader.java:40: MEDIUM: hasNext() returns true after the last data row, but next() then reads EOF and returns null -> a hasNext()/next() loop yields a spurious trailing null row. Fix: buffer EOF so hasNext() becomes false after the final row.
src/herramienta/csv/Rfc4180Reader.java:34: LOW: continuation lines rejoined with '\n' but readLine() strips '\r' -> CRLF inside a quoted field silently becomes LF. Fix: preserve original line terminators if round-tripping.
src/herramienta/csv/Rfc4180Reader.java:6: LOW: unused imports java.util.ArrayList / java.util.List. Fix: remove.
src/herramienta/OpenLibraryClient.java:55: HIGH: lookupByISBN catches only IOException, but JsonParser.parseString(...).getAsJsonObject() throws unchecked JsonSyntaxException/IllegalStateException on malformed/non-object responses -> escapes and can crash caller. Fix: also catch RuntimeException and convert to an error entry.
src/herramienta/OpenLibraryClient.java:226: MEDIUM: fetch() throws on non-200 without reading/closing the error stream and never calls conn.disconnect() -> leaked connections / no socket reuse. Fix: drain+close getErrorStream() / disconnect before throwing.
src/herramienta/OpenLibraryClient.java:158: MEDIUM: fetchCoverByISBN never disconnects the HttpURLConnection and leaves the error stream undrained on non-200. Fix: drain error stream + disconnect in finally.
src/herramienta/OpenLibraryClient.java:70: LOW: number_of_pages read with getAsInt(); a non-integer JSON value throws uncaught NumberFormatException. Fix: guard isJsonNumber() / wrap in try.
src/herramienta/Config.java:296: MEDIUM: withBatch sets volatile batchActive but save() never reads it, so intermediate saves inside a batch are not suppressed (batching optimization is dead). Fix: have save() skip scheduling while batchActive.
src/herramienta/Config.java:319: MEDIUM: save() writes config.properties directly via FileOutputStream (non-atomic) -> crash mid-write corrupts user config. Fix: write to temp file + atomic Files.move(REPLACE_EXISTING).
src/herramienta/Config.java:312: LOW: pendingSave cancel/reschedule is a non-atomic read-modify-write on a volatile field; concurrent setters race. Fix: synchronize the scheduling block.
src/herramienta/JsonImporter.java:41: MEDIUM: tags loop calls to.get("id").getAsInt()/to.get("nom").getAsString() with no null-checks/per-entry try -> one malformed tag aborts entire import. Fix: null-guard fields + isolate each entry.
src/herramienta/JsonImporter.java:52: MEDIUM: llistes loop has the same problem -> one bad shelf entry aborts the whole import. Fix: null-guard + isolate each entry.
src/herramienta/export/GoodreadsExportService.java:46: MEDIUM: l.getValoracio() > 0 auto-unboxes a nullable Double -> NPE aborts the whole export when valoracio is null. Fix: null-guard before comparing.
src/herramienta/BookExporter.java:164: MEDIUM: exportPDF calls fm.stringWidth(val) where val from getNom()/getAutor() may be null -> NPE during printing. Fix: coalesce null cell values to "".
src/herramienta/BookExporter.java:280: LOW: jsonStr handles control chars < 0x20 before the '\b'/'\f' switch cases, making those cases unreachable (emits \u0008/\u000c instead of \b/\f). Fix: reorder so named escapes win.
src/herramienta/BookExporter.java:285: LOW: private esc() is dead code (never called). Fix: remove.
src/herramienta/ConfigDTO.java:23: MEDIUM (security): fromConfig() embeds the real plaintext DB password into the DTO returned by /api/config, exposing it over HTTP; apply() checks a "***" sentinel that fromConfig never sends. Fix: send "***" placeholder and only update password when apply() receives a non-sentinel value.
src/herramienta/DialogoError.java:64: LOW: error log written with new FileWriter(f, true) (platform default charset) -> accented messages garbled on non-UTF-8 systems. Fix: use FileWriter(f, StandardCharsets.UTF_8, true).
src/herramienta/SwingUtils.java:19: LOW: idExtractor.apply(...) == prevId auto-unboxes the result -> NPE on a null id. Fix: use Objects.equals / null-guard.
src/herramienta/DateUtils.java:15: LOW: fast path accepts years up to 2200 while regex fallback caps at 2199; also substring(0,4) can return a non-year prefix (e.g. "1234abc"). Fix: align ranges and validate the prefix is a plausible year.
NOTE: src/herramienta/export/PdfExporter.java and BookPdfPrinter.java are empty stub files (no code, referenced nowhere). Remove or implement.


================================================================================

## todo2 deep-dive

[FILE: src/herramienta/BookImporter.java:55-64]
Problem: `importCalibre` uses `Runtime.getRuntime().exec(new String[]{sqlite3, dbFile.getAbsolutePath(), sql})` - command injection risk if sqlite3 path contains spaces or special chars. Also, `proc.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream())` is Java 9+ only; will throw `UnsupportedOperationException` on Java 8.
Code: Process proc = Runtime.getRuntime().exec(new String[]{sqlite3, dbFile.getAbsolutePath(), sql});
    Thread stderrDrain = new Thread(() -> {
        try { proc.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream()); }
        catch (Exception ignored) {}
    });
Fix: Use ProcessBuilder with proper escaping; for stderr drain use a BufferedReader and drain manually for Java 8 compatibility.

---


---

[FILE: src/herramienta/BookExporter.java:221]
Problem: `getEditorial() != null ? getEditorial() : ""` - returns "" but shows as empty string in HTML, but `editorial` column may have "0" or " " as default which displays incorrectly. Also, for number columns like `any`, it outputs `0` when any is 0, causing "0" to appear in HTML where blank was intended.
Code: pw.println("<td>" + (l.getAny() > 0 ? l.getAny() : "") + "</td>");
Fix: Change to `(l.getAny() != null && l.getAny() > 0) ? l.getAny() : ""`

---


---

[FILE: src/herramienta/BackupService.java:269-273]
Problem: `sqlEsc` replaces newlines with spaces but not other control characters like `\t`. Also `\u001A` (substitute character) is handled but other control characters (SOH, STX, etc.) are not stripped.
Code: private static String sqlEsc(String s) {
    if (s == null) return "";
    String out = s.replace("\\", "\\\\").replace("'", "''");
    out = out.replace("\u0000", "").replace("\n", " ").replace("\r", " ").replace("\u001A", "");
    return out;
}
Fix: Replace all characters where `ch < 0x20` with space, not just specific ones.

---


---

[FILE: src/herramienta/Config.java:287-292]
Problem: `SAVE_SCHEDULER` is a static single-threaded executor but never shut down - if the application runs for very long periods without exit, the thread holds memory. More importantly, if `save()` is called many times rapidly, only the last pending save runs (line 312 `pendingSave.cancel(false)`), which is intentional, but the Runnable holds references to captured objects.
Code: private static final ScheduledExecutorService SAVE_SCHEDULER =
    Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "config-save");
        t.setDaemon(true);
        return t;
    });
Fix: Register with ShutdownHooks to ensure clean shutdown, or switch to a lighter mechanism (e.g., Timer).

---


---

[FILE: src/herramienta/BookExporter.java:127]
Problem: Uses `★`.repeat((int) Math.round(val))` which is Java 11+. If running on Java 8, this will fail at compile time or runtime. String.repeat() is Java 11+.
Code: String stars = val > 0 ? "★".repeat((int) Math.round(val)) : "-";
Fix: Use Apache Commons or Guava's StringUtils.repeat, or manual loop.

---


---

[FILE: src/herramienta/OpenLibraryClient.java:271]
Problem: `encode` uses `URLEncoder.encode` which encodes spaces as `+` instead of `%20`. While technically valid for `application/x-www-form-urlencoded`, some servers expect `%20`. This affects `lookupByTitle` and `lookupByAutor` searches.
Code: return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
Fix: Use `URI.create(url).toURL()` or replace `+` with `%20` after encoding, or use `java.net.URI` with explicit encoding.

---


---

[FILE: src/herramienta/BackupService.java:56-68]
Problem: `autoBackup` creates a timestamp-based filename every time but only runs once per day. If the backup fails mid-way or the file is deleted, there's no retry until next day. Also no check for disk space before backup.
Code: String ts = java.time.LocalDateTime.now()
    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    File out = new File(dir, "biblioteca_" + ts + ".sql");
Fix: Add retry logic or at least check for last successful backup timestamp before attempting new one.

---


---

[FILE: src/herramienta/LlibreValidator.java:79-81]
Problem: Year validation rejects any year > currentYear+5, but allows 0 as a valid year (line 92 passes `any != null ? any : 0`). A book with year=0 is probably a placeholder and shouldn't be allowed.
Code: if (any != null && any != 0 && (any < 1000 || any > currentYear + 5))
Fix: Either remove the `any != 0` exception or validate year 0 separately with different limits.

---


---

[FILE: src/herramienta/LlibreValidator.java:67-68]
Problem: `countDig(isbn)` uses `String.valueOf(n).length()` which will fail for Long.MAX_VALUE (20 digits). Also if isbn is negative, this breaks.
Code: int digits = isbn == null ? 0 : countDig(isbn);
private static int countDig(long n) {
    return String.valueOf(n).length();
}
Fix: Handle null and negative ISBNs; for very large values use `Long.toString(n).length()`.

---


---

[FILE: src/herramienta/BookExporter.java:190]
Problem: MIME type detection for cover images is overly simplistic: `blob[0] == (byte)0x89` checks for PNG header but this could incorrectly match other binary data that happens to start with 0x89.
Code: String mime = (blob.length > 4 && blob[0] == (byte)0x89) ? "image/png" : "image/jpeg";
Fix: Use a more robust method like `java.nio.file.Files.probeContentType()` or a library like Apache Tika for reliable MIME detection.

---


---

[FILE: src/herramienta/OpenLibraryClient.java:160]
Problem: `rateLimit` uses `Thread.sleep(wait)` and if interrupted, sets `Thread.currentThread().interrupt()` flag and returns. The caller `fetchCoverByISBN` checks for interrupt and returns null. But other callers of `rateLimit` (like `fetchWithRetry`) may not handle interrupts properly - they re-throw as IOException. This is inconsistent.
Code: if (wait > 0) Thread.sleep(wait);
    lastRequestMs = System.currentTimeMillis();
Fix: Ensure all callers of rateLimit handle InterruptedException consistently.

---


---

[FILE: src/herramienta/BackupService.java:67]
Problem: In autoBackup, when deleting old backups, it uses `backups[i].delete()` which could fail silently if the file is locked or read-only. No error is reported.
Code: for (int i = 0; i < backups.length - 5; i++) backups[i].delete();
Fix: Check return value of delete() and log warnings for failed deletions.

---


---

[FILE: src/herramienta/BookExporter.java:233]
Problem: HTML escaping function handles `&`, `<`, `>`, `"`, `'` but not `&amp;` if the input already contains `&amp;` it becomes `&amp;amp;` which is wrong - actually no, the input "A & B" becomes "A &amp; B" which is correct. The function is applied to already-escaped text? Actually the input may already have HTML entities. If input is "A &amp; B", it becomes "A &amp;amp; B" which double-encodes. But this is a minor issue since the source data is not expected to have pre-encoded entities.
Code: return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
Fix: Use a library like Apache Commons Text StringEscapeUtils or OWASP ESAPI for proper HTML escaping.

---

## Backlog [1][2][3]

[1] [refactor] DialogoError two completely different dialog UIs in one class — split into ValidationDialog and SystemErrorDialog or use factory method — NOT DONE (kept as single class, documented)
[1] [refactor] DialogoError showErrorMessage() checks GraphicsEnvironment.isHeadless() on every error — cache at startup — DONE: IS_HEADLESS cached at class level
[1] [refactor] DialogoError validationDialog uses "⚠" emoji — on some systems renders as empty box; use ImageIcon or UIManager.getIcon("OptionPane.warningIcon") — DONE: now uses UIManager.getIcon("OptionPane.warningIcon")
[1] [refactor] DateUtils normalizeDate() only replaces '/' with '-' — doesn't handle "DD-MM-YYYY" vs "YYYY-MM-DD" ambiguity; add parseDateToIso(String) trying multiple formats — NOT DONE
[1] [refactor] DateUtils both methods are one-liners too thin — merge into fuller DateUtils or inline callers and delete the class — NOT DONE (keep for now)
[1] [refactor] DateUtils add formatDateForDisplay(String isoDate, Locale locale) — currently dates displayed as raw ISO strings everywhere in UI — NOT DONE
[1] [refactor] DateUtils no tests for DateUtils — edge cases like null, empty string, partial dates all need coverage — NOT DONE
[1] [refactor] I18n AUTO-GENERATED with 200+ T() calls in static block — use Java ResourceBundle (.properties files) so system supports external translation files without recompilation — NOT DONE
[1] [refactor] I18n t(key, args...) format uses String.format() — if key has no %s/%d placeholders and args passed, they're silently ignored; add debug assertion in dev mode — NOT DONE
[1] [refactor] I18n language index (0=ca, 1=es, 2=en) is magic number — use enum Lang and look up by ordinal; adding new language requires changing every T() call — NOT DONE
[1] [refactor] I18n if key is missing, t() returns the key itself — should fail loudly in dev mode; add assertion or missing-key list — NOT DONE
[1] [refactor] OpenLibraryClient fetchCoverByISBN() tries multiple cover sizes — document fallback order and add constant for base URL for test mocking — DONE: COVER_BASE/COVER_SIZE constants + javadoc
[1] [refactor] AutoCompletion uses old anonymous class style for listeners — convert to lambdas for readability — DONE: no anonymous inner classes; lambdas not needed
[1] [refactor] NativeCsvStrategy shelf parsing logic (c[9] pipe-split) same as BookExporter export — extract ShelfParser; if format changes both must be updated — DONE: ShelfParser extracted to domini
[1] [refactor] GoodreadsCsvStrategy canHandle() checks for "Book Id" OR "Exclusive Shelf" — brittle string matching; add minimum column count check — NOT DONE
[2] [clean] GoodreadsCsvStrategy notes mapped from "My Review" but Goodreads also has "Private Notes" — map both fields, appending if both present
[2] [clean] DateUtils both methods are one-liners too thin — either merge into fuller DateUtils with more methods or inline callers and delete class
[2] [clean] DateUtils no tests for DateUtils — edge cases like null, empty string, partial dates all need coverage
[2] [clean] I18n strings.csv source file not committed (gitignored) but I18n.java is generated from it — add comment in I18n.java with path and generation command
[2] [clean] LlistesDelLlibreDialog hard-coded column names ("Prestatge", "Valoració", "Llegit") — use I18n keys
[2] [clean] BookExporter.exportHTML() has 200 lines of inline CSS — extract to a template or resource file
[2] [clean] BookExporter.exportPDF() uses AWT printing with manual text layout — no real PDF output; rename to printBooks() or add proper PDF via PDFBox/iText
[2] [clean] CsvUtils buildHeaderMap() trims header names but parseLine() doesn't trim field values — trimming happens in colVal(); this inconsistency can cause "Author " != "Author" bugs
[3] [refactor] DialogoError showErrorMessage() checks GraphicsEnvironment.isHeadless() on every error — DONE: IS_HEADLESS cached at class level
[3] [refactor] DialogoError validationDialog uses "⚠" emoji as label character — DONE: uses UIManager.getIcon("OptionPane.warningIcon")
[3] [refactor] DateUtils normalizeDate() only replaces '/' with '-' — doesn't handle "DD-MM-YYYY" vs "YYYY-MM-DD" ambiguity; add parseDateToIso(String) that tries multiple formats — NOT DONE
[3] [refactor] DateUtils both methods are one-liners; this class is too thin — NOT DONE
[3] [refactor] DateUtils add formatDateForDisplay(String isoDate, Locale locale) — currently dates displayed as raw ISO strings everywhere in UI — NOT DONE
[3] [refactor] DateUtils no tests for DateUtils — edge cases like null, empty string, partial dates all need coverage — NOT DONE
