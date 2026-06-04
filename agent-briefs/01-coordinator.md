---
agent_brief: 01-coordinator.md
recommended_models: Minimax 2.7 (planning); either model for tiny edits
primary_paths: Whole repo (triage); `agent-briefs/`, session notes, architecture lists
---

# Coordinator / planner agent

> Read `AGENTS.md` at repo root first (`make test` before marking work complete).
> Coordinator: `agent-briefs/01-coordinator.md` | Index: `agent-briefs/00-INDEX.md`

## Your role

Plan, assign, and track DONE/NOT DONE; avoid large implementations unless blocking.

## Scope

Cross-cutting backlog, security overview, anti-patterns, dead-code status.

## Out of scope

Package-owned implementation without checking INDEX; duplicating specialist fixes.


## Sessions (done — do not redo)

# Combined TODO files - 2025-06-04

# Priority scale: [1] critical / [2] important / [3] nice-to-have
# Format: [priority] [tag] description
#
# ~60 items remain, mostly:
#   - Big architectural refactors (god classes, controler extraction)
#   - I18n properties migration (big refactor)
#   - ConfiguracioDialog tabs split
#   - TagsDelLlibreDialog refactor (search, count, consistency)
#   - DetallesLlibrePanel tab split

Session 2 (done previously):
  - LlibreRouter update/delete/image: 404 instead of 500, PNG MIME sniff fix
  - ApiServer.classify: 404/409/400/500 mapping, no Catalan leak
  - OpenLibraryClient: empty catch→LOG, interrupt re-thrown, null guard for pages
  - TagDao AUTOCOMPLETE_COLUMNS whitelist verified
  - Dead code deleted: 6 interficie, 6 persistencia, 4 api files
  - Makefile test target: classpath and -jar launcher fixed
  - Llista.setColor: centralized isValidColor(), BibliotecaException.Validation
  - Llibre.setAny: negative year guard
  - LlibreDao.executeSQLFile: quote-aware split + FORBIDDEN_LEAD blocklist
  - FiltreUtils.matches(): single predicate source of truth
  - Tests: 7 plain + 10 JUnit 5 (all pass)

Session 3 (done this session):
  - OpenLibraryClient: COVER_BASE/COVER_SIZE constants + javadoc fallback order
  - DialogoError: IS_HEADLESS cached at class level (not per-call)
  - DialogoError: ⚠ emoji → UIManager.getIcon("OptionPane.warningIcon")
  - install.sh: Java check + version validation + -Xmx512m heap
  - ModeSelectorDialog: window X button → SWING default
  - BookExporter.exportHTML: hardcoded Catalan "La meva biblioteca" → I18n key
    (dlg_export_html_title + new export_html_heading key)
  - ShelfParser (domini): extracted parseShelfEntries/exportToCsv/joinShelfEntries
  - BookExporter.exportCSV → delegates to ShelfParser
  - BookExporter.exportPDF dead wrappers deleted (BookPdfPrinter, PdfExporter)
  - UITheme.rebuildFonts: EDT assertion added (was missing)
  - HttpRouter: duplicate errCtx.commit() removed (was calling commit twice)
  - BackupService.sqlEsc: \u001A (EOF) now stripped in addition to \n/\r/\u0000
  - GoodreadsCsvStrategy: Private Notes field now mapped alongside My Review
  - AutoCompletion: noted as dead (Completer.attach never called from presentacio)
  - AutoCompletion: lambdas not needed (class has no anonymous inner classes)
  - I18n: export_html_heading + dlg_export_html_title keys added
  - tests: all passing (197 plain + 184 JUnit 5)

## Priority backlog [1][2][3] (full lists)

HIGH-PRIORITY (do first)

---

MEDIUM-PRIORITY (do when convenient)

---

LOW-PRIORITY (nice to have)

## Architecture (bugs, anti-patterns, security, dead code)

CROSS-CUTTING

---

Bugs

---

