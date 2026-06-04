# Plan: Remove web mode — Java Swing only

**Goal:** Drop the browser UI (`src/web/`), the local HTTP API (`src/api/`), and all startup/routing that launches web mode. The app should always start as the Swing desktop application via `main.Ejecutable`.

**Scope:** Planning only. Do not delete anything until you explicitly approve each batch.

**Verify after implementation:**

```bash
make compile && make test
# or: mvn -q compile test
make run   # should open Swing directly — no mode picker, no HTTP server on port 7070
```

---

## Summary

| Category | Count |
|----------|------:|
| Directories to remove | 2 (`src/api/`, `src/web/`) |
| Java files to delete | 3–4 (see § ConfigDTO decision) |
| Frontend files to delete | 14 |
| Java files to edit | 4–5 |
| Config / i18n / build edits | 6+ |
| Docs / scripts (optional cleanup) | several |

---

## Reconciled decisions (differences between source plans)

Two drafts existed: `PLAN-REMOVE-WEB.md` (repo root) and this file. They agreed on almost everything; these points differed and are resolved here:

| Topic | `PLAN-REMOVE-WEB.md` | `plans/remove-web-mode.md` (original) | **Merged decision** |
|-------|----------------------|----------------------------------------|---------------------|
| **`ConfigDTO.java`** | Optional **delete** (API-only record, zero Swing refs today) | **Keep**; edit Javadoc only | **Keep + edit Javadoc.** Grep shows no callers outside the file itself, but the record mirrors `Config` and is useful for a future Swing settings dialog. Delete in a follow-up only if still unused after settings work. |
| **`ModeButton.java`** | "Helper used only by `ModeSelectorDialog`" | "Dead helper — `ModeSelectorDialog` uses inline `buildModeButton`" | **Delete.** Class exists but is unused; dialog builds buttons inline. |
| **`Ejecutable` `--web` arg** | Drop arg parsing; `--web` silently ignored | Exit 1 with clear stderr message | **Reject `--web` explicitly** (stderr + `System.exit(1)`). Safer for scripts and users who expect web mode. |
| **`Ejecutable` `--swing` arg** | Optional no-op for backward compat | Same | **Keep `--swing` as accepted no-op** for launch scripts. |
| **String CSV cleanup** | Required step in main flow | Marked "optional cleanup" | **Required** — remove dead keys so `gen_strings.py` does not regenerate orphan entries in `I18n.java` / properties. |
| **`SwingLauncher.java`** | Mentioned in "Keep" table only | Dedicated section (Option A/B) | **Keep as thin `--swing` wrapper**; no functional change once `Ejecutable` is Swing-only. |
| **HTTP stack note** | Listed Javalin in `pom.xml` removal | Explicit: API uses JDK `HttpServer`, not Javalin | **JDK `com.sun.net.httpserver.HttpServer`** — Javalin/kotlin-stdlib in `pom.xml` were never imported. |
| **`src/web/` file count** | 14 files | Checklist said 15 | **14 source files** (15 glob hits were a path duplicate). |
| **Scripts** | `loop-healthcheck.ps1`, `split_agent_briefs.py`, `.loop-lib` | Not listed | **Include** — remove web smoke blocks from healthcheck and API brief generation. |
| **Agent briefs** | Generic trim list | Also `07-checkbiblio-qa.md`, `run-web.sh` in `tot.txt` | **Include all** listed brief/tot.txt cleanups. |
| **Generated artifacts** | `target/classes/web/`, `WebLauncher.class` | `lib/javalin-*.jar`, `lib/kotlin-stdlib-*.jar` on disk | **Merge both lists** — clean Maven output, IDE copies, and orphaned lib JARs if present. |
| **False-positive keep list** | § "What stays unchanged" (layers) | Named files (`OpenLibraryClient`, etc.) | **Both** — see § What stays unchanged and § Explicitly not deleted. |
| **Manual smoke test** | Not in verify step | Explicit post-test checklist item | **Include** — launch app, add/edit book, import/export, config dialog. |

> **Canonical plan:** this file. Root `PLAN-REMOVE-WEB.md` redirects here.

---

## Phase 1 — Delete entire directories

Ask before deleting each tree.

### 1.1 Delete `src/web/` (browser frontend — 14 files)

| Path | Role |
|------|------|
| `index.html` | SPA shell |
| `css/main.css` | Main layout |
| `css/modal.css` | Modals |
| `css/gallery.css` | Cover gallery |
| `css/sidebar.css` | Sidebar |
| `css/table.css` | Book table |
| `js/api.js` | Fetch wrapper for REST API |
| `js/main.js` | App bootstrap |
| `js/store.js` | Client state |
| `js/books.js` | Book list UI |
| `js/bookDetail.js` | Book detail panel |
| `js/shelves.js` | Shelves UI |
| `js/tags.js` | Tags UI |
| `js/i18n.js` | Web i18n (generated from CSV) |

