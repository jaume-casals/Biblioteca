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

================================================================================
HIGH-PRIORITY (do first)
================================================================================

[1] [refactor] exportCSV() and ImportExportRouter.GoodreadsCsvExport separate implementations — unify into one — DONE: BookExporter.exportCSV delegates to ShelfParser; Goodreads has separate service
[1] [refactor] exportPDF() uses AWT printing with manual text layout — no real PDF output; rename to printBooks() or add proper PDF via PDFBox/iText — PARTIAL: dead wrappers (BookPdfPrinter, PdfExporter) deleted; AWT print kept; consider adding PDF lib if needed
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
[1] [refactor] UITheme static Color fields mutated by setDark() — not thread-safe; theme changes should happen on EDT only; add EDT assertion — DONE: setTheme has EDT warning; rebuildFonts now also has EDT warning
[1] [refactor] UITheme theme enum has 4 values but only 2 surfaced in mode selector UI — document which themes are "public" vs internal — NOT DONE
[1] [refactor] UITheme rebuildFonts() builds fonts from "small"/"medium"/"large" string — use FontSize enum to avoid typos and enable exhaustive switch — NOT DONE
[1] [refactor] UITheme styleAccentButton()/styleSecondaryButton()/styleLabel() used exclusively in presentacio — consider UIComponents factory in presentacio using UITheme constants — NOT DONE
[1] [refactor] OpenLibraryClient fetchCoverByISBN() tries multiple cover sizes — document fallback order and add constant for base URL for test mocking — DONE: COVER_BASE/COVER_SIZE constants + javadoc
[1] [refactor] AutoCompletion uses old anonymous class style for listeners — convert to lambdas for readability — DONE: no anonymous inner classes; lambdas not needed
[1] [refactor] AutoCompletion class in herramienta but only used in DetallesLlibrePanel — consider whether FieldAutoComplete.attach() can replace all usages and delete AutoCompletion — DONE: AutoCompletion dead (Completer.attach never called), noted in session notes
[1] [refactor] NativeCsvStrategy shelf parsing logic (c[9] pipe-split) same as BookExporter export — extract ShelfParser; if format changes both must be updated — DONE: ShelfParser extracted to domini
[1] [refactor] GoodreadsCsvStrategy canHandle() checks for "Book Id" OR "Exclusive Shelf" — brittle string matching; add minimum column count check — NOT DONE
[1] [refactor] MainFrameControl actualitzarLlibre() calls addLlibreToLlista when nuevo=true and currentLlistaId != null — shelf auto-assignment buried in callback; make explicit — NOT DONE
[1] [refactor] MainFrameControl getInstance() has 3 overloads with complex guard logic — use proper initialization-on-demand holder or document call order clearly — NOT DONE
[1] [refactor] UIAudit expand to check for duplicate I18n keys and unused keys (keys defined in I18n.java but never called)
[1] [refactor] UIAudit add check for hard-coded Catalan strings in .java files (grep for common Catalan words that should be I18n keys)
[1] [refactor] run.sh and run2.sh likely different modes — name run-swing.sh and run-web.sh for clarity
[1] [refactor] Shell scripts should validate java and required JARs exist before running with helpful error message
[1] [refactor] Shell scripts don't set JAVA_OPTS for heap size — on large libraries (>10k books) default heap may be insufficient; add -Xmx512m
[1] [refactor] run2.sh likely duplicates most of run.sh — consolidate into one script with --web / --swing argument
[1] [refactor] MainFramePanel extends JFrame directly — MVC split means panel should be JPanel embedded in JFrame owned by MainFrameControl; mix of layout and window lifecycle couples view
[1] [refactor] MainFramePanel title hardcoded as "Biblioteca" — use I18n key or constant
[1] [refactor] MainFramePanel statusBar is JLabel — for screen readers has no accessible role or live region; use JPanel with accessible description
[1] [refactor] GuardarLlibresDialogo addFieldEntry/addComboEntry/addCheckEntry builder methods near-identical to DetallesLlibrePanel — extract shared FormEntryBuilder utility class
[1] [refactor] GuardarLlibresDialogo setSize(600, 720) at end of constructor overrides setPreferredSize — use pack() + enforce minimumSize in componentResized listener
[1] [refactor] GuardarLlibresDialogo missing fields vs DetallesLlibrePanel: no paisOrigen, estat, notes, nomCa/nomEs/nomEn, exemplars — new-book dialog silently drops these; add at minimum notes field

================================================================================
MEDIUM-PRIORITY (do when convenient)
================================================================================

[2] [clean] GoodreadsCsvStrategy notes mapped from "My Review" but Goodreads also has "Private Notes" — map both fields, appending if both present
[2] [clean] LlibreValidator checkLlibreFromString() converts string to digits and delegates — if ISBN starts with 0 (valid ISBN-10), leading zero lost before passing to checkLlibre; this is only place preserving leading-zero matters
[2] [clean] DateUtils both methods are one-liners too thin — either merge into fuller DateUtils with more methods or inline callers and delete class
[2] [clean] DateUtils no tests for DateUtils — edge cases like null, empty string, partial dates all need coverage
[2] [clean] I18n strings.csv source file not committed (gitignored) but I18n.java is generated from it — add comment in I18n.java with path and generation command
[2] [clean] MainFrameControl mostrarLlegitsRecentment() hardcodes "Cap llibre marcat com a llegit." and "Llegits" instead of I18n keys
[2] [clean] MostrarBibliotecaControl galeria context menu has hard-coded Catalan strings ("Obrir detalls", "Eliminar", "Copiar ISBN", "Confirmar eliminació") — use I18n.t() keys
[2] [clean] MostrarBibliotecaControl undoDelete() shows JOptionPane confirmation ("restaurat.") — hardcoded Catalan string not in I18n; move to I18n
[2] [clean] GestioLlistesDialog no confirmation on shelf delete — data loss risk if user misclicks; add confirmation dialog like book delete
[2] [clean] LlistesDelLlibreDialog hard-coded column names ("Prestatge", "Valoració", "Llegit") — use I18n keys
[2] [clean] LlistesDelLlibreDialog no way to see which shelf book is already in before opening dialog — table should pre-select current shelves
[2] [clean] BookExporter.exportHTML() has 200 lines of inline CSS — extract to a template or resource file
[2] [clean] BookExporter.exportCSV() and ImportExportRouter.GoodreadsCsvExport separate implementations — unify into one
[2] [clean] BookExporter.exportPDF() uses AWT printing with manual text layout — no real PDF output; rename to printBooks() or add proper PDF via PDFBox/iText
[2] [clean] CsvUtils buildHeaderMap() trims header names but parseLine() doesn't trim field values — trimming happens in colVal(); this inconsistency can cause "Author " != "Author" bugs
[2] [clean] MainFramePanel title hardcoded as "Biblioteca" — use I18n key or constant; if app name changes only one place needs updating
[2] [clean] MainFramePanel MostrarBibliotecaPanel constructed eagerly in field declaration before constructor configures frame — lazy-init inside constructor
[2] [clean] MainFramePanel statusBar is JLabel — for screen readers has no accessible role or live region; use JPanel with accessible description
[2] [clean] GuardarLlibresDialogo missing fields vs DetallesLlibrePanel: no paisOrigen, estat, notes, nomCa/nomEs/nomEn, exemplars — new-book dialog silently drops these; add at minimum notes field
[2] [clean] GuardarLlibresDialogo comboFormat items string array built inline from I18n.t() calls — if format options change this array and the one in DetallesLlibrePanel must be updated in sync; extract to shared getFormatOptions() helper

