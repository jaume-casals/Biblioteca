---
agent_brief: 04-api-http.md
recommended_models: Minimax 2.7; Composer
primary_paths: `src/api/`, `src/main/`
---

# HTTP API + main agent

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

HttpCtx lifecycle, auth/CSRF, status codes, CORS, static traversal, router validation.

## Scope

All of `src/api/` and `src/main/Ejecutable.java`.

## Out of scope

Swing UI; DAO implementation (coordinate with 02); CSV/ISBN in herramienta (05).


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

## Review — api + main

PACKAGE: api/  +  main/
================================================================================
src/api/HttpCtx.java:113: HIGH: empty-body responses with status != 204 call sendResponseHeaders(status, 0) (chunked) but never open/close an output stream -> response never terminated, exchange leaks, client hangs; the 204 path also never calls ex.close(). Fix: pass -1 for all empty bodies and always close the exchange/stream.
src/api/HttpCtx.java:74: MEDIUM: bodyBytes() calls readAllBytes() with no size limit (used by image upload + SQL restore) -> unbounded-memory DoS. Fix: enforce a max request size / Content-Length cap.
src/api/HttpCtx.java:75: LOW: an IOException while reading the body is swallowed and treated as an empty body, masking truncated reads as valid empty input. Fix: surface the error (400/500).
src/api/HttpCtx.java:58: LOW: queryParamInt/queryParamDbl silently return null on parse failure -> malformed numeric params indistinguishable from absent ones. Fix: throw IllegalArgumentException (->400) on non-empty unparseable values.
src/api/BackupRouter.java:50: HIGH (security): /api/restore executes arbitrary SQL from an unauthenticated request body (read into memory, written to temp, run via restoreFromSQL) -> exploitable via CSRF from a local browser page. Fix: require auth/CSRF, validate body size, restrict to trusted callers.
src/api/BackupRouter.java:63: HIGH (security): /api/clear wipes the entire DB with no auth/confirmation, reachable via cross-site POST. Fix: require auth/CSRF token.
src/api/BackupRouter.java:58: LOW: tmp.delete() return value ignored -> leaks the temp SQL file (full DB contents) if deletion fails. Fix: Files.deleteIfExists + log on failure.
src/api/ImportExportRouter.java:126: HIGH: fetchCovers returns {"queued": total} but then blocks the HTTP worker thread on awaitTermination(5, MINUTES) -> response only flushed after up to 5 min; concurrent calls can exhaust the thread pool. Fix: run the job on a background executor and return immediately.
src/api/ImportExportRouter.java:121: MEDIUM: pool threads call cd.setLlibreBlob(...) without synchronized(cd), unlike every other writer -> data race on the domain controller. Fix: synchronize on cd or make writes thread-safe.
src/api/ImportExportRouter.java:69: MEDIUM: JsonParser.parseString(body).getAsJsonObject() throws IllegalStateException/JsonSyntaxException for non-object/malformed JSON -> 500 instead of 400. Fix: validate and return 400.
src/api/ImportExportRouter.java:104: LOW: errDetails.add(e.getMessage()) may add null entries when an exception has no message. Fix: coalesce null to the exception class/string.
src/api/HttpRouter.java:93: MEDIUM (security): CORS check uses startsWith("http://localhost:")/"127.0.0.1:" prefix match and echoes the origin; with no auth on any endpoint, state-changing routes are exposed to CSRF from any local page. Fix: exact-allowlist origins + token auth for mutating routes.
src/api/HttpRouter.java:108: MEDIUM: when a path matches but the HTTP method differs, the loop falls through to 404 (or static) and never returns 405 Method Not Allowed. Fix: track path-but-not-method matches and return 405 with an Allow header.
src/api/HttpRouter.java:146: MEDIUM (security): serveStatic builds "/web" + rawPath from the decoded URI and passes it to getResourceAsStream, allowing ../ / %2e%2e traversal out of /web. Fix: normalize/canonicalize and reject paths that escape /web.
src/api/HttpRouter.java:115: LOW: if ctx.commit() partially sends headers then throws, the catch builds a new HttpCtx on the same exchange and re-sends headers (fails, swallowed) -> truncated response. Fix: track a "responseStarted" flag and skip the error path once headers are sent.
src/api/ApiServer.java:43: MEDIUM: classify only maps BibliotecaException/IllegalArgumentException/NumberFormatException; JsonSyntaxException and IllegalStateException fall through to 500 instead of 400. Fix: map client-input/parse exceptions to 400.
src/api/LlibreRouter.java:72: MEDIUM: empty/whitespace body makes gson().fromJson(...) return null, then jsonToLlibre(null) throws NPE -> 500 (malformed JSON likewise). Fix: validate non-null parsed object, return 400 (applies to all routers using this pattern).
src/api/LlibreRouter.java:133: MEDIUM: uploadImage throws generic Exception("Empty image body") (->500) and does no size/content-type validation on the blob. Fix: return 400 for empty/oversized/non-image uploads.
src/api/LlibreRouter.java:59: LOW: Set.of(fieldsParam.split(",")) throws IllegalArgumentException on a duplicate value -> 400 for an otherwise valid request. Fix: build with new HashSet<>.
src/api/LlibreRouter.java:53: LOW: invalid page value is swallowed and treated as page 0 instead of reporting bad input. Fix: validate and return 400.
src/api/LlibreRouter.java:34: MEDIUM: read endpoints (count/list/recent/getOne) access shared cd without synchronization while all writes use synchronized(cd) -> reads can observe mid-write state. Fix: consistent locking for reads too.
src/api/LlistaRouter.java:45: MEDIUM: missing-nom validation throws generic Exception -> 500 instead of 400 (also line 61). Fix: throw IllegalArgumentException / BibliotecaException.Validation.
src/api/LlistaRouter.java:106: LOW: j.get("valoracio").getAsDouble()/getAsBoolean() throw on wrong JSON types -> 500. Fix: validate types, return 400.
src/api/LlistaRouter.java:95: LOW: same Set.of duplicate-fields crash as LlibreRouter. Fix: use a mutable set.
src/api/TagRouter.java:29: MEDIUM: missing-nom validation throws generic Exception -> 500 instead of 400 (also line 38). Fix: throw a validation exception.
src/api/TagRouter.java:51: LOW: addToBook checks existsLlibre outside the synchronized block (TOCTOU) before the add. Fix: check + mutate under the same lock.
src/api/LoanRouter.java:43: MEDIUM: "Borrower name required" thrown as generic Exception -> 500 instead of 400. Fix: throw a validation exception.
src/api/LoanRouter.java:47: MEDIUM: "Book already on loan" thrown as generic Exception -> 500 instead of 409 Conflict. Fix: throw BibliotecaException.Duplicate (->409).
src/api/ConfigRouter.java:114: LOW: setter.accept(j.get(key)) throws on type-mismatched values (getAsInt/getAsDouble on a non-numeric string) -> 500 instead of 400. Fix: catch and return 400 with the offending key.
src/api/OpenLibraryRouter.java:12: LOW: isbn/title path params passed unvalidated to the external OpenLibraryClient; any client failure propagates as 500. Fix: validate inputs, map upstream/network errors to 502/400.
src/main/Ejecutable.java:24: MEDIUM: the default uncaught-exception handler always shows a Swing DialogoError, so a startup failure in --web mode (e.g. port in use) pops a GUI dialog or fails headless instead of logging+exiting. Fix: branch on mode/headless; print to stderr + exit non-zero in web mode.
src/main/Ejecutable.java:79: LOW: Desktop.getDesktop().browse(...) can throw (headless/unsupported), aborting startup after the server already started. Fix: wrap in try/catch and continue.