> **Note:** `default-cover.png` is referenced at `/web/img/default-cover.png` but exists only under `.vscode-java-bin/web/img/` (IDE copy), not in `src/web/`. No Swing code uses it.

### 1.2 Delete `src/api/` (HTTP server + REST routers — 14 Java files)

Uses **`com.sun.net.httpserver.HttpServer`** (JDK built-in). **Not** Javalin — those Maven deps were declared but never imported.

| File | Role |
|------|------|
| `ApiAuth.java` | Bearer token for mutating API calls |
| `ApiServer.java` | Server bootstrap, exception → HTTP status mapping |
| `BackupRouter.java` | `/api/backup`, `/api/restore`, `/api/clear` |
| `ConfigRouter.java` | `/api/config`, `/api/config/ui`, `/api/config/db` |
| `HttpCtx.java` | Request/response wrapper around `HttpExchange` |
| `HttpRouter.java` | JDK `HttpServer`, routing, CORS, static `/web/` serving |
| `ImportExportRouter.java` | JSON/CSV import-export, cover fetch (`/api/export/*`, `/api/import/*`, `/api/fetch-covers`) |
| `JsonMapper.java` | Gson DTO mapping (API-only) |
| `LlibreRouter.java` | `/api/books/*` |
| `LlistaRouter.java` | `/api/shelves/*` |
| `LoanRouter.java` | `/api/loans/*` |
| `MetaRouter.java` | `/api/meta/*` |
| `OpenLibraryRouter.java` | `/api/openlibrary/*` (local proxy; distinct from Swing's external Open Library client) |
| `TagRouter.java` | `/api/tags/*` |

### 1.3 Delete web-only Java entry / UI (3 files)

| File | Why |
|------|-----|
| `src/main/WebLauncher.java` | Entry point for `--web`; delegates to `Ejecutable` |
| `src/presentacio/ModeSelectorDialog.java` | Startup dialog: Web vs Desktop |
| `src/presentacio/ModeButton.java` | Unused; `ModeSelectorDialog` builds buttons via inline `buildModeButton()` |

**Keep (do not delete):**

| File | Why |
|------|-----|
| `src/main/SwingLauncher.java` | Thin `--swing` entry; still useful for scripts/tests |
| `src/main/ShutdownHooks.java` | Generic utility; update comment only |
| `src/herramienta/ConfigDTO.java` | Keep; edit Javadoc (see reconciled decisions) |

---

## Phase 2 — Edit existing Java files

### 2.1 `src/main/Ejecutable.java` — main simplification

**Remove:**

- `import api.ApiServer`
- `import presentacio.ModeSelectorDialog`
- `import java.awt.Desktop`, `java.net.URI` (browser open) if only used by web
- `webMode` flag and web branch in uncaught-exception handler
- `resolveMode()` (or reduce to: test → swing, optional `--swing` arg)
- `startWeb()` method
- All `Config.setLastMode` / `Config.getLastMode` usage

**Keep / simplify:**

- `main()` → shutdown hook → `startSwingWithSplash()` directly
- Accept `--swing` as no-op for backward-compatible launch scripts
- Reject `--web` with a clear error (do not silently ignore)

**Target shape:**

```java
public static void main(String[] args) throws Exception {
    if (args.length > 0 && "--web".equals(args[0])) {
        System.err.println("Web mode was removed. Run the Swing desktop app instead.");
        System.exit(1);
    }
    // optional: if ("--swing".equals(args[0])) { /* no-op */ }
    // uncaught handler → DialogoError (Swing only)
    Runtime.getRuntime().addShutdownHook(...);
    startSwingWithSplash();
}
```

### 2.2 `src/main/SwingLauncher.java`

**Option A (recommended, minimal):** Keep as thin `--swing` wrapper — still valid after `Ejecutable` always starts Swing.

**Option B:** Extract shared startup into a private method if you want one code path for both entry points.

No functional change required if `Ejecutable` always starts Swing.

### 2.3 `src/main/ShutdownHooks.java`

- Change comment from *"shared by web and swing modes"* → *"application shutdown hooks"* (or *"shared shutdown hooks"*).

### 2.4 `src/herramienta/Config.java`

**Remove methods and any related comments:**

| Method | Config key | Purpose (web-only) |
|--------|------------|-------------------|
| `getApiPort()` / `setApiPort()` | `apiPort` | HTTP listen port (default 7070) |
| `getApiToken()` | `apiToken` | CSRF/auth token for API |
| `getLastMode()` / `setLastMode()` | `lastMode` | Remember web vs swing |

Existing user files at `~/.biblioteca/config.properties` may still contain these keys; they become inert (no need to migrate; optional manual cleanup).

### 2.5 `src/herramienta/ConfigDTO.java`

**Keep the class.**

**Edit Javadoc:** Remove reference to `{@code /api/config}` — describe Swing settings dialog only.

### 2.6 `src/herramienta/JsonImporter.java` (comment only)

- Line ~22 references `api.ImportExportRouter#importJson`. Update Javadoc to name the Swing import path instead (e.g. `ImportController` or `BookImporter`).

---

## Phase 3 — Build, dependencies, strings

### 3.1 `pom.xml`

**Remove unused dependencies** (never imported in Java sources):

```xml
<!-- DELETE -->
<dependency>
  <groupId>io.javalin</groupId>
  <artifactId>javalin</artifactId>
  <version>6.3.0</version>
</dependency>
<dependency>
  <groupId>org.jetbrains.kotlin</groupId>
  <artifactId>kotlin-stdlib</artifactId>
  <version>2.0.21</version>
</dependency>
```

**Keep:**

| Dependency | Still used by |
|------------|---------------|
| `gson` | `BookExporter`, `JsonImporter`, `OpenLibraryClient`, `OpenLibraryParser`, Swing import/export |
| `h2`, `mariadb-java-client` | Persistence |

No other `pom.xml` changes required (`mainClass` stays `main.Ejecutable`).

If `lib/javalin-*.jar` or `lib/kotlin-stdlib-*.jar` exist on disk (copied manually), remove them when cleaning dependencies.

### 3.2 CSV sources — remove ModeSelector block

Delete these keys (and their section comment) from:

- `strings/ui.csv`
- `strings/strings.csv` (legacy monolith, if still used)
- `strings/csv.csv` / `strings/errors.csv` (section headers only, if present)

Keys to remove:

| Key | CA example |
|-----|------------|
| `dlg_mode_selector_title` | Biblioteca — Tria el mode d'inici |
| `lbl_mode_selector_subtitle` | Com vols obrir l'aplicació? |
| `mode_web` | Web |
| `mode_web_sub` | Obre al navegator |
| `mode_desktop` | Escriptori |
| `mode_desktop_sub` | Finestra Java nativa |

### 3.3 `scripts/gen_strings.py`

**Edit:** stop generating web i18n.

- Remove `JS_PATH` constant and `gen_js()` function (if present)
- Remove `JS_PATH` write block in `main()`
- Update module docstring (lines 3–8) to list only Java/properties outputs (`I18n.java`, `strings_*.properties`)

### 3.4 Regenerate Swing strings

After editing CSVs and `gen_strings.py`, run:

```bash
python scripts/gen_strings.py
```

Confirm removal of the six keys above from:

- `src/herramienta/I18n.java`
- `src/herramienta/strings_ca.properties`
- `src/herramienta/strings_es.properties`
- `src/herramienta/strings_en.properties`

(`src/web/js/i18n.js` will be gone once `src/web/` is deleted.)

---

## Phase 4 — Scripts and tooling (optional but recommended)

| File | Change |
|------|--------|
| `scripts/loop-healthcheck.ps1` | Remove `WebLauncher` / `--web` smoke block (~lines 38–49) |
| `scripts/split_agent_briefs.py` | Remove or shrink `api` agent routing; drop `04-api-http.md` from index generation |
| `.loop-lib/smoke-results.json` | Remove `runweb` / `run2web` entries (generated artifact) |
| `.loop-lib/smoke-runweb*.txt`, `loop-web-err.txt` | Delete if cleaning generated logs |

---

## Phase 5 — Documentation / agent briefs (optional)

Not required for compile/run, but avoids confusion:

| File | Action |
|------|--------|
| `agent-briefs/04-api-http.md` | Delete or archive |
| `agent-briefs/00-INDEX.md` | Remove HTTP API row; drop `src/api/` from table |
| `agent-briefs/PROMPTS.md` | Remove "HTTP API specialist" / `--web` prompt block |
| `agent-briefs/01-coordinator.md` | Trim web/API backlog items (or leave historical) |
| `agent-briefs/03-presentacio-swing.md` | Trim web/API backlog items |
| `agent-briefs/07-checkbiblio-qa.md` | Remove `--web` / API smoke-test backlog items |
| `tot.txt` | Remove web/API sections (`src/api/*`, `--web`, `run-web.sh`) when regenerating briefs |
| `AGENTS.md` | No web references today — no change needed |
| `README.md` | Already Swing-only — no change needed |

---

## Phase 6 — Generated / IDE copies (optional cleanup)

These are not source-of-truth; safe to delete locally after web removal:

| Path | Notes |
|------|-------|
| `.vscode-java-bin/web/` | IDE mirror of frontend (~15 files) |
| `target/classes/api/` | Maven output |
| `target/classes/web/` | Maven output (if copied) |
| `target/classes/main/WebLauncher.class` | Maven output |
| `lib/javalin-*.jar`, `lib/kotlin-stdlib-*.jar` | Orphan JARs if present on disk |

Run `make clean` or `mvn clean` after deletion to refresh `target/`.

---

## What stays unchanged

These layers have **no** imports from `api` or web code:

- `src/domini/` — business logic
- `src/persistencia/` — JDBC
- `src/interficie/` — reader/writer interfaces
- `src/presentacio/` — Swing UI (except files listed for deletion)
- `src/herramienta/` — tools, export/import, Open Library client (uses external openlibrary.org API, not local `/api/`)

Gson remains a runtime dependency for JSON import/export in Swing.

### Explicitly not deleted (common false positives)

| Path | Why keep |
|------|----------|
| `src/herramienta/OpenLibraryClient.java` | Swing book lookup (external API) |
| `src/presentacio/detalles/control/OpenLibrarySearchTask.java` | Swing |
| `src/herramienta/BookExporter.java`, `JsonImporter.java` | Swing import/export |
| `src/interficie/InMemoryBibliotecaReader.java` | Test helper, not web |
| `com.google.gson` dependency | Still used outside deleted API |
| `src/main/SwingLauncher.java` | Desktop alternate entry |
| Domini/persistencia edits on other branches | General app work, not web-specific |

---

## Suggested implementation order

1. **Edit** `Ejecutable.java` → always Swing; compile must fail on missing `api` imports (confirms no hidden callers).
2. **Delete** `src/api/` and `src/web/`.
3. **Delete** `WebLauncher.java`, `ModeSelectorDialog.java`, `ModeButton.java`.
4. **Edit** `Config.java`, `ConfigDTO.java` Javadoc, `ShutdownHooks.java`, `JsonImporter.java` comment.
5. **Edit** `pom.xml` — remove javalin + kotlin-stdlib.
6. **Edit** strings CSVs + `gen_strings.py`; regenerate properties and `I18n.java`.
7. **Optional:** scripts (§ Phase 4), agent briefs (§ Phase 5), `.loop-lib` / generated artifacts (§ Phase 6).
8. **`make compile && make test`**
9. **Manual smoke:** `make run` — add/edit book, import/export, config dialog; confirm nothing listens on port 7070.

---

## Risks and notes

| Risk | Mitigation |
|------|------------|
| User has `lastMode=web` in config | Harmless after removal; app ignores unknown keys |
| Launch scripts pass `--web` | `Ejecutable` exits 1 with clear message; update scripts/docs |
| `SwingLauncher` still passes `--swing` | Keep `--swing` as accepted no-op, or point scripts at `Ejecutable` directly |
| Loss of headless JSON API | Intended; Swing + file import/export remain |
| `ConfigDTO` unused after removal | Keep for future Swing settings refactor; delete in follow-up if grep still shows zero refs |
| Large `tot.txt` / briefs still mention API | Cosmetic; prune in Phase 5 |
| Javalin in `pom.xml` but API used JDK HttpServer | Removing unused deps is safe; no runtime behavior change |

---

## Checklist (copy for PR / task tracking)

```
[ ] Delete src/api/ (14 Java files)
[ ] Delete src/web/ (14 frontend files)
[ ] Delete src/main/WebLauncher.java
[ ] Delete src/presentacio/ModeSelectorDialog.java
[ ] Delete src/presentacio/ModeButton.java
[ ] Simplify src/main/Ejecutable.java (reject --web, Swing-only)
[ ] Trim src/herramienta/Config.java (apiPort, apiToken, lastMode)
[ ] Update src/herramienta/ConfigDTO.java Javadoc (keep class)
[ ] Update src/main/ShutdownHooks.java comment
[ ] Update src/herramienta/JsonImporter.java Javadoc
[ ] Remove javalin + kotlin-stdlib from pom.xml
[ ] Remove mode-selector keys from strings/*.csv
[ ] Update scripts/gen_strings.py (no i18n.js)
[ ] Run python scripts/gen_strings.py
[ ] Optional: loop-healthcheck.ps1, split_agent_briefs.py, agent-briefs, tot.txt
[ ] Optional: .vscode-java-bin/web/, target/classes/api|web/, lib/javalin|kotlin JARs
[ ] make compile && make test
[ ] Manual Swing smoke test (make run)
```

---

*Merged 2026-06-04 from `PLAN-REMOVE-WEB.md` + `plans/remove-web-mode.md` — inventory from repo scan; no files deleted yet.*