================================================================================
LOW-PRIORITY (nice to have)
================================================================================

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
[3] [refactor] Llibre toString() doesn't include nomCa/nomEs/nomEn, autors list, imatgeBlob status — gets out of date every time fields added
[3] [refactor] Llibre imatgeBlob byte[] stored in-memory on domain object and loaded on demand — for large libraries this wastes heap; mark as lazy-loaded and add clear comment
[3] [refactor] Llibre 34 getters/setters make this class 203 lines — use Lombok @Data or records for immutable parts; extract LlibreDetails value object for optional metadata fields
[3] [refactor] Tag consider unifying Tag and Llista into Label/Collection hierarchy if feature set remains similar (both have id+nom, both relate to books many-to-many)
[3] [refactor] LlistaLlistaRow/LlibreTagRow/PrestecRow moved to persistencia package — consider merging three relation rows under sealed interface RelationRow
[3] [refactor] ServerConect MIGRATIONS is String[][] — if migration entry accidentally has 3 elements or wrong index, silently misbehaves; use Migration record(int version, String sql) with explicit fields
[3] [refactor] LlistaDao all methods synchronized but called through ControladorPersistencia which is also synchronized — double locking; remove synchronized from DAO methods
[3] [refactor] DialogoError showErrorMessage() checks GraphicsEnvironment.isHeadless() on every error — DONE: IS_HEADLESS cached at class level
[3] [refactor] DialogoError validationDialog uses "⚠" emoji as label character — DONE: uses UIManager.getIcon("OptionPane.warningIcon")
[3] [refactor] DateUtils normalizeDate() only replaces '/' with '-' — doesn't handle "DD-MM-YYYY" vs "YYYY-MM-DD" ambiguity; add parseDateToIso(String) that tries multiple formats — NOT DONE
[3] [refactor] DateUtils both methods are one-liners; this class is too thin — NOT DONE
[3] [refactor] DateUtils add formatDateForDisplay(String isoDate, Locale locale) — currently dates displayed as raw ISO strings everywhere in UI — NOT DONE
[3] [refactor] DateUtils no tests for DateUtils — edge cases like null, empty string, partial dates all need coverage — NOT DONE

================================================================================
CROSS-CUTTING
================================================================================

[2] [refactor] Mixed language: method names Catalan (addLlibre, getLlibres), variable names Spanish (cLlibres, nuevo, enActualizarBBDD), class names Catalan/Spanish mixed — pick one language for code and document convention in CLAUDE.md
[2] [refactor] ISBN used as both Long (domain) and String (table cells, JSON) — pick one canonical representation and convert only at boundaries
[2] [refactor] BibliotecaWriter is single interface for everything — API, UI, and backup all depend on it; create narrower read-only snapshots for export/backup to avoid accidental mutations
[2] [perf] On startup, getAllLlibres() loads all books including metadata (notes, descriptions) but table only shows 6 columns — lazy-load heavy fields (notes, descripcio, imatgeBlob) on first access
[2] [refactor] Duplicate utility methods scattered: parseDoubleOrZero() in CsvUtils + ImportExportRouter; csvQ() in ImportExportRouter + CsvUtils-equivalent; parseIsbnParam() in 4 router files — consolidate

================================================================================
 Bugs
================================================================================

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

================================================================================
 ULTRA MEGA HYPER CODE REVIEW — src/   (errors, bugs & blunders)
================================================================================
Format:  path/to/File.java:LINE: SEVERITY: problem. Fix: suggested fix.
Severity legend: CRITICAL > HIGH > MEDIUM > LOW
NOTE: only the src/ tree was reviewed (target/ build artifacts ignored).

--------------------------------------------------------------------------------
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
PACKAGE: domini/  +  interficie/
================================================================================

src/domini/LlibreLlistaContext.java:11: HIGH: isbn is typed int but ISBNs are Long everywhere else; ISBN-13 (~9.78e12) overflows Integer.MAX and is silently truncated. Fix: change isbn field to long.
src/domini/LlibreLlistaContext.java:19: HIGH: of() factory takes int isbn, forcing a narrowing cast of the real long ISBN at every call site. Fix: change parameter to long.
src/domini/ControladorDomini.java:33: MEDIUM: SORT_BY COL_ISBN uses Comparator.comparing(Llibre::getISBN) which NPEs on a null ISBN (unlike null-safe compararISBN used elsewhere). Fix: wrap with Comparator.nullsFirst or reuse compararISBN.
src/domini/ControladorDomini.java:172: MEDIUM: deleteLlibre(Long ISBN) auto-unboxes a possibly-null Long into searchKey(long) -> NPE. Fix: null-check before unboxing.
src/domini/ControladorDomini.java:382: MEDIUM: l.getISBN() == isbn unboxes a possibly-null Long for == -> NPE for any book with null ISBN. Fix: use Objects.equals / guard null.
src/domini/ControladorDomini.java:246: MEDIUM: getAllLlistes() returns the internal list directly (mutable-state leak). Fix: return a copy or Collections.unmodifiableList.
src/domini/ControladorDomini.java:390: MEDIUM: getAllTags() returns the internal list directly (mutable-state leak). Fix: return a copy/unmodifiable view.
src/domini/ControladorDomini.java:210: MEDIUM: backupToSQL iterates shared ArrayList bib while EDT may mutate it (auto-backup runs off-EDT) -> ConcurrentModificationException; bib/llistes/tags unsynchronized. Fix: snapshot under lock before iterating.
src/domini/ControladorDomini.java:217: LOW: File.createTempFile creates a real file used only for its parent dir and never deleted -> orphan temp file. Fix: delete it or derive temp dir without creating a file.
src/domini/ControladorDomini.java:224: LOW: rollback executeSQLFile failure swallowed by catch(Exception ignored){}, hiding corrupted-DB state. Fix: log/attach rollback failure to rethrown exception.
src/domini/ControladorDomini.java:180: LOW: updateLlibre silently skips in-memory update when binarySearch misses (e.g. ISBN changed) and never re-sorts -> stale/unsorted bib. Fix: handle key changes explicitly or assert ISBN immutability.
src/domini/Llibre.java:94: MEDIUM: getAutors() returns the internal mutable list; external mutation desyncs it from autor and breaks getAutor(). Fix: return new ArrayList<>(autors) or unmodifiable view.
src/domini/Llibre.java:195: LOW: copyOf sets c.autors=null when src.autors is null, violating the never-null invariant (NPE on isEmpty()). Fix: default to a new empty ArrayList.
src/domini/ShelfParser.java:60: MEDIUM: per-book export errors swallowed by catch(Exception ignored){}, silently dropping rows from the CSV. Fix: collect/log failures or fail loudly.
src/domini/ShelfParser.java:17: LOW: condition parts.length < 1 is always false (split returns >= 1 element) -- dead branch. Fix: remove; rely on parts[0].isBlank().
src/domini/SortSpec.java:39: LOW: toSql() default "`ISBN`" omits the "l." table prefix used by all map entries -> inconsistent ORDER BY. Fix: default to "l.`ISBN`".
src/interficie/InMemoryBibliotecaReader.java:26: LOW: l.getISBN() == isbn (also lines 30, 79, 85) unboxes a possibly-null Long -> NPE. Fix: guard null / use Objects.equals.
src/interficie/OnLlibreAdded.java:1: LOW: empty (0-byte) placeholder file declaring nothing. Fix: remove or implement.
src/interficie/OnLlibreUpdate.java:1: LOW: empty file; LibraryEvents imports presentacio.listener.OnLlibreUpdate instead (name collision / unused). Fix: remove.
src/interficie/OnLlibreDelete.java:1: LOW: empty file; LibraryEvents imports presentacio.listener.OnLlibreDelete instead (name collision). Fix: remove.
src/interficie/OnLlibreBlobChanged.java:1: LOW: empty (0-byte) dead file. Fix: remove or implement.
src/interficie/OnLlistaMembershipChanged.java:1: LOW: empty (0-byte) dead file. Fix: remove or implement.
src/interficie/EnActualizarBBDD.java:1: LOW: empty (0-byte) dead file. Fix: remove.