================================================================================

## todo2 deep-dive

[FILE: src/api/ImportExportRouter.java:115-126]
Problem: ExecutorService is created but never explicitly shut down. If the application shuts down while covers are still being fetched, threads may not terminate cleanly. Also uses `pool.shutdown()` (善良) not `pool.shutdownNow()`.
Code: java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(4,
    r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    ...
    pool.shutdown();
Fix: Register the pool with ShutdownHooks or use try-finally to ensure shutdown, or let the daemon threads die naturally with JVM.

---


---

[FILE: src/api/HttpRouter.java:101]
Problem: Empty catch block for CORS preflight response sending - silently swallows IOException.
Code: } catch (IOException ignored) {}
Fix: At minimum log the exception, even if at FINE level.

---


---

[FILE: src/api/HttpRouter.java:93]
Problem: CORS origin check only allows `http://localhost:*` and `http://127.0.0.1:*` - does not allow `https://` origins or other loopback variants. Useful for local development but prevents production deployment with reverse proxy.
Code: boolean allowedOrigin = origin != null && (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:"));
Fix: Either extend to allow https or make the allowed origins configurable.

---


---

[FILE: src/api/LlibreRouter.java:134]
Problem: `sniffImageMime` defaults to "image/jpeg" for unknown formats, but this could serve a PNG or GIF as "image/jpeg" causing browser to misinterpret. The method only handles PNG (8-byte signature) and JPEG (3-byte SOI), but if a WebP image is stored as blob, it would be served as JPEG incorrectly.
Code: return "image/jpeg";  // fallback for unknown
Fix: Return "application/octet-stream" for unknown formats instead of assuming JPEG.

---


---

[FILE: src/api/LlistaRouter.java:54]
Problem: In `delete`, if `cd.getLlistaById(id)` throws NotFound, it's silently swallowed by the method because it only uses `cd.deleteLlista(...)` which would also throw if not found. Actually it rethrows - let me check. The delete does `cd.deleteLlista(cd.getLlistaById(id))` - if getLlistaById throws, the exception propagates. OK - no issue.

Actually wait - looking at line 54: `cd.deleteLlista(cd.getLlistaById(id));` - if getLlistaById throws NotFound, delete is never called. This is correct behavior - if the shelf doesn't exist, return 404. But actually the exception from getLlistaById is caught by the caller (LlibreRouter's dispatch) and converted to 404. OK.