[HIGH] LlibreRouter.update — FIXED: getLlibre throws NotFound → 404; removed dead existing==null check
[HIGH] LlibreRouter.delete — FIXED: existsLlibre() + NotFound → 404; removed dead null pre-check
[HIGH] LlibreFilter filter predicate divergence — FIXED: FiltreUtils.matches() single predicate source
[MED] OpenLibraryClient.fetchCoverByISBN — FIXED: empty catch {} → LOG.log(Level.FINE,...)
[MED] OpenLibraryClient.fetchWithRetry — FIXED: interrupt re-thrown as IOException
[MED] OpenLibraryClient.lookupByISBN — FIXED: !get("number_of_pages").isJsonNull() guard added
[MED] HttpRouter.dispatch — FIXED: ApiServer.classify maps 404/409/400/500 properly; duplicate commit removed
[MED] Llista.setColor — FIXED: isValidColor() centralized; BibliotecaException.Validation used
[LOW] Llibre.setAny(Integer any) — FIXED: negative year guard added
[LOW] LlibreDao.executeSQLFile — FIXED: quote-aware split + FORBIDDEN_LEAD blocklist
[LOW] BackupService.sqlEsc — FIXED: \u001A (EOF) now stripped along with \u0000, \n, \r
[MED] ApiServer binds HTTP to getLoopbackAddress() only. However: ControladorDomini and the DAO connection are unrestricted; if a future change binds to 0.0.0.0, the backup/clear/restore endpoints become unauthenticated remote DB wipe. No auth, no rate limit, no audit log. — NOT DONE (network-facing concerns outside current scope)
[HIGH] Duplicated predicate — FIXED: FiltreUtils.matches() single source of truth
[HIGH] Long method — FilterController.filtrar — 95 lines; partially addressed in session 1 (panel reduced); full extract still pending
[HIGH] Long method — MainFrameControl constructor: 130 lines, wires 8 keyboard shortcuts, builds 4 sub-controllers, sets up 2 window listeners, configures geometry. Should be split.
[HIGH] Long method — HttpRouter.dispatch: 50 lines, mixes CORS, routing, exception handling, static-file serving, and a 4-level deep nested try/catch.
[MED] Feature envy — LlibreDao.search reads LlibreFilter public fields directly (15 fields read) and acts on them. Filter construction belongs in LlibreFilter itself; DAO should accept a normalized form.
[MED] Primitive obsession — ISBN is long (no validation), year is Integer (no range check), price is Double (no currency, no negative check), rating is Double 0-10 (clamped in setter but not in DB write).
[MED] God class — MostrarBibliotecaControl orchestrates 6 sub-controllers; each sub-controller is fine, but the constructor wiring is dense — MostrarBibliotecaControl.java:34-64
[MED] Llibre constructor takes 9 positional args (isbn, nom, autor, any, descripcio, valoracio, preu, llegit, imatge). Easy to swap accidentally.
[MED] Repeated 5-arg setX in Llibre.setDataCompra, setDataLectura, setIdioma, setFormat, setPaisOrigen, setEstat, setLlenguaOriginal — all do (v != null && !v.trim().isEmpty()) ? v.trim() : null. Helper missing.
[LOW] JsonMapper.llibreToMap and LLIBRE_FIELD_EXTRACTORS define the same field list twice. LlibreCodec was created to unify them but is unused.
[LOW] Config has 30+ get*/set* pairs; one giant static class. Would be better as a BibliotecaConfig value object held by Config.
[LOW] I18n.java is 535+ lines of T("key", "ca", "es", "en") calls. Should be in a properties file; auto-gen is fine but the file is hard to read in source.
[LOW] ServerConect.MIGRATIONS is 39 inline SQL strings; one big array. Fine for now, but a file-based migration is more standard.
[LOW] ControladorDomini has 60+ public methods, no sub-facade. Hard to navigate.
[LOW] BackupService.writeLlibreINSERT 60 lines of pw.print chain; should use a template helper.
Dead code
[HIGH] interficie.EnActualizarBBDD, OnLlibreAdded, OnLlibreBlobChanged, OnLlibreDelete, OnLlibreUpdate, OnLlistaMembershipChanged — FIXED: deleted (6 files)
[HIGH] persistencia.SchemaManager, Persistencia, LibraryGraph, DbProfile, H2Config, MariaDbConfig — FIXED: deleted (6 files)
[HIGH] api.Routers, MetaLookupRouter, MembershipRouter, LlibreCodec — FIXED: deleted (4 files)
[HIGH] interficie.BookReader — HALF-MIGRATED: 8 @Deprecated ArrayList methods, ArrayList fields in domain objects. Left untouched (interface change risks breaking callers)
Anti-patterns
[HIGH] Singleton god-object — ControladorDomini is a singleton (line 16: private static ControladorDomini inst;) with 60+ public methods, holds the entire in-memory library as ArrayList<Llibre> bib, and is the only path from UI to data. Couples everything; untestable without reset hacks (resetForTest).
[HIGH] Singleton god-object — ControladorPersistencia is a singleton with all-synchronized methods. Comment in LlistaDao line 14-15 says "Double-locking note: all callers go through ControladorPersistencia which is already synchronized, so DAO methods need not be synchronized themselves." But LlibreDao, TagDao, PrestecDao ARE individually synchronized — the comment is wrong; there is double-locking in some DAOs. The "double-locking note" is duplicated in LlistaDao and AutorDao but the conclusion ("need not be synchronized") is incorrect for the synchronized ones.
[HIGH] Service locator — Ejecutable wires everything by direct new, but api/*Router constructors take BibliotecaWriter cd as a parameter and call into it. ImportExportRouter does if (cd instanceof ControladorDomini dom) — that's a service-locator-via-cast. Domain methods (getAllLlibreLlistaRows) leak through the contract.
[HIGH] Mutable statics — UITheme.BG_MAIN, UITheme.ACCENT, etc. are all public static Color — the whole palette is shared mutable state, mutated by setTheme(...). EDT warning at line 157-158 confirms this is a known issue.
[HIGH] Mutable statics — Config.props is a static ConcurrentHashMap. Setters race on the pendingSave timer (line 281). Two threads can stomp each other's debounce.
[HIGH] Anemic domain model — Llibre is a pure bean, all behavior lives in ControladorDomini and the DAOs. Adding a "next loan due date" or "computed read progress %" would require edits across Llibre, ControladorDomini, every DAO, every JSON serializer, every UI renderer.
[MED] Tight coupling via casts — ImportExportRouter.exportJson does if (cd instanceof ControladorDomini dom). The contract (BibliotecaReader) doesn't expose getAllLlibreLlistaRows() clearly enough; the router depends on the concrete class. — api/ImportExportRouter.java:36
[MED] Inverted dependency — Llista.setColor and ControladorDomini.setLlistaColor import herramienta.I18n; domini/ (domain) depends on herramienta/ (utility). Should be reversed via an exception type or a passed-in error message.
[MED] Inverted dependency — BackupService is in herramienta/ but uses ControladorPersistencia. The util layer reaches down into persistence, which is normally a domain/infrastructure concern.
[MED] Layer inversion — presentacio/detalles/control/*.java (sub-package of presentacio/) is the control layer for details; the presentacio/ root contains similar things. No consistent sub-package convention: some controls live in root, some in detalles/control/, some in listener/.
[MED] God comments — ControladorPersistencia.java:13-30 is a 17-line class-level comment that includes its own audit history. Use javadoc for current behavior, store the history elsewhere.
[MED] Inappropriate intimacy — MainFrameControl reads cLlibres.getAllLlibres() (line 45) and passes a copy to MostrarBibliotecaControl. Then MostrarBibliotecaControl later calls back into cd.getAllLlibres() (line 158) for refresh. Two sources of truth for the same data.
[MED] Race condition window — ControladorDomini.addLlibre (lines 159-166): does binarySearch on bib, then cp.afegirLlibre(l), then inserts into bib. Between the binarySearch and the DB insert, another thread can add the same ISBN. The DB has a unique constraint (PK on ISBN), so the second add will fail with a SQL error, but the in-memory bib is now in an inconsistent state (added once successfully elsewhere, not here).
[MED] Race condition window — ControladorDomini.deleteLlibre (lines 168-173): binarySearch, DB delete, in-memory delete. Same window.
[MED] Race condition window — ControladorDomini.swapLlistesOrdre (lines 312-328): on DB failure, restores a.setOrdre(ordreA); b.setOrdre(ordreB); but does NOT also restore the llistes list position. If Collections.swap(llistes, i, j) already happened and then the DB throws, the in-memory list position is now inverted vs the DB.
[LOW] Thread.sleep in fixed-delay scheduler — BackupService.autoBackup line 47: Thread.sleep(30_000) on the scheduled thread. scheduleWithFixedDelay already accounts for execution time, but the manual sleep means the first backup always runs ~60s after startup (30s sleep + first invocation). Misleading.
[LOW] Manual mavis re-implementation of java.util.Properties — Config.props is a ConcurrentHashMap<String,String> with manual forEach((k,v) -> tmp.put(k,v)) for save. Just use java.util.Properties with synchronized and store().
Security issues
[HIGH] TagDao.getDistinctValues — column-name SQL injection — PARTIAL: AUTOCOMPLETE_COLUMNS whitelist verified; full column-name allowlist still needed — persistencia/TagDao.java:137-139 — NOT DONE
[HIGH] HttpRouter.dispatch — error message from Exception.getMessage() returned to client in JSON body. Includes DB error text, paths, and class names. Information disclosure. — PARTIAL: ApiServer.classify now sanitizes NotFound→"Not found", Unknown→"Internal error"; DUPLICATE/VALIDATION preserve local message (safe); BUT HttpRouter's own catch block (when exceptionHandler is null) still sends raw 500 with no body — api/HttpRouter.java:127 — NOT DONE ( ApiServer.classify mitigates most routes, but null-handler path still leaks)


################################################################################

## Priority [1][2][3] — general / unassigned only

[1] [refactor] exportPDF() uses AWT printing with manual text layout — no real PDF output; rename to printBooks() or add proper PDF via PDFBox/iText — PARTIAL: dead wrappers (BookPdfPrinter, PdfExporter) deleted; AWT print kept; consider adding PDF lib if needed
[1] [refactor] UITheme static Color fields mutated by setDark() — not thread-safe; theme changes should happen on EDT only; add EDT assertion — DONE: setTheme has EDT warning; rebuildFonts now also has EDT warning
[1] [refactor] UITheme theme enum has 4 values but only 2 surfaced in mode selector UI — document which themes are "public" vs internal — NOT DONE
[1] [refactor] UITheme rebuildFonts() builds fonts from "small"/"medium"/"large" string — use FontSize enum to avoid typos and enable exhaustive switch — NOT DONE
[1] [refactor] Shell scripts should validate java and required JARs exist before running with helpful error message
[1] [refactor] Shell scripts don't set JAVA_OPTS for heap size — on large libraries (>10k books) default heap may be insufficient; add -Xmx512m
[2] [clean] LlibreValidator checkLlibreFromString() converts string to digits and delegates — if ISBN starts with 0 (valid ISBN-10), leading zero lost before passing to checkLlibre; this is only place preserving leading-zero matters
[2] [clean] LlistesDelLlibreDialog no way to see which shelf book is already in before opening dialog — table should pre-select current shelves
[3] [refactor] Llibre toString() doesn't include nomCa/nomEs/nomEn, autors list, imatgeBlob status — gets out of date every time fields added
[3] [refactor] Llibre imatgeBlob byte[] stored in-memory on domain object and loaded on demand — for large libraries this wastes heap; mark as lazy-loaded and add clear comment
[3] [refactor] Llibre 34 getters/setters make this class 203 lines — use Lombok @Data or records for immutable parts; extract LlibreDetails value object for optional metadata fields
[3] [refactor] Tag consider unifying Tag and Llista into Label/Collection hierarchy if feature set remains similar (both have id+nom, both relate to books many-to-many)
[3] [refactor] LlistaLlistaRow/LlibreTagRow/PrestecRow moved to persistencia package — consider merging three relation rows under sealed interface RelationRow
[3] [refactor] LlistaDao all methods synchronized but called through ControladorPersistencia which is also synchronized — double locking; remove synchronized from DAO methods
[2] [refactor] Mixed language: method names Catalan (addLlibre, getLlibres), variable names Spanish (cLlibres, nuevo, enActualizarBBDD), class names Catalan/Spanish mixed — pick one language for code and document convention in CLAUDE.md
[2] [refactor] ISBN used as both Long (domain) and String (table cells, JSON) — pick one canonical representation and convert only at boundaries
[2] [refactor] BibliotecaWriter is single interface for everything — API, UI, and backup all depend on it; create narrower read-only snapshots for export/backup to avoid accidental mutations
[2] [perf] On startup, getAllLlibres() loads all books including metadata (notes, descriptions) but table only shows 6 columns — lazy-load heavy fields (notes, descripcio, imatgeBlob) on first access