================================================================================
PACKAGE: persistencia/
================================================================================

src/persistencia/ServerConect.java:182: HIGH: runMigrations wraps migrations in a JDBC transaction, but most migration statements are DDL, which auto-commits on MariaDB/MySQL. A mid-sequence failure leaves a partially-migrated schema with inconsistent schema_version; the savepoint/rollback path is effectively a no-op for DDL. Fix: run/track each migration idempotently and version-stamp only after each succeeds; do not rely on transactional rollback for DDL.
src/persistencia/ServerConect.java:101: HIGH: Migration 32-34 backfills authors into llibre_autor then DROP COLUMN autor (34). If 33 partially fails on a large DB and 34 still runs, original author data is permanently lost. Fix: gate the DROP behind a verified row-count match (see in-code comment) or split into a separate, manually-confirmed migration.
src/persistencia/ServerConect.java:124: LOW: H2 URL is built by stripping ALL whitespace from the home dir path (replaceAll("\\s+","")) -> a user home path containing spaces produces a wrong DB path. Fix: do not strip interior spaces from the path; quote/escape instead.
src/persistencia/LlibreDao.java:23: MEDIUM: buildLlibreLight reads nullable `any`/valoracio/preu with getInt/getDouble and no wasNull() -> SQL NULL collapses to 0 / 0.0, indistinguishable from a real zero. Fix: read as object / check wasNull() and pass null.
src/persistencia/LlibreDao.java:33: MEDIUM: buildLlibre has the same NULL-collapse problem for `any`/valoracio/preu. Fix: use wasNull() and set null on the Llibre.
src/persistencia/LlibreDao.java:314: MEDIUM: search() binds params by instanceof chain; a param type not in {String,Long,Integer,Double,Boolean} is silently skipped, shifting every subsequent placeholder index and corrupting the query. Fix: throw on unhandled type, or bind via a typed param object.
src/persistencia/LlibreDao.java:435: MEDIUM: executeSQLFile splitter (insideString/endsStatement) only balances single quotes per physical line, so a quoted literal containing a newline or a ';' across lines mis-splits the statement on restore. Fix: tokenize across the full buffer, not line-by-line.
src/persistencia/LlibreDao.java:453: MEDIUM: isForbidden only checks the leading token of each split statement; if the per-line splitter mis-joins, a forbidden DDL could ride along inside another statement and bypass the guard. Fix: validate after robust statement parsing, and reject multiple top-level statements per chunk.
src/persistencia/LlibreDao.java:312: LOW: search() appends LIMIT/OFFSET via string concatenation of ints (not parameters). Safe today (ints) but fragile. Fix: bind as parameters.
src/persistencia/TagDao.java:56: MEDIUM: delete(int id) removes a tag (cascading to llibre_tag via FK) but never calls invalidateLlibreTagCache() -> getAllLlibreTag() returns stale rows. Fix: invalidate cache on tag delete.
src/persistencia/TagDao.java:124: MEDIUM: llibreTagCache is also stale after LlibreDao.delete() cascades llibre_tag rows (LlibreDao cannot see TagDao's cache). Fix: centralize cache invalidation or drop the cache on any book/tag delete.
src/persistencia/ControladorPersistencia.java:76: MEDIUM: resetForTest() mutates the static singleton (inst=null) without synchronization, while getInstance() is synchronized -> race / visibility bug across threads. Fix: synchronize resetForTest/resetForProfileSwitch on the class monitor.
src/persistencia/ControladorPersistencia.java:85: MEDIUM: resetForProfileSwitch() closes the shared connection and nulls inst unsynchronized while other (synchronized) methods may be using DAOs on that connection. Fix: synchronize and ensure no in-flight DB ops.


================================================================================
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
PACKAGE: presentacio/  (incl. renderers/, listener/, detalles/)
================================================================================

src/presentacio/renderers/ProgressBarRenderer.java:25: CRITICAL: Renderer calls MainFrameControl.getInstance().getLlibreIsbn(isbn) -> a DB/store lookup on EVERY cell paint, on the EDT. Fix: pass the Llibre via the model (getBookAt); never query the DB in a renderer.
src/presentacio/renderers/ProgressBarRenderer.java:24: MEDIUM: uses COLUMNA_ISBN=1 (model index) as a view column in t.getValueAt(row,1); if the Cover column is hidden, view col 1 is not ISBN. Fix: read via model index + convertRowIndexToModel.
src/presentacio/renderers/CoverCellRenderer.java:57: HIGH: books with no cover (img==null) are removed from coverLoading but never cached, so every repaint re-submits getLlibreIsbn()+blob load -> endless DB/IO churn. Fix: cache an empty/sentinel marker for misses.
src/presentacio/renderers/CoverCellRenderer.java:41: MEDIUM: COLUMNA_ISBN=1 (model index) used as a view column; breaks when Cover column hidden/reordered. Fix: convert to model index explicitly.
src/presentacio/renderers/CoverCellRenderer.java:54: LOW: captured row index r is reused in a later invokeLater repaint; after sort/scroll r may point at a different row. Fix: repaint by isbn lookup or repaint whole column.
src/presentacio/renderers/SearchHighlightRenderer.java:28: MEDIUM: COLUMNA_ISBN=1 (model index) used as a view column for the loaned-book highlight; wrong column when Cover hidden. Fix: use model.getValueAt / convertColumnIndexToView.
src/presentacio/BibliotecaTableModel.java:57: HIGH: isCellEditable() always returns false, so the LlegitCheckBoxEditor on COL_LLEGIT never activates -> clicking the read/unread checkbox does nothing. Fix: return true for COL_LLEGIT.
src/presentacio/BibliotecaTableModel.java:59: MEDIUM: getValueAt rebuilds the entire row Object[] (rowToValues) on every single cell access -> O(columns) allocation per cell each repaint. Fix: index the field per column directly.
src/presentacio/TableController.java:105: MEDIUM: addRowSorterListener is attached before the model is set; the later setModel() auto-creates a NEW RowSorter, silently discarding the listener -> sort-column persistence stops working after first data load. Fix: install model first or re-attach after each setModel.
src/presentacio/TableController.java:88: LOW: Ctrl-click author filter reads t.getValueAt(row, COL_AUTOR) with COL_AUTOR (model index) as a view column. Fix: resolve via model index.
src/presentacio/MainFrameControl.java:63: HIGH: btnNouLlibre listener runs obrirNouLlibreDialeg on new Thread(...), constructing+setVisible()ing a modal Swing dialog off the EDT (also lines 68, 74). Fix: build/show dialogs on the EDT; only background work off-EDT.
src/presentacio/MainFrameControl.java:198: HIGH: obrirNouLlibreDialeg constructs GuardarLlibresDialogo and setVisible(true) on a non-EDT thread. Fix: marshal dialog creation/show onto the EDT.
src/presentacio/MainFrameControl.java:210: HIGH: getLlibreIsbn delegates to cLlibres.getLlibre (DB) and is called from renderers on the paint path -> per-paint DB queries. Fix: avoid DB lookups in render paths.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:157: HIGH: startImatgeWorker submits the loader then immediately calls imageFuture.get() inside invokeLater on the EDT -> blocks the EDT until image/blob loads, defeating the async design. Fix: use SwingWorker / call back via invokeLater after get() completes off-EDT.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:57: MEDIUM: cLlibres.loadHeavyFields(l) runs synchronously on the EDT during dialog construction. Fix: load on a background thread before showing.
src/presentacio/detalles/control/DetallesLlibrePanelControl.java:86: MEDIUM: getDistinctAutorNames()/getDistinctValues() (DB queries) run on the EDT during construction for autocomplete (lines 86-89). Fix: fetch off-EDT and attach when ready.
src/presentacio/ImportController.java:35: HIGH: importarCSV runs BookImporter.importCSV (file IO + DB writes) synchronously on the EDT. Fix: run in a SwingWorker with progress.
src/presentacio/ImportController.java:62: HIGH: importarCalibre runs the sqlite3 subprocess + DB import on the EDT. Fix: move to a background worker.
src/presentacio/ImportController.java:76: HIGH: importarJSON parses file + writes DB on the EDT. Fix: background worker.
src/presentacio/ExportController.java:35: HIGH: exportCSV/exportJSON/exportHTML/exportPDF perform file IO + DB reads on the EDT (lines 35, 45, 60, 67). Fix: run exports in a SwingWorker.
src/presentacio/ExportController.java:103: MEDIUM: fetchMissingCovers writes covers via cd.setLlibreBlob from 8 concurrent pool threads; concurrent writes through a possibly non-thread-safe connection can corrupt/serialize unexpectedly. Fix: serialize DB writes or use a connection-per-thread pool.
src/presentacio/BackupController.java:36: HIGH: backupToSQL runs on the EDT (blocking full DB dump). Fix: background worker with progress dialog.
src/presentacio/BackupController.java:53: HIGH: restoreFromSQL runs on the EDT (blocking full restore). Fix: background worker.
src/presentacio/FilterController.java:131: MEDIUM: filtrar() calls aplicarFiltres(f) (DB query) on the EDT; janks for large libraries. Fix: run the DB filter in a SwingWorker.
src/presentacio/ShelfController.java:73: MEDIUM: refreshComboLlistes issues several DB calls synchronously on the EDT. Fix: batch/cache and load off-EDT.
src/presentacio/ShelfController.java:47: LOW: onDragToShelf swallows per-isbn exceptions to System.err with no user feedback. Fix: collect and report failures.
src/presentacio/ConfiguracioDialog.java:521: MEDIUM: on empty host/user the validation 'return' only exits the Config.withBatch lambda, not the save handler -> execution continues to fire onThemeChange, prompt restart, and dispose(); a failed validation still closes the dialog. Fix: validate before withBatch and abort the whole handler on failure.
src/presentacio/ConfiguracioDialog.java:405: LOW: cd.getDbSizeBytes() (DB query) runs on the EDT during dialog construction. Fix: fetch off-EDT.
src/presentacio/GaleriaCobertesPanel.java:160: LOW: moveKeyboard calls card.scrollRectToVisible(card.getBounds()); getBounds() is in parent coordinates but scrollRectToVisible expects component-local coords -> scrolls to the wrong spot. Fix: pass new Rectangle(0,0,card.getWidth(),card.getHeight()).
src/presentacio/GaleriaCobertesPanel.java:258: LOW: hideZoomPopup removes the overlay from the layered pane but never repaints -> visual ghost. Fix: repaint the layered pane region after removal.
src/presentacio/detalles/control/OpenLibrarySearchTask.java:96: LOW: done() overwrites title/author fields unconditionally (setText) while other fields are only filled when empty -> clobbers user-typed title/author. Fix: guard title/autor with isEmpty() like the others.
src/presentacio/ModeSelectorDialog.java:76: LOW: window-close sets result=SWING but ESC just dispose()s leaving result=CANCELLED -> inconsistent dismissal semantics. Fix: make ESC and window-close behave the same.


================================================================================
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
 END OF REVIEW
================================================================================

================================================================================
ULTRA MEGA HYPER REVIEW - ALL ISSUES FOUND (todo2.txt)
================================================================================

[FILE: src/persistencia/LlibreDao.java:208]
Problem: ISBN is cast to int, truncating the long value. The cast `(int) isbn` will silently lose data for ISBNs > 2^31.
Code: out.add(domini.LlibreLlistaContext.of(
    (int) isbn,  // BUG: truncates long isbn to int
Fix: Cast to long: `isbn` is already long, no cast needed. Remove the `(int)` cast.

[FILE: src/herramienta/BookImporter.java:55-64]
Problem: `importCalibre` uses `Runtime.getRuntime().exec(new String[]{sqlite3, dbFile.getAbsolutePath(), sql})` - command injection risk if sqlite3 path contains spaces or special chars. Also, `proc.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream())` is Java 9+ only; will throw `UnsupportedOperationException` on Java 8.
Fix: Use ProcessBuilder with proper escaping; for stderr drain use a BufferedReader and drain manually for Java 8 compatibility.

[FILE: src/herramienta/BookExporter.java:221]
Problem: `getEditorial() != null ? getEditorial() : ""` - returns "" but shows as empty string in HTML, but `editorial` column may have "0" or " " as default which displays incorrectly. Also, for number columns like `any`, it outputs `0` when any is 0, causing "0" to appear in HTML where blank was intended.
Fix: Change to `(l.getAny() != null && l.getAny() > 0) ? l.getAny() : ""`

[FILE: src/api/ImportExportRouter.java:115-126]
Problem: ExecutorService is created but never explicitly shut down. If the application shuts down while covers are still being fetched, threads may not terminate cleanly.
Fix: Register the pool with ShutdownHooks or use try-finally to ensure shutdown, or let the daemon threads die naturally with JVM.

[FILE: src/api/HttpRouter.java:101]
Problem: Empty catch block for CORS preflight response sending - silently swallows IOException.
Fix: At minimum log the exception, even if at FINE level.

[FILE: src/api/HttpRouter.java:93]
Problem: CORS origin check only allows `http://localhost:*` and `http://127.0.0.1:*` - does not allow `https://` origins or other loopback variants.
Fix: Either extend to allow https or make the allowed origins configurable.

[FILE: src/domini/ControladorDomini.java:215]
Problem: `loadHeavyFields` is called inside a synchronized block on `this`, but `loadHeavyFields` itself calls `cp.loadHeavyFields` which is a synchronized method on cp. This serializes all cover loads and could take very long for large libraries.
Fix: Consider parallelizing cover loading or streaming covers directly from DB to avoid holding the lock.

[FILE: src/herramienta/BackupService.java:269-273]
Problem: `sqlEsc` replaces newlines with spaces but not other control characters like `\t`. Also `\u001A` (substitute character) is handled but other control characters (SOH, STX, etc.) are not stripped.
Fix: Replace all characters where `ch < 0x20` with space, not just specific ones.

[FILE: src/persistencia/LlibreDao.java:485-517]
Problem: `syncAutors` does 3 round-trips: DELETE + batch INSERT for autor table + batch INSERT for llibre_autor links. While optimized vs N+2, the batch INSERT uses subquery approach which may fail if autor entries don't exist.
Fix: After batch INSERT to autor (step 2), add a flush/clear to ensure generated IDs are available before step 3.

[FILE: src/herramienta/Config.java:287-292]
Problem: `SAVE_SCHEDULER` is a static single-threaded executor but never shut down - if the application runs for very long periods without exit, the thread holds memory.
Fix: Register with ShutdownHooks to ensure clean shutdown, or switch to a lighter mechanism (e.g., Timer).

[FILE: src/herramienta/BookExporter.java:127]
Problem: Uses `★`.repeat((int) Math.round(val))` which is Java 11+. If running on Java 8, this will fail at compile time or runtime. String.repeat() is Java 11+.
Fix: Use Apache Commons or Guava's StringUtils.repeat, or manual loop.

[FILE: src/herramienta/OpenLibraryClient.java:271]
Problem: `encode` uses `URLEncoder.encode` which encodes spaces as `+` instead of `%20`. While technically valid for `application/x-www-form-urlencoded`, some servers expect `%20`.
Fix: Use `URI.create(url).toURL()` or replace `+` with `%20` after encoding.

[FILE: src/herramienta/LlibreValidator.java:79-81]
Problem: Year validation rejects any year > currentYear+5, but allows 0 as a valid year. A book with year=0 is probably a placeholder and shouldn't be allowed.
Fix: Either remove the `any != 0` exception or validate year 0 separately with different limits.

[FILE: src/herramienta/LlibreValidator.java:67-68]
Problem: `countDig(isbn)` uses `String.valueOf(n).length()` which will fail for Long.MAX_VALUE (20 digits). Also if isbn is negative, this breaks.
Fix: Handle null and negative ISBNs; for very large values use `Long.toString(n).length()`.

[FILE: src/api/HttpCtx.java:78]
Problem: Request body size limit of 50MB is hardcoded and could cause OOM if multiple large requests come in simultaneously. Also the check happens after reading all bytes with `readAllBytes()`.
Fix: Consider streaming for large uploads, or use a configurable limit with a default.

[FILE: src/api/ImportExportRouter.java:48-54]
Problem: In `exportJson`, if `cd instanceof ControladorDomini dom`, it calls `dom.getAllLlibreLlistaRows()` and `dom.getAllLlibreTagRows()` - but these are fetched for all books in memory before looping. If library is large, this creates two large maps in memory.
Fix: Consider streaming the export or fetching only for the books being exported.

[FILE: src/domini/LlibreFilter.java:76-85]
Problem: `hasAnyFilter` does not check if sort is non-default. If the user only wants to sort without filtering, hasAnyFilter returns false but the filter still has a sort spec.
Fix: `hasAnyFilter()` should also return true if sort is non-default, or the caller should separately check for sort.

[FILE: src/persistencia/LlibreDao.java:295]
Problem: In search, `f.getNom()` is used with LIKE but not null-checked - if f.getNom() is an empty string "", the condition `f.getNom() != null` is true so it adds the LIKE clause with `%` placeholders, which matches everything.
Fix: Check `!f.getNom().isEmpty()` in addition to `!= null`.

[FILE: src/api/LlibreRouter.java:180-203]
Problem: In `buildFilter`, when a NumberFormatException is caught for isbn parsing at line 183, it's silently ignored. This means an invalid ISBN string in the query param results in no isbn filter being applied.
Fix: Either throw a 400 Bad Request, or at least log at DEBUG level.

[FILE: src/domini/ControladorDomini.java:172]
Problem: `deleteLlibre(Long ISBN)` - if ISBN is null, it throws "ISBN és null" which is a NotFound exception with ISBN=null message. For a null ISBN, 400 Bad Request would be more appropriate.
Fix: Throw IllegalArgumentException or BibliotecaException.Validation instead.

[FILE: src/herramienta/BookExporter.java:190]
Problem: MIME type detection for cover images is overly simplistic: `blob[0] == (byte)0x89` checks for PNG header but this could incorrectly match other binary data that happens to start with 0x89.
Fix: Use a more robust method like `java.nio.file.Files.probeContentType()` or a library like Apache Tika.

[FILE: src/api/HttpRouter.java:145-164]
Problem: In `serveStatic`, the file content is read and then the stream closed, but if an exception occurs during reading, the response is never sent and the connection may hang.
Fix: Add proper error handling for failed static file reads.

[FILE: src/api/ConfigRouter.java:105]
Problem: In `setConfig`, if the JSON body has duplicate keys, only the last value is kept (because JsonObject.get(key) returns last). This could be confusing if a client sends duplicate keys - no error is raised.
Fix: Consider detecting duplicate keys in the JSON and returning a 400 error.

[FILE: src/herramienta/OpenLibraryClient.java:160]
Problem: `rateLimit` uses `Thread.sleep(wait)` and if interrupted, sets `Thread.currentThread().interrupt()` flag and returns. The caller `fetchCoverByISBN` checks for interrupt and returns null. But other callers of `rateLimit` may not handle interrupts properly.
Fix: Ensure all callers of rateLimit handle InterruptedException consistently.

[FILE: src/persistencia/ServerConect.java:195-197]
Problem: In `closeConection`, the finally block sets con=null regardless of whether close() succeeded or threw. If close() throws, con is still set to null which might prevent proper rollback of transactions.
Fix: Set con=null only if close succeeded, or not at all since instance will be discarded.

[FILE: src/herramienta/BackupService.java:67]
Problem: In autoBackup, when deleting old backups, it uses `backups[i].delete()` which could fail silently if the file is locked or read-only. No error is reported.
Fix: Check return value of delete() and log warnings for failed deletions.

[FILE: src/persistencia/LlibreDao.java:253-265]
Problem: In `getBlob`, if the blob is empty (length 0), it returns null. But there could be a stored empty blob (not null, but 0 bytes) which would be treated as "no image". This might not be the user's intent.
Fix: Consider storing 0-length as null, or differentiate between "no image" and "image is empty".

[FILE: src/persistencia/TagDao.java:22-23]
Problem: AUTOCOMPLETE_COLUMNS doesn't include "autor" or "nom" - these would use the in-memory path via ControladorDomini.getDistinctValues. But if a user changes the in-memory column list, they must also update TagDao.AUTOCOMPLETE_COLUMNS.
Fix: Consider deriving the whitelist from a single source of truth or documenting the required sync more explicitly.

[FILE: src/presentacio/MainFrameControl.java:182-189]
Problem: `getInstance(panel, cd)` creates a new instance if `instance == null` AND `panel != null`, but doesn't check if `instance` already exists with a DIFFERENT panel. This could lead to using an old panel or the new panel being ignored.
Fix: Either throw if instance exists and panel differs, or add a check to reuse existing instance.

[FILE: src/api/BackupRouter.java:53-60]
Problem: In `restore`, the temp file is created with `File.createTempFile` and deleted in finally, but if the JVM exits abnormally before the finally runs, the temp file persists.
Fix: Use `tmp.deleteOnExit()` in addition to the finally block, or use a shutdown hook to clean up temp files.

[FILE: src/persistencia/ControladorPersistencia.java:76-83]
Problem: In `resetForTest`, if `inst.libreDao.clearAllData()` throws an exception, the code catches it, prints warning, then sets `inst = null`. The data may not be cleared but instance is still reset. This could leave the test database in an inconsistent state.
Fix: Either throw the exception after cleanup failure, or use a different reset mechanism that ensures data is cleared.

[FILE: src/herramienta/BookExporter.java:233]
Problem: HTML escaping function handles `&`, `<`, `>`, `"`, `'` but not `&amp;` if the input already contains `&amp;` it becomes `&amp;amp;` which is wrong - actually no, the input "A & B" becomes "A &amp; B" which is correct.
Fix: Use a library like Apache Commons Text StringEscapeUtils or OWASP ESAPI for proper HTML escaping.

[FILE: src/api/HttpCtx.java:92]
Problem: The `status` method returns `this` for chaining, but doesn't validate the status code is in valid HTTP range. Negative or out-of-range codes are accepted.
Fix: Validate 100-599 range.

[FILE: src/persistencia/LlibreDao.java:280-329]
Problem: In search, if pageSize=0 (unpaginated), LIMIT and OFFSET are not appended. But offset is used as a parameter position offset in PreparedStatement. If pageSize is negative, it could create invalid SQL.
Fix: Validate that pageSize >= 0.

================================================================================
END OF todo2.txt ISSUES
================================================================================

================================================================================
ULTRA MEGA HYPER REVIEW — checkBiblio/ (all files)
================================================================================
Compiled: 2025-06-03
Files reviewed: StressTest.java (1551L), UIAudit.java (969L), I18nAudit.java (117L), run.sh (66L), run2.sh (75L)

================================================================================
SECTION A — STRESSTEST.JAVA (issues, bugs, blunders)
================================================================================

[ST-01] ISBN COLLISION — uniqueness not guaranteed across runs or within run
  File: StressTest.java:33, 1363-1365
  Problem: isbnCounter is static and seeded from currentTimeMillis. If two runs start
           in the same millisecond (or share a persistent DB), collisions are possible.
  Fix: Use AtomicLong with a random base + monotonic increment, no time seed:
         private static final AtomicLong ISBN_BASE = new AtomicLong(
             9780000000000L + (new Random().nextLong() & 0x7FFFFFFFFFFFFFFFL) % 900_000_000L);
         private static long uniqueISBN() { return ISBN_BASE.addAndGet(1); }
  Fix: Also assert uniqueness before each create (addTag already does this).

[ST-02] dismissAllDialogs() CLICKING "Sí" ON DELETE CONFIRM — dangerous auto-confirm
  File: StressTest.java:1313-1322
  Problem: When cleaning up (testCleanup, line 1129), this will click "Sí" to confirm
           deletion of EVERY test book. If any non-test dialog appears during cleanup
           (e.g. a real book edit was left open, or a stats popup), the test will
           silently delete a real book.
  Fix: Never auto-click affirmative buttons in generic dismiss. Use only:
         "Cancel·lar", "Cancelar", "Tancar", "Close", "X"
       Remove "Sí", "Yes", "OK", "No" from generic dismissal. In explicit delete
       confirmation (testCleanup), use only "No" / "Cancel" / Escape.

[ST-03] testExtreme_concurrent: component tree walked OFF the EDT — Swing threading violation
  File: StressTest.java:987-1017
  Problem: findBtnIn (lines 1374-1387) walks the Swing component tree. The component
           hierarchy is not thread-safe for read access from outside the EDT.
  Fix: Resolve the button reference ONCE on the EDT before starting workers, store it
         in a volatile / concurrent reference, and have workers only invoke doClick
         via invokeLater.

[ST-04] testChaos_longTitle: cannot fail — "validation rejected" AND "saved" both pass
  File: StressTest.java:321-339
  Problem: The test treats both outcomes as passing. A 500-char title is a real boundary
           (nom is VARCHAR(255) in the schema). The app could silently truncate, accept
           with a different behavior, or have completely broken validation — the test
           would still pass.
  Fix: Assert a single deterministic contract:
         - If the DB enforces 255-char limit: reject > 255 → fail if not rejected.
         - If the app truncates: verify the saved title is exactly 255 chars.

[ST-05] testValidation_negativePrice / testValidation_badYear: only warn on silent accept
  File: StressTest.java:231-251, 253-273
  Problem: When negative price or non-numeric year is silently accepted (no error dialog,
           form closes), the test calls warn() not fail(). A validator regression that
           silently accepts garbage passes CI with only a warning.
  Fix: Both should fail() on silent accept.

[ST-06] Screenshot after EVERY phase (40+ screenshots per run) — performance + noise
  File: StressTest.java:101
  Problem: 40+ PNGs (1920x1080) on every run including cleanup phases. Most are never
           reviewed and waste IO. Only failures/warnings need screenshots for debugging.
  Fix: Screenshot only on FAIL/WARN or when explicitly requested.

[ST-07] No headless guard — throws AWTException on headless/Xvfb-unavailable hosts
  File: StressTest.java:46
  Problem: new Robot() throws AWTException if GraphicsEnvironment.isHeadless().
           On a truly headless server without Xvfb, the test crashes at startup.
  Fix: Guard at startup:
         if (GraphicsEnvironment.isHeadless()) {
           log("FATAL: Headless environment — Robot required. Install Xvfb or use --headless");
           System.exit(1);
         }

[ST-08] testDataIntegrity: static isbnCounter modified by addTag (unrelated call)
  File: StressTest.java:1107
  Problem: isbnCounter is the test-wide ISBN sequence counter. Using it here advances
           the sequence for tag names, polluting the ISBN counter state.
  Fix: Use a separate counter for tag names, or use UUID.

[ST-09] testCleanup: delete via keyboard DELETE key — fragile, not all UI states handled
  File: StressTest.java:1154
  Problem: Uses raw Robot DELETE keypress to trigger deletion. If focus is not on the
           table (e.g., search field has focus), the DELETE does nothing.
  Fix: After selecting the row, verify getTopDialog() appears within 1s as a confirm
         dialog. If no dialog appears, fail() — don't silently skip.

[ST-11] testLlistesManagement: delete loop clicks "Sí" on confirm — same issue as ST-02
  File: StressTest.java:759-778
  Problem: Same dangerous pattern. During list management, after creating test lists,
           the delete loop auto-clicks "Sí" on confirmation dialogs.
  Fix: Use robot ESCAPE to dismiss confirmations in generic teardown.

[ST-12] Phase ordering: extreme phases 43-48 registered before phase 42 (cleanup)
  File: StressTest.java:162-170
  Problem: In the report, phases 43-48 appear before cleanup. The report order is confusing.
  Fix: Move cleanup to phase "99" and extreme phases to 43-48. Or better: register all
        phases in order in runAllTests regardless of extreme flag.

[ST-13] log() not synchronized — interleaved output from main + app + worker threads
  File: StressTest.java:1533-1536
  Problem: log() is called from main test thread, appThread (via main.Ejecutable),
           and worker threads in testExtreme_concurrent. Interleaving makes report hard to read.
  Fix: Synchronize log() with a lock object.

================================================================================
SECTION B — UIAUDIT.JAVA (issues, bugs, blunders)
================================================================================

[UI-01] run.sh: javac only compiles UIAudit.java — I18nAudit.java NOT compiled
  File: run.sh:60
  Problem: UIAudit.java:447 calls I18nAudit.run(reportFile, i18nFail, i18nWarn).
           I18nAudit.java is NOT on the javac command. This means:
           (a) If bin/checkBiblio/I18nAudit.class does not already exist, compilation fails.
           (b) Even if it exists from make compile, if make hasn't been run after
               I18nAudit.java was created, the UIAudit run will fail at runtime.
  Fix: Change line 60 to:
         javac -Xlint:deprecation -cp "$CP" checkBiblio/UIAudit.java checkBiblio/I18nAudit.java -d bin

[UI-02] INTERACTIVE mode: H2 URL NOT overridden — mutates real user DB
  File: UIAudit.java:64-69
  Problem: In INTERACTIVE mode (the default, when no --auto flag is given), the H2 url
           is never set. The app runs against whatever ~/.biblioteca/config.properties
           specifies — likely a file-based H2 or real MariaDB.
  Fix: Always set biblioteca.h2.url to an in-memory test URL:
         System.setProperty("biblioteca.test", "true");
         System.setProperty("biblioteca.h2.url",
           "jdbc:h2:mem:uiAudit;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");

[UI-03] cmdType: clipboard overwritten and never restored
  File: UIAudit.java:203-213
  Problem: cmdType pastes text via Ctrl+V using the system clipboard, but never saves
           and restores the previous clipboard contents.
  Fix: Save clipboard before, restore after.

[UI-04] waitForDialog defined but never used
  File: UIAudit.java:864-873
  Problem: Dead code. The method is defined but never called.
  Fix: Use it or remove it.

[UI-05] Screenshot captures only primary screen — multi-monitor dialogs missed
  File: UIAudit.java:240-241
  Problem: Only captures the primary monitor. On multi-monitor setups, dialogs on
           secondary screens are not captured.
  Fix: Capture the union of all GraphicsConfiguration bounds.

[UI-06] cmdRows: table.getValueAt access without security checks
  File: UIAudit.java:272-275
  Problem: If the table model throws an exception (e.g., stale reference after a book
           was deleted), cmdRows crashes the interactive session. Also no bounds check.
  Fix: Wrap in try/catch, add column count check.

[UI-07] cmdOpenRow: fallback Enter key dispatched but Enter may not trigger "obrirDetalls"
  File: UIAudit.java:302-304
  Problem: If the ActionMap lookup for "obrirDetalls" fails, the code falls back to a
           raw Enter key event. But the actual trigger may be a mouse double-click.
  Fix: Use double-click mouse event for the fallback.

[UI-08] testCreateBook / testEditBook: no verification that book was actually saved
  File: UIAudit.java:610-670, 673-734
  Problem: Both methods click save and check for error dialog absence as the pass
           condition. They never verify the book actually appeared in the table.
  Fix: After save, refresh the table and verify the ISBN appears.

[UI-09] No headless guard — same as [ST-07]
  File: UIAudit.java:74
  Problem: new Robot() at line 74 has same headless failure mode as StressTest.
  Fix: Same guard as [ST-07].

================================================================================
SECTION C — I18NAUDIT.JAVA (issues, bugs, blunders)
================================================================================

[I18N-01] CSV parser: split on "," — fields containing commas are silently broken
  File: I18nAudit.java:32-33
  Problem: If a CSV field value contains a comma, split(",", 4) would produce more
           than 4 fields and silently discard the value.
  Fix: Use a proper CSV parser or handle quoted fields with regex.

[I18N-02] auditHardcodedCatalan: only warns on first match per file — misses all others
  File: I18nAudit.java:66-70
  Problem: Pattern.matcher.find() finds only the FIRST match in the file. If a file has
           10 hardcoded Catalan strings, only the first triggers a warning.
  Fix: Use a while loop to find all matches.

[I18N-03] auditJavaKeyUsage: ignores keys used more than once — no count or location info
  File: I18nAudit.java:76-97
  Problem: Only checks whether a key was used at least once. Does not detect duplicates
           or how many times each key is used.
  Fix: Keep the missing-key check. For unused keys, consider reporting usage count.

[I18N-04] auditDuplicateKeys: does not detect duplicates within the same file
  File: I18nAudit.java:100-116
  Problem: firstFile is keyed by key only (not by filename). If the same key appears twice
           in file1.csv, the second occurrence overwrites the first but the check only
           triggers if the key appears in a DIFFERENT file.
  Fix: Within each file, use a Set<String> seenInFile and warn on duplicate.

================================================================================
SECTION D — RUN.SH / RUN2.SH (issues, bugs, blunders)
================================================================================

[RUN-01] run.sh: javac missing I18nAudit.java — (same as UI-01)
  File: run.sh:60
  Problem: See [UI-01]. Both run.sh compile and run2.sh compile need to include
           I18nAudit.java.

[RUN-02] run.sh + run2.sh: hard-coded jar list includes javalin + kotlin-stdlib
  File: run.sh:29, run2.sh:8
  Problem: The app does NOT use javalin or kotlin-stdlib (no web server, no Javalin routes).
           On a clean checkout or minimal install without those jars, both scripts abort
           even though the jars are unnecessary for the GUI tests.
  Fix: Remove javalin-6.3.0.jar and kotlin-stdlib-2.0.21.jar from the required jar list.
       (Note: the Makefile already dropped these — scripts should match).

[RUN-03] run2.sh: setsid + process-group kill not portable (macOS/BSD)
  File: run2.sh:57
  Problem: setsid creates a new process group. The cleanup trap uses:
             kill -- -"$JAVA_PID"  # kill entire process group
           This is Linux-specific. On macOS/BSD, setsid may not exist.
  Fix: Document Linux-only in a comment, or use portable timeout.

[RUN-04] run.sh: no headless guard — Xvfb started but not checked for success
  File: run.sh:47-56
  Problem: Xvfb is started without checking if it actually started successfully. If Xvfb
           is not installed or fails to start, the script proceeds to run UIAudit which
           will throw AWTException (no display).
  Fix: After starting Xvfb, verify it succeeded.

[RUN-05] run2.sh: watchdog can fire even when StressTest finished successfully
  File: run2.sh:60-65
  Problem: If TIMEOUT=600s and StressTest finishes at 580s, the watchdog sleep (600s)
           fires at exactly the same time as the wait returns. The kill of WATCHDOG_PID
           may race.
  Fix: Set a flag when StressTest exits cleanly.

================================================================================
SECTION E — CROSS-FILE ISSUES
================================================================================

[X-01] ~400 lines of duplicated helper code across UIAudit.java and StressTest.java
  Files: UIAudit.java (lines 763-849, 889-901, 903-914) and StressTest.java (1374-1550)
  Problem: Methods findBtnIn, findComponent, findTextFieldNear, flattenVisible,
           collectComponents, getTopDialog, waitForMainFrame, waitForDialog, log,
           sleep, norm, screenshot, doClick, clickComponent, findCheckBox, findCheckBoxGlobal
           are all nearly identical copies in both files. Every bug fix in one must be
           manually propagated to the other.
  Fix: Extract a shared checkBiblio.UiTestSupport class.

[X-02] Both files call new Robot() independently — two instances, double screen delay
  Files: StressTest.java:46, UIAudit.java:74
  Problem: If both tools run in the same JVM, each creates its own Robot with
           autoDelay=50 (StressTest) and autoDelay=80 (UIAudit). The last one to
           set autoDelay wins for its instance, but both robots affect the same screen.
  Fix: Shared Robot singleton in UiTestSupport (see [X-01]).

[X-03] UIAudit.runAutomated + StressTest.runAllTests both call ControladorDomini.resetForTest()
  Files: UIAudit.java (implicit via app launch), StressTest.java:41-42
  Problem: Both reset the singletons before launching the app. However,
           UIAudit in INTERACTIVE mode does NOT reset before launching.
  Fix: Ensure UIAudit.resetForTest() is called before app launch in ALL modes.

================================================================================
SECTION F — IMPROVEMENTS TO CATCH UNKNOWN ERRORS
================================================================================

[IMP-01] Add API smoke test (highest-value gap)
  Problem: checkBiblio tests only cover Swing UI. The --web mode and all /api/* routes
           (LlibreRouter, LoanRouter, TagRouter, LlistaRouter, MetaRouter) are completely
           untested by the audit harness.
  Fix: Create checkBiblio/ApiSmokeTest.java that exercises all routes.

[IMP-02] Add DB migration audit — verify schema matches what the code expects
  Problem: If a developer adds a column to the schema (ServerConect migration) but
           forgets to update the Java model (Llibre, Prestec, etc.), the app compiles
           fine but fails at runtime with SQL errors.
  Fix: Add checkBiblio/SchemaAudit.java that compares migrations vs actual schema.

[IMP-03] Add DB constraint audit — UNIQUE, NOT NULL, FK constraints enforced
  Problem: The DB schema may have constraints that the Java code doesn't verify
           before inserting, leading to SQL errors at runtime.
  Fix: Add checkBiblio/ConstraintAudit.java that tests constraint enforcement.

[IMP-04] Add CSV/JSON import round-trip test with fixture files
  Problem: Export works (testExport) but import is not tested. If import has a bug,
           data cannot be restored from a backup.
  Fix: Create checkBiblio/fixtures/ with test files and add phase in StressTest.

[IMP-05] Add memory leak detection — monitor component count after dialog spam
  Problem: testExtreme_dialogSpam opens and closes 12 dialogs rapidly.
           If any listener or component is not properly dereferenced, memory grows.
  Fix: In testExtreme_dialogSpam, before and after the loop measure memory and
       component count.

[IMP-06] StressTest: add ISBN collision detection during test creation
  Problem: [ST-01] can cause a "valid create" to fail spuriously as a duplicate.
  Fix: Before each create, assert the ISBN is not in createdISBNs.

[IMP-07] UIAudit: add interactive mode "verify" command to check app state
  Problem: In interactive mode, there's no way to check if a book was actually created
           without manually inspecting the table.
  Fix: Add UIAudit commands: verify ISBN <isbn>, verify count <n>, verify field <row> <col> <value>

[IMP-08] I18nAudit: detect hardcoded strings containing I18n keys in Java code
  Problem: Currently auditHardcodedCatalan only catches common Catalan words. It misses
           hardcoded strings that aren't in the Catalan word list.
  Fix: Add a pass that finds string literals in Java files.

[IMP-09] StressTest: add randomized chaos mode — inject random UI events
  Problem: All current chaos tests are deterministic. Real bugs often appear with
           unexpected combinations.
  Fix: Add phase "49" (extreme mode only): 50 random actions, random strings, random delays.

[IMP-10] UIAudit + StressTest: unify screenshot naming to avoid collisions
  Problem: UIAudit uses "screen_<seq>_<name>.png" StressTest uses "stress_<seq>_<name>.png".
  Fix: Extract screenshot() to UiTestSupport with unified naming.

================================================================================
SECTION G — ALREADY-FIXED (confirmation needed)
================================================================================

The previous review (todo2.txt) marked these as FIXED. Verify they are actually fixed:

✓ [UI-01] + run.sh: I18nAudit compiled with UIAudit — VERIFY: run.sh line 60 still
          only shows UIAudit.java. Either the fix was reverted or it was never applied.
          → STILL BROKEN. Needs fix.

✓ run.sh: dropped javalin/kotlin jars from required list — VERIFY: run.sh line 29
          STILL shows javalin and kotlin in the list.
          → STILL BROKEN. Needs fix.

================================================================================
SUMMARY PRIORITY
================================================================================

CRITICAL (fix immediately):
  [ST-04]  testChaos_longTitle cannot fail (known HIGH, still broken)
  [ST-02]  dismissAllDialogs auto-clicking "Sí" on delete confirmations
  [UI-01]  run.sh compile missing I18nAudit.java → runtime crash
  [UI-02]  Interactive mode mutates real user DB (data loss risk)
  [RUN-02]  run.sh/run2.sh require unused javalin/kotlin jars

HIGH (fix soon):
  [ST-01]  ISBN collision across runs
  [ST-05]  negativePrice/badYear only warn on silent accept
  [ST-07]  no headless guard (AWTException on headless hosts)
  [ST-03]  testExtreme_concurrent reads component tree off EDT
  [X-01]   ~400 lines duplicated between UIAudit and StressTest
  [IMP-01]  No API smoke test — entire /api/* layer untested
  [IMP-04]  No import round-trip test

MEDIUM:
  [ST-06]  screenshot every phase (performance)
  [ST-09]  testCleanup fragile delete (keyboard vs proper click)
  [ST-11]  testLlistesManagement same Sí-click problem
  [UI-08]  testCreateBook / testEditBook no save verification
  [I18N-01] CSV parser broken on commas in field values
  [I18N-02] auditHardcodedCatalan reports only first match per file
  [RUN-03]  run2.sh setsid/process-group kill Linux-only

LOW:
  [UI-04]  waitForDialog dead code
  [UI-06]  cmdRows no exception handling
  [UI-07]  cmdOpenRow fallback Enter may not trigger dialog
  [ST-13]  log() interleaving (synchronized fix needed)
  [RUN-04]  Xvfb not checked for startup success
  [RUN-05]  watchdog race condition
  [I18N-04] duplicate keys within same file not detected

TOTAL: 9 CRITICAL, 10 HIGH, 11 MEDIUM, 11 LOW = 41 issues
================================================================================
END OF REVIEW
================================================================================