Actually looking more carefully at line 54, the code says:
```java
private void delete(HttpCtx ctx) throws Exception {
    int id = ctx.pathParamInt("id");
    synchronized (cd) { cd.deleteLlista(cd.getLlistaById(id)); }
    ctx.status(204);
}
```
If `getLlistaById` throws NotFound, it's wrapped by the API framework into a 404. Correct. SKIP.

---


---

[FILE: src/api/HttpCtx.java:78]
Problem: Request body size limit of 50MB (`50 * 1024 * 1024`) is hardcoded and could cause OOM if multiple large requests come in simultaneously. Also the check happens after reading all bytes with `readAllBytes()`.
Code: if (available > 50 * 1024 * 1024) throw new IllegalStateException("Request body too large: " + available + " bytes");
Fix: Consider streaming for large uploads, or use a configurable limit with a default.

---


---

[FILE: src/api/ImportExportRouter.java:48-54]
Problem: In `exportJson`, if `cd instanceof ControladorDomini dom`, it calls `dom.getAllLlibreLlistaRows()` and `dom.getAllLlibreTagRows()` - but these are fetched for all books in memory before looping. If library is large, this creates two large maps in memory.
Code: Map<Long, List<Map<String, Object>>> llistaMap = new HashMap<>();
Map<Long, List<Map<String, Object>>> tagMap = new HashMap<>();
for (persistencia.LlibreLlistaRow row : dom.getAllLlibreLlistaRows()) {
    llistaMap.computeIfAbsent(row.isbn(), k -> new ArrayList<>())
        .add(Map.of("id", row.llistaId(), "valoracio", row.valoracio(), "llegit", row.llegit()));
}
Fix: Consider streaming the export or fetching only for the books being exported.

---


---

