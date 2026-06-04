# Biblioteca — Session 4 Triage Plan

## Session 2 & 3 — Already Done

**Session 2:** LlibreRouter 404/500 fixes, ApiServer classify mapping, OpenLibraryClient error handling, TagDao whitelist, dead code deletion (16 files), Makefile classpath, Llista.setColor / isValidColor, Llibre.setAny guard, LlibreDao.executeSQLFile quote-split, FiltreUtils.matches single predicate, 17 tests passing.

**Session 3:** OpenLibraryClient constants, DialogoError IS_HEADLESS caching, DialogoError ⚠→UIManager icon, install.sh Java check/-Xmx, ModeSelectorDialog X button, BookExporter HTML I18n, ShelfParser extraction, BookExporter.exportCSV delegation, PDF dead wrappers deleted, UITheme rebuildFonts EDT assertion, HttpRouter duplicate commit removed, BackupService EOF char, GoodreadsCsvStrategy Private Notes, AutoCompletion dead code, I18n export keys added, 381 tests passing.

**Not yet started** — all other items across the 41 remaining issues in the briefs.

---

## Phased Order

### Phase 1 — ISBN/long overflow (BLOCKER)
**Owns:** `agent-brief 02` | **Severity:** CRITICAL
Blocks: All API shelf-membership endpoints (`/api/llibres/{isbn}/llistes`), all DAO `LlibreLlistaContext` consumers.

| File | Line | Fix |
|------|------|-----|
| `src/domini/LlibreLlistaContext.java` | 11 | `isbn` field `int` → `long` |
| `src/domini/LlibreLlistaContext.java` | 19 | `of(int isbn)` → `of(long isbn)` |
| `src/persistencia/LlibreDao.java` | 208 | Remove `(int)` cast; `isbn` already long |
| `src/persistencia/LlibreDao.java` | 295 | Empty `nom` filter: add `!f.getNom().isEmpty()` check |

---

### Phase 2 — ProgressBarRenderer + CoverCellRenderer (CRITICAL)
**Owns:** `agent-brief 03` | **Severity:** CRITICAL
Blocks: Rendering sanity; shared understanding with Phase 3 (EDT work).

| File | Line | Fix |
|------|------|-----|
| `src/presentacio/renderers/ProgressBarRenderer.java` | 25 | Pass `Llibre` via model; never call `getLlibreIsbn` (DB) in paint |
| `src/presentacio/renderers/ProgressBarRenderer.java` | 24 | Use model index, not hardcoded column, for `t.getValueAt` |
| `src/presentacio/renderers/CoverCellRenderer.java` | 57 | Cache sentinel for cover misses (null→sentinel marker) |
| `src/presentacio/renderers/CoverCellRenderer.java` | 41 | Resolve view↔model column index properly |
| `src/herramienta/CoverService.java` | 27 | Synchronize L1 LinkedHashMap or switch to `ConcurrentHashMap` |

---

### Phase 3 — EDT violations: Import/Export/Backup/Filter (HIGH)
**Owns:** `agent-brief 03` | **Severity:** HIGH

| File | Line | Fix |
|------|------|-----|
| `src/presentacio/ImportController.java` | 35, 62, 76 | Wrap CSV/Calibre/JSON import in `SwingWorker` with progress |
| `src/presentacio/ExportController.java` | 35, 45, 60, 67 | Wrap all exports in `SwingWorker` |
| `src/presentacio/BackupController.java` | 36, 53 | Wrap backup/restore in `SwingWorker` |
| `src/presentacio/FilterController.java` | 131 | Move `aplicarFiltres` DB call off EDT |
| `src/presentacio/detalles/control/DetallesLlibrePanelControl.java` | 157 | Remove blocking `get()` on EDT; use `invokeLater` callback |
| `src/presentacio/detalles/control/DetallesLlibrePanelControl.java` | 57, 86 | DB autocomplete / heavy-field load off EDT |

---

### Phase 4 — API HttpCtx empty-body + unauth'd endpoints (HIGH)
**Owns:** `agent-brief 04` | **Severity:** HIGH

| File | Line | Fix |
|------|------|-----|
| `src/api/HttpCtx.java` | 113 | Pass `-1` for empty bodies; always close exchange/stream |
| `src/api/BackupRouter.java` | 50 | `/api/restore`: add auth check or CSRF token |
| `src/api/BackupRouter.java` | 63 | `/api/clear`: add auth check or CSRF token |
| `src/api/HttpCtx.java` | 74 | Cap `bodyBytes` size; enforce `Content-Length` limit |

---

### Phase 5 — BookImporter CSV (RFC4180) + ISBN-10 normalization (HIGH)
**Owns:** `agent-brief 05` | **Severity:** HIGH

| File | Line | Fix |
|------|------|-----|
| `src/herramienta/BookImporter.java` | 32 | Stream rows through `Rfc4180Reader` instead of `readLine()` |
| `src/herramienta/BookImporter.java` | 64 | Pass `StandardCharsets.UTF_8` to sqlite3 InputStreamReader |
| `src/herramienta/Isbn13Normalizer.java` | 17 | Convert every valid ISBN-10 to 13, not only leading `'0'` |
| `src/herramienta/csv/CsvUtils.java` | 84 | Convert all 10-digit ISBNs (not just `checkDigit=='X'`) |

---

### Phase 6 — `isCellEditable` dead checkbox + column-index view/model (HIGH)
**Owns:** `agent-brief 03` | **Severity:** HIGH (UX regression)

| File | Line | Fix |
|------|------|-----|
| `src/presentacio/BibliotecaTableModel.java` | 57 | Return `true` for `COL_LLEGIT` column |
| `src/presentacio/renderers/ProgressBarRenderer.java` | 24 | `convertColumnIndexToModel` for ISBN column |

---

### Phase 7 — OpenLibraryClient: JSON error + connection leaks (MEDIUM)
**Owns:** `agent-brief 05` | **Severity:** MEDIUM

| File | Line | Fix |
|------|------|-----|
| `src/herramienta/OpenLibraryClient.java` | 55 | Catch `JsonSyntaxException`/`IllegalStateException` → error entry |
| `src/herramienta/OpenLibraryClient.java` | 158, 226 | Drain error stream + `disconnect()` in `finally` |

---

### Phase 8 — Domain: null-safety, ConcurrentModification, SQL injection guard (MEDIUM)
**Owns:** `agent-brief 02` | **Severity:** MEDIUM

| File | Line | Fix |
|------|------|-----|
| `src/domini/ControladorDomini.java` | 172 | `null` ISBN → `BibliotecaException.Validation`, not `NotFound` |
| `src/domini/ControladorDomini.java` | 210, 382 | Use `Objects.equals` for `Long` comparison; guard unboxing |
| `src/domini/ControladorDomini.java` | 246, 390 | `getAllLlistes()`/`getAllTags()`: return copy or unmodifiable view |
| `src/persistencia/TagDao.java` | 56, 124 | `invalidateLlibreTagCache()` on tag delete; centralize on any delete |
| `src/persistencia/ControladorPersistencia.java` | 76 | Synchronize `resetForTest()` on class monitor |

---

### Phase 9 — Config atomic write + CoverService SAVE_SCHEDULER shutdown (MEDIUM)
**Owns:** `agent-brief 05` | **Severity:** MEDIUM

| File | Line | Fix |
|------|------|-----|
| `src/herramienta/Config.java` | 319 | Write to temp file + `Files.move(REPLACE_EXISTING)` atomic swap |
| `src/herramienta/Config.java` | 287–292 | Register `SAVE_SCHEDULER` with shutdown hook |
| `src/herramienta/LlibreValidator.java` | 67 | `countDig` handle `Long.MAX_VALUE` / negative values |
| `src/herramienta/LlibreValidator.java` | 79 | Reject year `0` as invalid |
| `src/herramienta/BookExporter.java` | 221 | `any=0` → `""` not `"0"` |

---

### Phase 10 — checkBiblio QA fixes (CRITICAL/HIGH)
**Owns:** `agent-brief 07` | **Severity:** CRITICAL + HIGH

| File | Line | Fix |
|------|------|-----|
| `run.sh` | 60 | Add `I18nAudit.java` to javac command |
| `run.sh` | 29 | Remove `javalin-*.jar` and `kotlin-stdlib-*.jar` from required list |
| `StressTest.java` | 46 | Add `GraphicsEnvironment.isHeadless()` guard before `new Robot()` |
| `StressTest.java` | 1313–1322 | Remove `"Sí"`, `"Yes"`, `"OK"` from generic `dismissAllDialogs` |
| `StressTest.java` | 231–273 | `testValidation_negativePrice`/`badYear`: `fail()` not `warn()` on silent accept |
| `StressTest.java` | 321–339 | `testChaos_longTitle`: assert single deterministic contract |
| `UIAudit.java` | 64 | Always set `biblioteca.h2.url` in-memory even in INTERACTIVE mode |

---

## Overlaps Flagged

| Overlap | Files | Resolved by |
|---------|-------|-------------|
| `LlibreLlistaContext.isbn` int→long | 02 + 04 share this type | **Brief 02** fixes the domain type; brief 04 adapts routers |
| `CoverService` unsync L1 cache | 02 (threading) + 03 (renderer calls) + 05 (service itself) | **Brief 05** fixes the cache; brief 03 stops calling it from renderers |
| `HttpCtx` empty-body bug | 04 (fix) + 04 (every router uses it) | **Brief 04** only |
| ISBN-10 normalization | 02 (`LlibreDao` search) + 05 (`CsvUtils`, `Isbn13Normalizer`) | **Brief 05** normalizes at import boundary; brief 02 then works with clean ISBNs |
| `Config` atomic write | 01 notes it; 05 owns `Config.java` | **Brief 05** |

---

## Parallel Checklist