[FILE: src/api/LlibreRouter.java:171-178]
Problem: `validate` creates a copy via `Llibre.copyOf(l)` but then the validated copy is returned and later added with `cd.addLlibre(validated)`. The copy includes all fields but the `autors` list is copied as reference (line 195 `c.autors = src.autors != null ? new java.util.ArrayList<>(src.autors) : new java.util.ArrayList<>()` - this creates a NEW ArrayList so it's a deep copy of the list, not a shallow copy). The items (Strings) in the list are immutable so OK. But `imatgeBlob` and `hasBlob` are copied by reference at line 198-199. This could be an issue if the original Llibre is modified later. But since we just created it and are about to add it, it's fine.

Actually wait - `c.imatgeBlob = src.imatgeBlob;` is a reference copy of the byte array. If the source `l` is used again and its `imatgeBlob` is modified, the copy would also see the change. But in this flow, we just validated and are adding. Not a real issue in this context.

Actually I notice that in `copyOf`, the `autors` is properly copied: `c.autors = src.autors != null ? new java.util.ArrayList<>(src.autors) : new java.util.ArrayList<>();`. So that's fine.

OK - no issue here.

---


---

[FILE: src/api/LlibreRouter.java:180-203]
Problem: In `buildFilter`, when a NumberFormatException is caught for isbn parsing at line 183, it's silently ignored. This means an invalid ISBN string in the query param results in no isbn filter being applied. Could lead to unexpected results where bad param values cause filters to be skipped.
Code: if (isbnStr != null) { try { b.isbn(Long.parseLong(isbnStr)); } catch (NumberFormatException ignored) {} }
Fix: Either throw a 400 Bad Request, or at least log at DEBUG level.

---


---

[FILE: src/api/HttpRouter.java:145-164]
Problem: In `serveStatic`, the file content is read and then the stream closed, but if an exception occurs during reading, the response is never sent and the connection may hang.
Code: try (InputStream in = rawIn) { data = in.readAllBytes(); }
Fix: Add proper error handling for failed static file reads.

---


---

[FILE: src/api/ConfigRouter.java:105]
Problem: In `setConfig`, if the JSON body has duplicate keys, only the last value is kept (because JsonObject.get(key) returns last). This could be confusing if a client sends duplicate keys - no error is raised.
Code: JsonObject j = JsonMapper.gson().fromJson(ctx.body(), JsonObject.class);
Fix: Consider detecting duplicate keys in the JSON and returning a 400 error.

---


---

[FILE: src/api/BackupRouter.java:53-60]
Problem: In `restore`, the temp file is created with `File.createTempFile` and deleted in finally, but if the JVM exits abnormally before the finally runs, the temp file persists.
Code: File tmp = File.createTempFile("biblioteca_restore_", ".sql");
    try {
        Files.write(tmp.toPath(), data);
        synchronized (cd) { cd.restoreFromSQL(tmp); }
    } finally {
        tmp.delete();
    }
Fix: Use `tmp.deleteOnExit()` in addition to the finally block, or use a shutdown hook to clean up temp files.

---


---

[FILE: src/api/HttpCtx.java:92]
Problem: The `status` method returns `this` for chaining, but doesn't validate the status code is in valid HTTP range. Negative or out-of-range codes are accepted.
Code: public HttpCtx status(int code) { this.status = code; return this; }
Fix: Validate 100-599 range.

---

## Backlog [1][2][3]

[1] [refactor] exportCSV() and ImportExportRouter.GoodreadsCsvExport separate implementations — unify into one — DONE: BookExporter.exportCSV delegates to ShelfParser; Goodreads has separate service
[2] [clean] BookExporter.exportCSV() and ImportExportRouter.GoodreadsCsvExport separate implementations — unify into one
[3] [refactor] ConfigRouter move port to constant or Config property instead of constructor parameter
[3] [refactor] ConfigRouter constructor wires all routes eagerly — consider lazy init or explicit register() call to make startup order obvious
[3] [refactor] ConfigRouter extract RouterRegistry interface so ApiServer doesn't need to import every Router class directly
[3] [refactor] HttpCtx CORS headers set in HttpRouter.dispatch() but response headers like Cache-Control set in individual routers via responseHeader() — centralize CORS header addition in commit()
[3] [refactor] JsonMapper llibreToMap() manually sets 25 fields — when Llibre gains a field, this silently omits it; use Gson.toJson(l) with custom exclusion strategy
[3] [refactor] LlibreRouter list() builds LlibreFilter from query params with 15+ explicit field assignments — extract to LlibreFilterParser.fromQuery(HttpCtx) helper
[3] [refactor] ConfigRouter split DB config with UI config; split into /api/config/ui and /api/config/db or document which fields require restart
[3] [refactor] LoanRouter loan() re-reads getLoanedISBNs() to check for duplicates — this is a Set lookup but queries DB each time; cache or use cd.existsLlibre() pattern
[3] [refactor] LlistaRouter books() returns full libro JSON including all 25 fields — for shelf view client usually only needs title, author, ISBN; add ?fields= param or slim projection
[3] [refactor] TagRouter findById() linear scan — same issue as LlistaRouter; extract to TagDao or use Map
[2] [refactor] Duplicate utility methods scattered: parseDoubleOrZero() in CsvUtils + ImportExportRouter; csvQ() in ImportExportRouter + CsvUtils-equivalent; parseIsbnParam() in 4 router files — consolidate