```
=== AGENT 02 (Domain + DB) ===
Files: src/domini/LlibreLlistaContext.java, src/persistencia/LlibreDao.java, src/persistencia/TagDao.java, src/persistencia/ControladorPersistencia.java, src/domini/ControladorDomini.java

Phase 1 (CRITICAL): Fix ISBN int→long overflow:
- LlibreLlistaContext.java:11 — isbn field int→long
- LlibreLlistaContext.java:19 — of() param int→long
- LlibreDao.java:208 — remove (int) cast
- LlibreDao.java:295 — empty nom filter !isEmpty() guard

Phase 8 (MEDIUM):
- ControladorDomini.java:172 — null ISBN → Validation exception
- ControladorDomini.java:210,382 — Objects.equals null guard
- ControladorDomini.java:246,390 — return copy/unmodifiable
- TagDao.java:56,124 — invalidateLlibreTagCache() on delete
- ControladorPersistencia.java:76 — synchronize resetForTest

Run: make test
```

```
=== AGENT 03 (Swing UI) ===
Files: src/presentacio/renderers/ProgressBarRenderer.java, src/presentacio/renderers/CoverCellRenderer.java, src/presentacio/BibliotecaTableModel.java, src/presentacio/ImportController.java, src/presentacio/ExportController.java, src/presentacio/BackupController.java, src/presentacio/FilterController.java, src/presentacio/detalles/control/DetallesLlibrePanelControl.java

Phase 2 (CRITICAL):
- ProgressBarRenderer.java:25 — pass Llibre via model, no DB in paint
- CoverCellRenderer.java:57 — cache sentinel for misses
- CoverCellRenderer.java:41 — view↔model column index

Phase 3 (HIGH):
- ImportController.java:35,62,76 — SwingWorker for all imports
- ExportController.java:35,45,60,67 — SwingWorker for all exports
- BackupController.java:36,53 — SwingWorker for backup/restore
- FilterController.java:131 — SwingWorker for filtrar
- DetallesLlibrePanelControl.java:157 — remove blocking get() on EDT
- DetallesLlibrePanelControl.java:57,86 — DB calls off EDT

Phase 6 (HIGH):
- BibliotecaTableModel.java:57 — return true for COL_LLEGIT

Run: make test
```

```
=== AGENT 04 (HTTP API) ===
Files: src/api/HttpCtx.java, src/api/BackupRouter.java, src/api/LlibreRouter.java, src/api/ImportExportRouter.java

Phase 4 (HIGH):
- HttpCtx.java:113 — pass -1 for empty bodies, always close exchange
- HttpCtx.java:74 — cap bodyBytes size / Content-Length
- BackupRouter.java:50 — /api/restore auth/CSRF check
- BackupRouter.java:63 — /api/clear auth/CSRF check
- LlibreRouter.java:183 — NumberFormatException → 400 Bad Request
- ImportExportRouter.java:115 — shut down pool with try-finally

Phase 1 fix needed first from 02: LlibreLlistaContext.isbn long affects router path params

Run: make test
```

```
=== AGENT 05 (Import/Export/Tools) ===
Files: src/herramienta/BookImporter.java, src/herramienta/Isbn13Normalizer.java, src/herramienta/csv/CsvUtils.java, src/herramienta/CoverService.java, src/herramienta/OpenLibraryClient.java, src/herramienta/Config.java, src/herramienta/LlibreValidator.java, src/herramienta/BookExporter.java

Phase 5 (HIGH):
- BookImporter.java:32 — Rfc4180Reader instead of readLine()
- BookImporter.java:64 — StandardCharsets.UTF_8 for sqlite3
- Isbn13Normalizer.java:17 — convert all ISBN-10, not just leading 0
- CsvUtils.java:84 — convert all 10-digit ISBNs to 13

Phase 2 (shared): CoverService L1 unsync:
- CoverService.java:27 — synchronize or use ConcurrentHashMap

Phase 9 (MEDIUM):
- Config.java:319 — atomic write via temp+move
- Config.java:287-292 — shutdown hook for SAVE_SCHEDULER
- LlibreValidator.java:67 — countDig edge cases
- LlibreValidator.java:79 — reject year 0
- BookExporter.java:221 — any=0 → ""

Phase 7 (MEDIUM):
- OpenLibraryClient.java:55 — catch JsonSyntaxException
- OpenLibraryClient.java:158,226 — drain error stream + disconnect

Run: make test
```

```
=== AGENT 07 (checkBiblio QA) ===
Files: run.sh, run2.sh, StressTest.java, UIAudit.java, I18nAudit.java

CRITICAL (fix first):
- run.sh:60 — add I18nAudit.java to javac command
- run.sh:29 — remove javalin/kotlin jars from required list
- StressTest.java:46 — headless guard before new Robot()
- StressTest.java:1313-1322 — remove "Sí"/"Yes"/"OK" from generic dismiss
- StressTest.java:231-273 — fail() not warn() on silent bad-value accept
- StressTest.java:321-339 — testChaos_longTitle single deterministic assertion
- UIAudit.java:64 — always set biblioteca.h2.url in-memory in INTERACTIVE mode

Run: make test  (then verify checkBiblio compiles with run.sh)
```
