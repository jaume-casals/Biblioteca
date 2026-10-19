# Biblioteca — Architecture & Function Map

Complete map of the codebase: every layer, every package, every class, the data model, the
runtime flows, and the build/test surface.

Every number in this document was **measured**, not estimated. The measurement commands are
given in §7 so the figures can be reproduced or falsified.

> Language note: the code, comments, UI strings and commit messages are Catalan.
> This document is English. Identifiers are quoted verbatim.

---

## 1. What the project is

**Biblioteca** is a **personal library manager for the desktop** — a single-user Swing
application for cataloguing a physical book collection, tracking who you lent books to,
organising them into shelves and tags, and keeping reading progress and ratings.

It is *not* a library-lending system, not multi-user, and not a web app. It runs as one
local Java process, talking to a local file-backed H2 database (or a remote MariaDB if you
switch it), with a Swing GUI on the EDT.

**Public upstream**: <https://github.com/jaume-casals/Biblioteca>

### Feature surface

| Area | Capability |
|---|---|
| Catalogue | Books with title, authors, ISBN-10/13, year, rating 0–10, price, cover, notes |
| Covers | Auto-import from OpenLibrary by ISBN; images stored as **BLOB in the DB**, not as loose files |
| Shelves | Unlimited shelves; each book carries its **own rating and read-state per shelf** |
| Tags | Free-form classification (genre, author, project, …) |
| Translations | Per-title translations (`nomCa` / `nomEs` / `nomEn`) alongside the canonical title |
| Search | Real-time across title, author, ISBN, description **and notes** |
| Filters | Composable: rating, year, language, format, shelf, tag, read-state |
| Loans | Track who you lent a book to, and returns; overdue detection |
| Progress | Pages-read with a progress bar rendered in the table |
| Statistics | Per-shelf averages, read percentages, most-present authors, reading pace |
| Import | CSV (native / Goodreads / LibraryThing dialects), Calibre `metadata.db`, JSON |
| Export | CSV, JSON, HTML, PDF |
| Backup | SQL dump + restore, with an automatic pre-restore safety backup |
| UI | Dark/light theme, 3 font sizes, 3 UI languages, gallery **and** table view modes |
| Scanning | Barcode-scanner "scan station" with an ISBN lookup pipeline |

---

## 2. At a glance

| | |
|---|---|
| Language / runtime | Java 25 (`--release 25`), Swing, Nimbus Look & Feel |
| Build | Plain `Makefile` + `javac` — **no Maven/Gradle for the app itself** |
| Dependency manifest | `build/deps.lock` — versions + SHA-256, single source of truth; `make deps` |
| Main class | `main.Executable` |
| Source | **197** Java files, **22 504** lines under `src/` |
| Tests | **69** files, **13 329** lines under `test/` — 715 tests executed by `make test` (JUnit Jupiter + jqwik) |
| Aux tools | `checkBiblio/` — 4-file, 3 465-line standalone CLI analyser + stress driver |
| Runtime deps | 8 jars (see §6 *Dependencies*), versions pinned in `build/deps.lock` |
| i18n | `strings/{ui,errors,csv}.csv` → `scripts/gen_strings.py` → generated `I18n.java` — **595 keys**, 3 locales |
| Persistence | H2 (file or in-memory) **or** MariaDB, chosen at runtime |
| HTTP | `java.net.http.HttpClient` (JDK built-in) — no HTTP library dependency |
| CI | `.github/workflows/ci.yml` — 6 jobs (build+test, i18n, h2-compat, package, fuzz, deps) |

### Layer metrics

| Layer | Package root | Files | Lines | Role |
|---|---|---:|---:|---|
| Bootstrap | `src/main` | 2 | 200 | Entry point, splash screen, fatal-error handling |
| Domain | `src/domini` | 22 | 2 117 | Entities, filters, sorting, facades — no UI, no SQL |
| Persistence | `src/persistencia` | 38 | 3 143 | Contracts, DAOs, connection, schema, row mappers |
| Shared utils | `src/herramienta` | 52 | 5 610 | Config, i18n, IO, CSV, text, theming, OpenLibrary client |
| Presentation | `src/presentacio` | 83 | 11 434 | Swing panels, controllers, dialogs, renderers, gallery |
| **Total** | `src/` | **197** | **22 504** | |

Line counts are a snapshot of one working-tree state; the class inventory and package
structure are stable, the line counts move as the code changes.

**Dependency direction is strictly downward**: `presentacio → domini → persistencia.contract`,
with `herramienta` usable from every layer. The domain layer never imports Swing or JDBC.

Four packages carry a `package-info.java` documenting the package contract: `domini`,
`herramienta`, `persistencia`, `presentacio`.

### Test execution

`make test` runs a legacy plain-Java suite followed by one JUnit Platform session.

| | |
|---|---|
| Discovery | Class names derived from **source file paths**, so both `FooTest.java` and `TestFoo.java` are picked up — 64 mandatory classes |
| Engines | JUnit Jupiter + jqwik (property tests) |
| Excluded | 5 Jazzer coverage-guided harnesses (need the agent) — `make fuzz-jazzer` |
| Exit code | propagates failures; `--fail-if-no-tests` fails an empty suite |
| Reports | JUnit XML in `build/reports/tests/` |
| Legacy suite | `test/BibliotecaTest.java` runs first as a plain `java` invocation |

**Measured result** (Temurin 25.0.4.1+1, H2 in-memory, `EXIT=0`):

| | |
|---|---:|
| Tests found | 715 |
| Tests started | 714 |
| Tests successful | **694** |
| Tests failed | **0** |
| Tests skipped | 1 |
| Tests aborted | 20 |
| Containers (incl. nested) | 86 |

Source annotations: **634** `@Test`, **17** `@ParameterizedTest`, **6** `@Property`.
Test files by area: `test/persistencia` 18, `test/herramienta` 19, `test/domini` 12,
`test/presentacio` 7, `test/fuzz` 6, `test/` root 7.

---

## 3. Architecture

```
                      ┌──────────────────────────────────────────────┐
                      │  src/main/Executable.java                     │
                      │  splash ∥ theme ∥ DB seed → main window       │
                      └───────────────────┬──────────────────────────┘
                                          │
        ┌─────────────────────────────────▼─────────────────────────────────┐
        │                      src/presentacio  (83 files)                   │
        │  panells · controladors · dialegs · detalles · formularis          │
        │  galeria · layouts · listener · models · renderers · config · util│
        └───────────────────┬─────────────────────────────────┬─────────────┘
                            │ delegates to                   │ reads/writes
        ┌───────────────────▼──────────────┐   ┌──────────────▼──────────────┐
        │      src/domini  (22 files)      │   │  src/persistencia           │
        │  ControladorDomini (facade)     │   │  contract/  dao/  internal/ │
        │  DelegatLlibre · DelegatPrestec  │   │  row/                       │
        │  DelegatPrestatgeria · TagDelegate│  │                            │
        │  StateContext · Llibre · LlibreFilter│                            │
        └───────────────────┬──────────────┘   └──────────────┬──────────────┘
                            │                                  │
                            └──────────────┬───────────────────┘
                                           │
                            ┌──────────────▼──────────────┐
                            │   src/herramienta (52 files)  │
                            │  config · i18n · io · text    │
                            │  api · ui                     │
                            └──────────────────────────────┘
```

### The facade seam

`domini.ControladorDomini` is a singleton facade that hides which persistence backend is
live. It exposes the domain through **narrow role interfaces** so the UI never talks to
SQL and never needs to know if it is H2 or MariaDB:

| Interface | Role | Declared methods |
|---|---|---:|
| `persistencia.contract.LectorLlibre` | Read books, filters, pagination, blobs | 15 |
| `persistencia.contract.EscritorLlibre` | Write books | 5 |
| `persistencia.contract.EscritorBiblioteca` | Aggregate super-interface over all readers + writers | — |
| `persistencia.contract.LectorBiblioteca` | Read the whole aggregate in one shot | 4 |
| `persistencia.contract.AdministradorBiblioteca` | Backup / restore / admin operations | 3 |
| `persistencia.contract.LectorPrestatgeria` / `EscritorPrestatgeria` | Shelves (lists) | 7 / 9 |
| `persistencia.contract.LectorEtiqueta` / `EscritorEtiqueta` | Tags | 4 / 5 |
| `persistencia.contract.LectorPrestec` / `EscritorPrestec` | Loans | 5 / 2 |
| `persistencia.contract.LectorBibliotecaEnMemoria` | In-memory backend (used by tests) | 42 |

`EscritorBiblioteca` is the aggregate: it extends `LectorBiblioteca`,
`AdministradorBiblioteca`, `EscritorLlibre`, `EscritorPrestatgeria`, `EscritorEtiqueta` and
`EscritorPrestec`. `ControladorDomini` implements it directly.

`LectorLlibre.obtenirAllLlibresSummary()` is a *lean* variant that skips heavy text/blob
columns; its default implementation delegates to the full read.

`ControladorDomini` exposes roughly **70 public methods**, all one-line delegations, split
into seven role delegates in `domini/facade/`:

| Delegate | Owns |
|---|---|
| `DelegatLlibre` | Book read/write + filtering + pagination + blob access. Owns `SQL_FILTER_THRESHOLD` (= 2000). |
| `DelegatPrestatgeria` | Shelf CRUD, book↔shelf membership, per-shelf rating/read-state. |
| `TagDelegate` | Tag CRUD and book↔tag membership. |
| `DelegatPrestec` | Loans: lend, return, active loans, overdue loans, loaned-ISBN set. |
| `DelegatEstadistiques` | Statistics aggregation (averages, read %, top authors, pace). |
| `DelegatCopiaSeguretat` | Backup export / restore orchestration. |
| `NamedEntityDelegate` | Shared generic CRUD for name-based entities. |

### Two distinct pieces of shared state — do not conflate them

| | `domini.facade.StateContext` | `presentacio.util.EstatVistaBiblioteca` |
|---|---|---|
| Scope | **Domain-side** | **UI-side** |
| Holds | the in-memory book / shelf / tag lists + id indexes + the persistence reference | current book list, current shelf id, group-by-series flag, loaned-ISBN set, undo buffer |
| Concurrency | one `lock` monitor; all reads/writes go through `withLock` / `withLockReturning`; `snapshotAll()` takes a defensive copy; `replaceAll()` swaps atomically | not thread-safe; EDT-confined |
| Consumers | `domini.facade.*` only | the six sub-controllers of the main library screen |

`StateContext` is package-private to `domini.facade`; `lock()` has no public getter on
purpose, so the only sanctioned way to touch the lists is `withLock(...)`.

---

## 4. Layer detail

### 4.1 `src/main` — bootstrap (2 files)

#### `main.Executable`
The single entry point. `main(String[] args)` does three things:

1. Installs a `Thread.setDefaultUncaughtExceptionHandler` that logs the throwable and
   shows a `DialegError`. Headless-aware: it prints to `stderr` and exits 1 instead of
   trying to show a dialog. The handler accepts a `Throwable` — non-`Exception` throwables
   (`StackOverflowError`, `OutOfMemoryError`) are wrapped in a `RuntimeException` so the
   `DialegError(Throwable)` overload can carry them.
2. Registers `main.ShutdownHooks` → `ControladorPersistencia::reinicialitzarForProfileSwitch`,
   so the DB closes cleanly on profile switch or exit.
3. Calls `iniciarSwingWithSplash()`, which does the rest on the EDT.

`iniciarSwingWithSplash()` sets up the UI and the database **in parallel**, because
opening the DB and installing the Look & Feel do not depend on each other:

```
EDT                                        loader thread ("biblioteca-loader", daemon)
────                                        ───────────────────────────────────────────────
UIManager.setLookAndFeel(NimbusLookAndFeel) SeedorBaseDades.seedSiCal()
UITheme.posarTheme(...)                     ControladorDomini.getInstance()
UITheme.rebuildFonts(...)
UIManager.put(nimbusBase / control / …)
new PantallaInici(); splash.show()
new PanelMarcPrincipal()
                                            EventQueue.invokeLater(→ EDT):
                                              hideSplash()
                                              ControladorMarcPrincipal.getInstance(vista, cd)
                                                                      .setVisible(true)
```

Two ordering details are load-bearing and commented as such in the source:

- **Nimbus is installed explicitly, before any `UIManager.put`.** `UIManager.put`
  initialises the default (Metal) Look & Feel when none is installed; installing Nimbus
  afterwards would re-initialise a second Look & Feel from scratch.
- **`PanelMarcPrincipal` is built while the database is still opening.** It is pure Swing
  and does not depend on the connection, so waiting would buy nothing.

The `UIManager` keys overridden with the palette are `nimbusBase`, `nimbusBlueGrey`,
`control`, `text`, `nimbusFocus`, `nimbusSelectionBackground`, `nimbusSelectedText`,
`defaultFont` and `Table.alternateRowColor`.

`hideSplashSafely()` and `fatalExit(...)` exist specifically so that a background-thread
failure never touches a Swing component off the EDT (which would throw
`IllegalComponentStateException` and mask the real error). `fatalExit` shows a supplied
dialog on the EDT and then exits 1. The normal path uses `hideSplash()`, which respects
the splash's minimum display time.

#### `main.ShutdownHooks`
Registers JVM shutdown hooks for DB teardown.

---

### 4.2 `src/domini` — domain model (22 files)

Package root (14 files):

| Class | Purpose |
|---|---|
| `domini.Llibre` | The book. Core identity (`Long isbn`) plus mutable metadata. Holds the cover as a `byte[]` blob. Tracks `heavyFieldsLoaded` (exposed as `teCampsPesatsCarregats()`) — whether heavy text/blob columns have been loaded — so list views can skip them. Carries per-locale titles `nomCa` / `nomEs` / `nomEn`. Keeps a flat `autor` string even though the DB stores authors in the N:M `llibre_autor` ⋈ `autor` tables — the DAO rebuilds the flat form with `GROUP_CONCAT` on read. |
| `domini.LlibreCore` | Value-object core of a book (identity + the fields needed for list rows), used to avoid loading the full object. |
| `domini.Llista` | A shelf. Has `id`, `nom`, `ordre`, `color` — the order and colour drive the shelf combo swatches. |
| `domini.Tag` | A tag. `id` + `nom`. |
| `domini.EntitatNomenada` | Base for name-based entities (shelf/tag share create-rename-delete semantics). |
| `domini.LlibreLlistaContext` | Per-shelf context for a book: the shelf plus that shelf's per-book rating and read flag. |
| `domini.LlibreFilter` | Filter value object. Fluent `with*(...)` setters mutate in place and return `this`; `obtenir*(...)` getters read back: name, author, ISBN, year min/max, rating min/max, price min/max, editorial, series, language, format, read-state, tag id, list id, sort spec. |
| `domini.ConstructorFiltreLlibre` | Separate fluent builder for `LlibreFilter` — 18 chained methods (`isbn`, `autor`, `nom`, `anyMin`, …, `sort`) ending in `build()`. Used by the tests; production code uses `LlibreFilter.with*(...)` directly. |
| `domini.EspecificacioOrdenacio` | Sort specification — column + ascending/descending, applied in-memory. |
| `domini.AnalitzadorPrestatgeria` | Shelf-matching rules (which books belong to which shelf given a filter). |
| `domini.SqlOp` | SQL fragment builder — composable predicates and ordering for DB-side filtering. |
| `domini.BibliotecaException` | Domain exception hierarchy; `BibliotecaException.NoTrobat` is the "book not found" signal. |
| `domini.package-info` | Package contract documentation. |

`domini/facade/` (8 files): `ControladorDomini` (in `domini/`), `StateContext`,
`DelegatLlibre`, `DelegatPrestatgeria`, `DelegatPrestec`, `TagDelegate`,
`DelegatEstadistiques`, `DelegatCopiaSeguretat`, `NamedEntityDelegate`.

---

### 4.3 `src/persistencia` — persistence (38 files)

#### `persistencia/contract/` (12 interfaces)
The seam. Reader/writer pairs per aggregate, plus `AdministradorBiblioteca` for
backup/restore and `LectorBibliotecaEnMemoria` for the in-memory test backend.

#### `persistencia/dao/` (8 classes)
| DAO | Responsibility |
|---|---|
| `LlibreDaoCore` | Core book CRUD: `obtenirAll`, `insert`, `update`, `delete`, `obtenirRecentlyAdded`, `count`, `obtenirDbSizeBytes`, `netejarAllData`, `executarSQLFile`, `validarCopiaSeguretat` |
| `LlibreBlobDao` | Cover BLOB read/write separate from the row data |
| `LlibreSearchDao` | DB-side search, used by the large-library filter path |
| `LlibreLecturaDao` | Reading-log (`lectura`) table access |
| `LlistaDao` | Shelf CRUD, counts, books-in-shelf, ISBN set for a shelf, per-shelf rating/read writes, order and colour updates |
| `TagDao` | Tag CRUD, book↔tag links, distinct-value queries (e.g. distinct author names) |
| `AutorDao` | Author rows and distinct author names. Since migration 34 this is the **only** store of authors — `llibre.autor` is gone, and the flat author string the domain sees is a `GROUP_CONCAT` over `llibre_autor` ⋈ `autor`. |
| `PrestecDao` | Loans: add, return, active loans, loans-for-ISBN, overdue loans, loaned-ISBN set |

#### `persistencia/row/` (9 records)
`AutorRow`, `LecturaRow`, `LlibreAutorRow`, `LlibreLlistaRow`, `LlibreTagRow`,
`PrestecRow`, `PrestecEndarrerit`, `RelationRow`, `RowDates`.

`RelationRow` is the marker interface implemented by the three N:M link records.
`RowDates` holds the shared `ISO_LOCAL_DATE` / `dd/MM/yyyy` formatters and `parseOrNull`.

#### `persistencia/internal/` (8 classes)
| Class | Purpose |
|---|---|
| `ControladorPersistencia` | **Singleton** — the real implementation behind the contract interfaces. Every method is `synchronized`. Exposes `getInstance()`, `reinicialitzarForTest()`, `reinicialitzarForProfileSwitch()`, `obtenirConnexio()`, `netejarAllData()`, `snapshotForBackup()` / `BackupSnapshot`. |
| `ConnexioServidor` | Connection lifecycle, DDL, and the migration runner. |
| `ConnectionConfig` | `record ConnectionConfig(String dbType, String host, String user, String password, String profile, String testUrl)`. `dbType`/`host`/`user`/`password` are non-null (enforced in the compact constructor); `toString()` redacts the password as `password=***`. |
| `LlibreFieldBindings` | The single source of truth for the book **INSERT** column list and the `ResultSet`↔`Llibre` binding. `COLUMNS_INSERT` (27 columns — no `autor`, which now lives in the N:M tables) plus `forLlibre(...)` / `forInsert(Llibre)` / `forUpdate(Llibre)` / `bind(PreparedStatement, int, Object)`. Used by `LlibreDaoCore` **and** `ServeiCopiaSeguretat` — the backup writer and the DAO therefore cannot drift. The SELECT projections are derived separately by `LlibreDaoCore`'s own `Columna` enum. |
| `LlibreMapper` | Static `ResultSet` → `Llibre` mappers: `buildLlibreLight(rs)` and `buildLlibre(rs)`, shared between `LlibreDaoCore` (its own SELECTs) and `LlistaDao` (SELECTs that join book rows). Column names must match `LlibreDaoCore`'s canonical SELECT lists. |
| `MapejadorsFiles` | Shared read-only query helpers for DAOs. |
| `Schema` | The shared `CLEAR_ORDER` array — the correct child→parent DELETE sequence for FK constraints, shared by the DAO, the backup service, and every mass-reset path. |
| `JarLocator` | Finds the JDBC driver jars on the classpath at runtime. |

`ConnectionFactory` no longer exists. It was inlined into `ConnexioServidor.createDatabase`
by a refactor; `test/persistencia/ConnectionFactoryTest.java` survives but actually tests
`ConnectionConfig` and says so in its own Javadoc.

#### The schema

Base table, from `ConnexioServidor.CREATE_TABLE`:

```sql
CREATE TABLE IF NOT EXISTS llibre(
  ISBN BIGINT PRIMARY KEY,
  nom VARCHAR(255) NOT NULL,
  autor VARCHAR(255),
  `any` INT,
  descripcio VARCHAR(512),
  valoracio FLOAT,
  preu FLOAT,
  llegit BOOLEAN,
  imatge VARCHAR(255),
  imatge_blob BLOB
);
```

That is the **initial** shape. The current effective schema differs — see the migration
table below for what changed. The two structural points worth knowing up front:

- **`llibre.autor` no longer exists.** It was dropped in migration 34 after the data was
  backfilled into the `autor` / `llibre_autor` tables. A book now has many author rows;
  the flat `autor` string the domain and the UI still see is a `GROUP_CONCAT` projection
  rebuilt at read time.
- **`valoracio` and `preu` are `DOUBLE`,** not `FLOAT` (migrations 29–31).

Supporting tables: `schema_version (version INT NOT NULL)`, `llista`, `llibre_llista`,
`llibre_autor`, `llibre_tag`, `tag`, `autor`, `lectura`, `prestec`.

`llibre_llista` is the N:M between books and shelves, and it carries the
**per-shelf** state:

```sql
CREATE TABLE IF NOT EXISTS llibre_llista (
  isbn BIGINT NOT NULL,
  llista_id INT NOT NULL,
  valoracio DOUBLE DEFAULT 0.0,
  llegit BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (isbn, llista_id),
  FOREIGN KEY (isbn) REFERENCES llibre(ISBN) ON DELETE CASCADE,
  FOREIGN KEY (llista_id) REFERENCES llista(id) ON DELETE CASCADE
);
```

#### One enum drives every projection

`LlibreDaoCore` declares a private `enum Columna` that is the **single source of truth**
for the book's column set. Each entry carries the column name, its SELECT fragment, its
INSERT column name, and three flags (heavy / light-excluded / update-excluded). From that
one array the DAO derives all three SELECT projections and the INSERT/UPDATE statements:

| Projection | Contents |
|---|---|
| `LLIBRE_COLUMNS_L_LIGHT` | list view — excludes the heavy columns (`descripcio`, `notes`, `imatge_blob`) |
| `LLIBRE_COLUMNS_L` | full book read |
| `LLIBRE_COLUMNS_L_SHELF` | full read plus the shelf-relation extras |

Two entries are **subqueries** rather than plain columns, so their `selectFragment` is a
whole subselect and their `bind` is a no-op:

- `AUTOR_SUBQUERY` —
  `(SELECT GROUP_CONCAT(a.nom ORDER BY a.nom SEPARATOR ', ') FROM llibre_autor la JOIN autor a ON la.autor_id = a.id WHERE la.isbn = l.ISBN) AS autor`
- `HAS_BLOB_SUBQUERY` — whether a cover blob exists, without fetching it.

Adding a column to the schema means adding one entry here; the rest stays in sync
automatically.

#### Migrations — 39 steps

`ConnexioServidor` holds an ordered `Migracio[] MIGRATIONS` array of
`record Migracio(int version, String sql)`. On connect it reads `schema_version`, applies
every migration with a version greater than the current one, and records the new version.

| v | Change | v | Change |
|---:|---|---:|---|
| 1 | `llibre.imatge_blob BLOB` | 21 | table `autor` |
| 2 | table `llista` | 22 | table `llibre_autor` |
| 3 | table `llibre_llista` | 23 | `llibre.pais_origen VARCHAR(100)` |
| 4 | `llibre.notes VARCHAR(2048)` | 24 | `llibre.notes` → `TEXT` |
| 5 | `llista.ordre INT` | 25 | table `lectura` |
| 6 | `llibre.data_afegit TIMESTAMP` | 26 | `llibre.estat VARCHAR(20)` |
| 7 | `llista.color VARCHAR(7)` | 27 | `llibre.exemplars INT` |
| 8 | `llibre.pagines INT` | 28 | `llibre.llengua_original VARCHAR(100)` |
| 9 | `llibre.pagines_llegides INT` | 29 | `llibre.valoracio` → `DOUBLE` |
| 10 | table `prestec` | 30 | `llibre.preu` → `DOUBLE` |
| 11 | `llibre.editorial VARCHAR(255)` | 31 | `llibre_llista.valoracio` → `DOUBLE` |
| 12 | table `tag` | 32 | backfill distinct `autor` names into `autor` |
| 13 | table `llibre_tag` | 33 | backfill `llibre` → `llibre_autor` links |
| 14 | `llibre.serie VARCHAR(255)` | 34 | **`DROP COLUMN llibre.autor`** |
| 15 | `llibre.volum INT` | 35 | `llibre.nom_ca VARCHAR(500)` |
| 16 | `llibre.data_compra DATE` | 36 | `llibre.nom_es VARCHAR(500)` |
| 17 | `llibre.data_lectura DATE` | 37 | `llibre.nom_en VARCHAR(500)` |
| 18 | `llibre.idioma VARCHAR(100)` | 38 | index `idx_llibre_nom` on `llibre(nom)` |
| 19 | `llibre.format VARCHAR(50)` | 39 | index `idx_llibre_editorial_serie` on `llibre(editorial, serie)` |
| 20 | `llibre.desitjat BOOLEAN` | | |

The `IF NOT EXISTS` guards make each migration idempotent.

#### Connection and transaction strategy

- **Backend choice** is a runtime setting: `ConfiguracioDb.getType()` returns `"h2"` or
  `"mariadb"`. H2 runs against a file (default under the app's data directory) or an
  in-memory URL supplied by system properties for tests. MariaDB uses host/port/database/user/password.
- **No connection pool.** Each operation opens a connection, uses it, and closes it.
  Correctness comes from `synchronized` on every `ControladorPersistencia` method,
  which serialises DB access across threads.
- **Heavy-field loading** is lazy and explicit: `carregarHeavyFields(isbn, target)` and
  `carregarHeavyFieldsBatched(isbns, targets)` fetch `descripcio` / `notes` / cover
  blobs on demand. The list view uses the lean `obtenirAllLlibresSummary()` path.
- **Heavy columns are only written when they are loaded.** `LlibreDaoCore.update`
  excludes `descripcio` and `notes` from the `UPDATE` when
  `Llibre.teCampsPesatsCarregats()` is false, and `LlibreFieldBindings.forUpdate`
  omits the matching values. A book object that never loaded its heavy fields —
  a list-view row, or a detail panel whose background load failed — therefore
  cannot erase what is stored. "Not loaded", "loaded empty" and "explicitly
  cleared" are three distinct states.
- **Domain-side state is separately locked.** `StateContext.lock()` serialises access to
  the in-memory lists and the id indexes, independent of the DB lock.

#### Backup and restore

- `ControladorPersistencia.snapshotForBackup()` returns a `BackupSnapshot` — an
  in-memory `ArrayList` of books, shelves, tags and loans captured under the same lock.
  The books are read with the **full** projection, so descriptions and notes are
  included; the lean view is only for the list UI.
- `ServeiCopiaSeguretat.backupToSQL(file)` writes the snapshot out as SQL statements,
  deleting in `Schema.CLEAR_ORDER` so foreign keys never block the dump.
  The three-argument overload loads the heavy fields of any book it receives
  before writing, so an automatic or pre-restore backup is never lossy.
- `restaurarFromSQLFile(file)` takes a **pre-restore safety backup automatically**
  before executing the dump, then runs the SQL via `LlibreDaoCore.executarSQLFile`.
- **A restore validates the file first**: `LlibreDaoCore.validarCopiaSeguretat`
  requires the backup marker (`-- BIBLIOTECA BACKUP`) or at least one
  `INSERT INTO llibre`. An empty, truncated or foreign file is rejected *before*
  anything is deleted, so the database is never left empty by a bad file.
- `ImpressorTaulaCopiaSeguretat` renders the backup as a human-readable table.

---

### 4.4 `src/herramienta` — shared utilities (52 files)

Package root (3 files): `ImportadorLlibres` (the import entry point — CSV, Calibre, JSON;
`cercarSqlite3()` locates the `sqlite3` binary for Calibre imports), `ExportadorLlibres`
(the export entry point — CSV, JSON, HTML, PDF), `package-info`.

#### `herramienta/config/` (8 files)
| Class | Purpose |
|---|---|
| `Configuracio` | **Singleton** configuration façade. Loads/saves `~/.biblioteca/config.properties` (plain text) and exposes grouped accessors plus a `withBatch(Runnable)` transactional setter used by the settings dialog so all settings commit in one write. The class Javadoc explicitly warns that anyone with read access to `config.properties` can see the database password — it is stored in cleartext. |
| `ConfiguracioDTO` | Base record for the config sections. |
| `ConfiguracioDb` | DB type, host, port, database, user, password. |
| `ConfiguracioUi` | Theme, font size, currency symbol, UI language, sort column/order, view mode, default rating, column visibility. |
| `ConfiguracioFinestra` | Window geometry, column widths, last-used file paths. |
| `ConfiguracioFiltre` | Default cover folder and other filter defaults. |
| `MidaLletra` | Font-size enum (`small` / `medium` / `large`) with `fromKey(...)`. |
| `PresetStore` | Persists named filter presets (the save/load/delete buttons in the filter drawer). |

#### `herramienta/i18n/` (3 files)
| Class | Purpose |
|---|---|
| `I18n` | The runtime translator. **Not** `ResourceBundle`-based — it holds a hard-coded `Map<String, String[]> TABLE` of `key → [ca, es, en]` triples, filled by a static block of `T("key", "català", "español", "english")` calls. `I18n.t(key, args…)` looks up the key, picks the column for the active language, and substitutes `{0}`-style placeholders using **pre-compiled regex patterns** indexed by `args.length` (a 4-entry `PLACEHOLDER_PATTERNS` array; cases 1–3 cover ~99 % of calls). Also `aplicarSwingOptionPane()` to localise the standard Swing dialogs. |
| `Idioma` | Enum `ca` / `es` / `en`, each carrying its display name. |
| `Escapers` | HTML and property-file escaping helpers used when building Swing HTML labels and writing property files. |

**The translation pipeline is code generation, not resource loading:**

```
strings/ui.csv      (575) ┐
strings/errors.csv  (15) ├─► scripts/gen_strings.py ─► src/herramienta/i18n/I18n.java   ← what runs
strings/csv.csv      (5) ┘                   └─────────► src/herramienta/strings_{ca,es,en}.properties
                                                          595 keys each
```

`strings/strings.csv` is the legacy monolith (521 lines), used **only** if none of the
three split files exist. `I18n.java` carries the header
`// AUTO-GENERAT per scripts/gen_strings.py — edita strings/strings.csv en lloc d'això`
— edit the CSV, never the Java file.

The three `.properties` files are **written by the generator but never read at runtime**;
no Java source and no Makefile rule references them. They are current (595 keys each),
but they are exports, not the runtime source of truth.

#### `herramienta/io/` (7 files + 3 subpackages)
| Class | Purpose |
|---|---|
| `SeedorBaseDades` | Seeds the database on first run (`seedSiCal()`) — creates the schema and inserts starter data. |
| `ServeiCopiaSeguretat` | SQL backup export / restore, with the pre-restore safety copy. |
| `ServeiCoberta` | Fetches cover images (from the DB blob, the configured image folder, or OpenLibrary). |
| `ImpressorTaulaCopiaSeguretat` | Renders a backup as a readable table. |
| `JsonImporter` | The JSON import path. |
| `JsonHelpers` | JSON read/write helpers (Gson-based). |
| `RelationRemapper` | Rewrites relation rows (book↔shelf, book↔tag, book↔author) when IDs change during an import or restore. |

`herramienta/io/imports/` holds `ImportadorCsv`; `herramienta/io/export/` holds
`ExportadorCsv`, `JsonExporter`, `HtmlExporter` and `GoodreadsExportService`.

#### `herramienta/io/csv/` — the CSV strategy pattern (8 files)
| Class | Purpose |
|---|---|
| `CsvImportStrategy` | The strategy interface. |
| `NativeCsvStrategy` | The app's own CSV dialect. |
| `GoodreadsCsvStrategy` | Goodreads export dialect. |
| `LibraryThingCsvStrategy` | LibraryThing export dialect. |
| `Rfc4180Reader` | Strict RFC-4180 CSV reader (quoted fields, embedded newlines, escaped quotes). |
| `UtilitatsCsv` | Header detection, column mapping, value coercion. |
| `ShelvesHelper` | Maps Goodreads/LibraryThing shelf columns onto `Llista` records. |
| `AnalitzadorPertinençaPrestatgeria` | Decides which shelf an imported row belongs to. |

Format detection inspects the header row and picks the matching strategy.

#### `herramienta/text/` (10 files)
| Class | Purpose |
|---|---|
| `Isbn13Normalizer` | ISBN-10 ↔ ISBN-13 conversion with check-digit validation and normalisation. |
| `UtilitatsData` | Date parsing across the formats the app accepts, and formatting. |
| `AutoCompletion` / `Completer` / `FieldAutoComplete` | Autocomplete engine. `FieldAutoComplete.attachDistinct(cd, field…)` wires distinct-value suggestions into author / editorial / series / language fields. |
| `ValidadorLlibre` | Book validation (ISBN, required title, numeric ranges). |
| `FiltreUtils` | The single source of truth for "does this book match this filter" — used by the in-memory row filter. |
| `FormatOptions` | Format/label helpers. |
| `EstatOptions` | Reading-state option labels. |
| `ParseHelpers` | Small shared parsing helpers used by the import and text layers. |

#### `herramienta/api/` (2 files)
| Class | Purpose |
|---|---|
| `ClientOpenLibrary` | HTTP client for **OpenLibrary and Google Books**, built on `java.net.http.HttpClient`. Endpoints: `https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data` (ISBN lookup), `https://www.googleapis.com/books/v1/volumes?q={query}&maxResults=1` (title/author search), `https://covers.openlibrary.org/b/isbn/{isbn}-L.jpg` (cover image). `base()` honours a `testBaseUrl` override so tests can point at a local stub. `fetchWithRetry` does `MAX_RETRIES = 3` attempts with **exponential backoff** (`RETRY_BASE_MS = 500`, delay `baseMs * (1L << i)`), and a `rateLimit()` gate throttles outbound calls. |
| `AnalitzadorOpenLibrary` | Parses the OpenLibrary / Google Books JSON response into metadata fields plus cover bytes. |

#### `herramienta/ui/` (6 files)
| Class | Purpose |
|---|---|
| `UITheme` | Theme enum + palette. `posarTheme(...)`, `rebuildFonts(MidaLletra)`, `palette()`, `esDark()`, `fontBase()`. |
| `UtilitatsSwing` | Swing helpers — component lookup, colour utilities, layout tweaks. |
| `UtilitatsColor` | Colour maths and conversions. |
| `SelectorMostraColor` | The colour-picker widget used for shelf colours. |
| `Documents` | `DocumentListener` helpers — used for the live cover preview and live field validation. |
| `DialegError` | The standard error dialog. Accepts `Exception` or `Throwable`. |

---

### 4.5 `src/presentacio` — Swing UI (83 files)

**Naming convention**: a `Panel*` / `Dialeg*` class is the **view** (pure Swing, exposes
components); the matching `Controlador*` class is the **behaviour** (listeners, business
calls, SwingWorker offloading). Views expose their widgets; controllers wire them.

#### `presentacio/util/` (10 files) — shared UI state and helpers
| Class | Purpose |
|---|---|
| `EstatVistaBiblioteca` | **The shared mutable UI state.** Holds the current book list, the model list, the current shelf id, the loaned-ISBN set, the group-by-series flag and the undo buffer (`UNDO_MAX = 20`). Also owns the `CoordinadorGeneracions`. |
| `CoordinadorGeneracions` | **Async staleness guard.** Each result flow (`FLUX_CONSULTA`, `FLUX_FILTRE`, `FLUX_PAGINA`) has its own counter; opening a generation bumps the counter and invalidates the flows that query supersedes. A generation is live only if its counter is still the flow's latest, so no stale result can be applied to the EDT. The table and the gallery share `FLUX_CONSULTA` because they represent the same query. `SwingWorker.cancel()` is only an optimisation — a JDBC query is not always interruptible, so the guarantee comes from the generation comparison made just before applying the result, not from `isCancelled()`. |
| `AmfitrioPantallaBiblioteca` | Interface for the host screen. |
| `GestioLlistesListener` | Explicit callback interface for shelf-management actions (replaces bare `Runnable`). |
| `GestorArrossegarSoltarPrestatgeria` | Attaches the shelf drag-and-drop targets; keeps DnD logic out of `PanellBarraEsquerra`. |
| `MemoriaImatgesCoberta` | LRU cover-icon cache shared by table and gallery on the main screen. |
| `AjudaEstadistiques` | Statistics helpers. |
| `DreceresTeclat` | Keyboard shortcuts. |
| `UIComponents` | Shared Swing component factories. |
| `UtilitatsLlibre` | Book display helpers. |

#### `presentacio/panells/` (8 files)
| Class | Purpose |
|---|---|
| `PanelMarcPrincipal` | The main window body. `BorderLayout`; owns `PanelMostrarBiblioteca` and the status bar (`JLabel`). |
| `PanelMostrarBiblioteca` | The central library view — top toolbar, filter drawer, table, gallery, pagination bar. |
| `PanelBarraSuperior` | Top toolbar: search, view toggle, filter toggle, book actions. |
| `PanelCalaixFiltre` | The filter drawer (slide-out panel) with all filter fields. |
| `PanellBarraEsquerra` | Left drawer: shelves combo + tags combo. |
| `PanelTaulaCentral` | The `JTable` host. |
| `PanelGaleriaCobertes` | The cover-card gallery. |
| `PantallaInici` | Splash screen. |

#### `presentacio/controladors/` (15 files)
| Controller | Owns |
|---|---|
| `ControladorMarcPrincipal` | The `JFrame`: title bar, menu, accelerators, the "new book" and "scan station" entry points, and the global uncaught-exception alert path. |
| `ControladorMostrarBiblioteca` | Coordinator — creates the sub-controllers below and routes between them. |
| `ControladorTaula` | `ModelTaulaBiblioteca`, renderers, column visibility, sort persistence, double-click-to-open, Ctrl+click-to-filter-by-author, debounced width persistence (800 ms). |
| `ControladorPaginaTaula` | Pagination. `PAGE_SIZE = 100`. Two modes: DB-side (`obtenirLlibresPage`) and in-memory slicing. |
| `ControladorFiltre` | The filter drawer, the search bar (250 ms debounce), and filter presets (load/save/delete). |
| `ControladorAccionsLlibre` | Open/edit details, delete + undo (buffer of 20), statistics dialog, random unread book, quick views (recently added, recently read, wanted, in-progress), settings. |
| `ControladorMenuContextual` | Right-click menus for table and gallery: open, delete one/many, add to shelf, duplicate, batch edit, loan/return, copy ISBN. |
| `ControladorModeVista` | Gallery/table toggle, group-by-series, title-bar text. |
| `ControladorPrestatgeria` | Shelf combo, shelf drag-and-drop, shelf management dialog, shelf colour swatches. |
| `ControladorIOLlibre` | Thin facade over the three IO controllers. |
| `ControladorImportacio` | CSV / Calibre / JSON import, ISBN scan-and-fetch. |
| `ControladorExportacio` | CSV / JSON / HTML / PDF export, plus bulk cover fetch (with a re-entrancy guard). |
| `ControladorCopiaSeguretat` | Backup to SQL file, restore from SQL file. |
| `ControladorEstacioEscaneig` | The scan station backend. `Outcome` enum (`ADDED`, `DUPLICATE`, `INVALID`, `NETWORK_ERROR`, `OTHER_ERROR`), a `Semaphore(1)`, a `Deque<String>` queue, and `LongAdder` counters. |
| `ControladorGaleria` | Minimal gallery update coordinator. |

#### `presentacio/models/` (3 files)
- `ModelTaulaBiblioteca` — `AbstractTableModel` with 10 columns:
  `COL_COVER=0`, `COL_ISBN=1`, `COL_NOM=2`, `COL_AUTOR=3`, `COL_ANY=4`,
  `COL_VALORACIO=5`, `COL_PREU=6`, `COL_LLEGIT=7`, `COL_PROGRES=8`, `COL_DETALLS=9`.
  Only `COL_LLEGIT` is cell-editable.
- `ModelPaginacio` — pagination state: current page, paginated mode, DB-pagination flag,
  cached total count with `invalidateCache()`.
- `VistaPaginacio` — applies a page to the pagination widget (`apply(page, totalPages)`,
  `hide()`).

#### `presentacio/renderers/` (5 files)
`RenderitzadorCellaCoberta` (cover thumbnail), `RenderitzadorCasellaLlegit` +
`EditorCasellaLlegit` (read checkbox), `RenderitzadorBarraProgres` (reading progress),
`RenderitzadorDestacatCerca` (search-term highlighting, and marks loaned rows).

#### `presentacio/detalles/` (12 files)
- `vista/PanellDetallsLlibre` — the book-detail editor (`JDialog`): cover preview, tabs
  (general / notes / advanced), and the editable inputs declared through a
  `RegistreCampsFormulari` registry.
- `vista/DialegDesarLlibres` — the new-book form. Builds its grid through
  `ConstructorGraellaFormulari.columnsOf(grid)` and its fields through
  `RegistreCampsFormulari`.
- `vista/DialegEntitatLlibre<E>` — abstract base for the tag and shelf dialogs.
- `vista/DialegEtiquetesLlibre` / `vista/DialegLlistesLlibre` — the two concrete dialogs.
- `vista/ModelTaulaLlistesLlibre` — the per-book shelf-membership table
  (name / rating / read; the last two editable).
- `control/ControladorPanellDetallsLlibre` — detail-panel behaviour, including lazy
  heavy-field loading in a `SwingWorker`.
- `control/ControladorDialegDesarLlibres` — new-book form behaviour.
- `control/ControladorEntitatLlibre<V, W>` — abstract base for the two entity controllers.
- `control/ControladorEtiquetesLlibre` / `control/ControladorLlistesLlibre` — concrete.
- `control/TascaCercaOpenLibrary` — `SwingWorker` that queries OpenLibrary and fills the form.

#### `presentacio/dialegs/` (5 files)
`ConfiguracioDialog` (settings), `DialegCarrega` (progress dialog **and** the standard
`runAsync(parent, title, backgroundTask, onDone)` async pattern used by every long
operation), `DialegGestioLlistes` (shelf manager), `EstacioEscaneigDialog` (scan station),
`QuantADialeg` (About — reads `/version.properties` from the classpath).

#### `presentacio/galeria/` (3 files)
- `FabricantTargetesCoberta` — builds and paints the gallery cards. The painter is a
  **static** method (`paintCard(Graphics2D, …)`) so it is unit-testable without Swing.
  Card hue is derived from the book title.
- `ServeiImatgesCoberta` — async cover loader with an LRU cache
  (capacity 150, load factor 0.75, 4 daemon threads). The cache is a `LinkedHashMap`
  guarded by a `ReentrantReadWriteLock` with `accessOrder=false`, so concurrent EDT reads
  never structurally mutate the map.
- `PopupZoomCoberta` — 500 ms debounced hover zoom, its own 32-entry LRU.

#### `presentacio/formularis/` (4 files)
- `RegistreCampsFormulari` — declarative field registry (`Camp.text/check/combo/button`),
  with typed accessors `textField(key)`, `comboBox(key)`, `comprovarBox(key)`, `button(key)`.
- `ConstructorGraellaFormulari` — fluent `GridLayout` form builder. `columnsOf(host)` opens
  a builder; `afegirField` / `afegirCombo` / `afegirCheck` / `afegirRaw` add rows.
- `LligadorFormulariLlibre` — small field-binding helper.
- `ValidadorFormulari` — field validation styling.

#### `presentacio/layouts/` (1 file)
`WrapLayout` — a `FlowLayout` that wraps (adapted from the Oracle Java Tutorial sample).
Used only by `PanelGaleriaCobertes`.

#### `presentacio/listener/` (6 files)
`EnActualitzarLlibre` (book changed), `EnEliminarLlibre` (**cancellable / vetoable**
delete — `EsborrarEvent` with `veto()`), `EnActualitzarBBDD` (the aggregate of both),
`EnAfegirLlibre`, `EnCanviarBlobLlibre`, `EnCanviarMembreLlista`.

#### `presentacio/config/` (10 files)
`ConfiguracioDialogListener`, `ConfiguracioSeccio`, `SeccionsConfiguracio`
(widget lookup by client-property `"id"`), `ConfiguracioConstants`, `BarraBotonsConfiguracio`
(saves every setting in one `Configuracio.withBatch(...)` transaction), `ConfiguracioDataSection`,
and the four section builders: `SeccioDbConfiguracio`, `SeccioImatgesConfiguracio`,
`SeccioAparençaConfiguracio`, `SeccioIdiomaConfiguracio`.

---

## 5. Key runtime flows

### 5.1 Write path (UI → SQL)

```
ControladorMarcPrincipal / ControladorMostrarBiblioteca
  → ControladorAccionsLlibre (or ControladorImportacio)
    → ControladorDomini.getInstance()
      → DelegatLlibre / DelegatPrestatgeria / TagDelegate / DelegatPrestec
        → ControladorPersistencia (synchronized)
          → persistencia.dao.*Dao
            → Connection (JDBC) → H2 or MariaDB
```

### 5.2 Read path with pagination

```
ControladorPaginaTaula.mostrarPage(page, biblio)
  ├─ DB mode  → cd.comptarLlibresDB() (cached) + cd.obtenirLlibresPage(offset, 100)
  └─ in-memory → biblio.subList(from, to)
  → ControladorMostrarBiblioteca.posarTable(list)
    → ControladorTaula.posarBooks(...) → ModelTaulaBiblioteca
    → gallery refresh if in gallery mode
```

### 5.3 Filter

```
ControladorFiltre.filtrar()
  → build domini.LlibreFilter from the text fields + read state + format + tag + shelf
  → if tag/shelf filter active: SwingWorker fetches the ISBN sets
  → aplicarRowFilterInMemory(...) → RowFilter delegating to
       herramienta.text.FiltreUtils.matches(l, f, tagISBNs, llistaISBNs)
```

Above `DelegatLlibre.SQL_FILTER_THRESHOLD` (2 000 books) the filter is pushed into
SQL via `domini.SqlOp` instead of run in memory.

### 5.4 Async results and staleness

```
Controller opens a generation   →  CoordinadorGeneracions.obrir(flux)
   ... SwingWorker queries the DB ...
Controller applies the result    →  if (!generacions.esVigent(flux, generacio)) discard
                                 else apply to the EDT
```

`FLUX_CONSULTA` is shared by the table and the gallery, so a stale table result can
neither touch the gallery nor invalidate its counters. `FLUX_FILTRE` and `FLUX_PAGINA`
both produce the visible list and therefore supersede each other.

### 5.5 Import

```
ControladorIOLlibre.importarCSV()
  → ControladorImportacio.importarCSV()
    → DialegCarrega.runAsync(...)
      → ImportadorLlibres.importarCSV(file, cd)
        → Rfc4180Reader → header detection → CsvImportStrategy (Native / Goodreads / LibraryThing)
          → ShelvesHelper + AnalitzadorPertinençaPrestatgeria for shelf mapping
```

Calibre imports pick a **directory** containing `metadata.db` and shell out to `sqlite3`,
resolved by `ImportadorLlibres.cercarSqlite3()`.

### 5.6 Backup / restore

```
ControladorCopiaSeguretat.copiaSegBD()
  → cd.copiaSegToSQL(file)
    → ControladorPersistencia.snapshotForBackup()   [in-memory snapshot under lock]
    → ServeiCopiaSeguretat.backupToSQL(file)        [DELETE order = Schema.CLEAR_ORDER]

ControladorCopiaSeguretat.restaurarBD(onSuccess)
  → two-step confirmation
  → cd.restaurarFromSQL(file)
    → LlibreDaoCore.validarCopiaSeguretat(file)     [rejects empty/foreign files FIRST]
    → automatic pre-restore safety backup
    → LlibreDaoCore.executarSQLFile(file)
```

### 5.7 Scan station

```
Barcode scanner → EstacioEscaneigDialog text field
  → ControladorEstacioEscaneig.submitIsbn(raw)
    → Semaphore(1) + Deque<String> queue
    → SwingWorker<Result, Void>
      → ISBN validation → duplicate check → OpenLibrary lookup
      → Outcome { ADDED | DUPLICATE | INVALID | NETWORK_ERROR | OTHER_ERROR }
    → SwingUtilities.invokeLater → dialog colour + counters
```

---

## 6. Build, run, test

### Build

```bash
make compile        # javac -g --release 25 -cp <deps> @classes.txt -d bin
make run            # java -cp bin:<deps> main.Executable
```

The `Makefile` drives everything with plain `javac`/`java`. It locates the JDK by probing
candidates in order (`which javac`, then `find /usr/lib/jvm /home/j/opt /usr/java`) and
accepting only a launcher that actually prints a version — a zero-byte or truncated stub
exits 0 without compiling and would otherwise produce a silent empty build. The probe
result is cached in the `JAVAC` / `JAVA` variables, with the `compile` and `run` targets
re-checking the probe output and failing loudly with an explicit error.

`make compile` compiles **only `src/`** into `bin/` (338 class files measured). It also
copies `src/herramienta/*.properties`, `src/version.properties` and `src/LICENSE` into the
build dir.

Non-Java resources under `src/`:
- `src/version.properties` → `version=2.10` — read from the classpath by `QuantADialeg`
  (the About dialog) at runtime.
- `src/herramienta/strings_{ca,es,en}.properties` — **generated export artifacts** of the
  translation pipeline, copied into the build but never read at runtime.
- `src/fichers/` — `234.csv` (sample import data), `credit.html`, `license.html`.
- `src/LICENSE` — copied into the build dir.

The Makefile also has a `cp -r src/web` rule guarded by `if [ -d src/web ]` — that
directory **does not exist**, so the rule is a no-op.

### Test

```bash
make test              # legacy suite + one JUnit Platform session
make test-mariadb      # MariaDB contract against a throwaway container (see docs/MARIADB-TEST.md)
make h2-compat         # open a legacy-H2 database with the current driver
make fuzz-property     # jqwik property tests
make fuzz-jazzer       # coverage-guided Jazzer harnesses
```

The `test` target:

1. Compiles **every** `*.java` under `test/` into `bin/`.
2. Runs the legacy `test/BibliotecaTest.java` as a plain `java` invocation.
3. Runs one JUnit Platform session selecting **every** test class derived from its
   source path (so `FooTest.java` and `TestFoo.java` are both picked up), minus the
   Jazzer harnesses, which are detected by grepping for `com.code_intelligence.jazzer`
   rather than by a hard-coded list.
4. Writes JUnit XML to `build/reports/tests/`.

**Measured result** — see §2 for the full table. 715 tests found, 694 successful,
0 failed, exit code 0.

The five excluded Jazzer harnesses are:
`fuzz.domini.TestFuzzAnalitzadorPrestatgeria`,
`fuzz.domini.TestFuzzConstructorFiltreLlibre`,
`fuzz.domini.TestFuzzEspecificacioOrdenacio`,
`fuzz.herramienta.io.csv.Rfc4180FuzzTest`,
`fuzz.herramienta.io.csv.TestFuzzUtilitatsCsv`.

### Dependencies

`build/deps.lock` is the single source of truth. Each line is
`group:artifact:version sha256 tag`, and the tag selects the classpath set.

| Tag | Count | Members |
|---|---:|---|
| `runtime` | 8 | h2 2.5.252, mariadb-java-client 3.5.10, gson 2.14.0, slf4j-api 2.0.20, error_prone_annotations 2.48.0, waffle-jna 3.7.0, jna 5.19.1, jna-platform 5.19.1 |
| `test` | 8 | junit-platform-console-standalone 1.14.4, assertj-core 3.27.7, mockito-core 5.24.0, byte-buddy 1.18.15, byte-buddy-agent 1.18.15, objenesis 3.6, jqwik-api 1.10.2, jqwik-engine 1.10.2 |
| `fuzz` | 3 | jazzer 0.30.0, jazzer-api 0.30.0, jazzer-junit 0.30.0 |
| `legacy` | 1 | h2 2.3.232 — reference driver for the H2-compatibility check; in no classpath |

`make deps` fetches, `make deps-verify` re-checks every SHA-256, `make deps-update`
refreshes versions. `lib/` also holds older jar versions left over from earlier
resolutions; only the lock-pinned ones enter any classpath.

### CI

`.github/workflows/ci.yml` runs on push, pull request, manual dispatch and a weekly
schedule (Mondays 03:17 UTC). Six jobs:

| Job | What it does |
|---|---|
| `build-and-test` | toolchain check → `make deps` → `make deps-verify` → `make compile` → `make test` → upload JUnit XML |
| `i18n` | `python3 scripts/gen_strings.py --check --no-java` |
| `h2-compat` | `make h2-compat` — opens a legacy-driver database with the current driver |
| `package` | `make jar` and asserts the JAR contains `main/Executable.class`, `version.properties`, `strings_ca.properties` |
| `fuzz` (scheduled) | `make fuzz-property` + `make fuzz-jazzer` |
| `dependencies` (scheduled) | checks for newer dependency versions |

### Also present

- `checkBiblio/` — a flat folder of standalone audit tools (4 Java files, 3 465 lines):
  `UIAudit` (770 lines, drives the running UI), `TestEstres` (1 900 lines, stress/soak
  driver), `I18nAudit` (251 lines, translation-key auditor), `SuportTestUi` (547 lines,
  shared UI support). Plus `biblioteca-ui-stress.user.js` (a Tampermonkey userscript) and
  the `run.sh` / `run.ps1` launchers. **Not part of `make compile`** — `checkBiblio/run.sh`
  compiles them into `bin/` with its own `javac` call, and `make audit` assumes they are
  already there.
- `scripts/` — dependency fetching (`fetch_deps.py`), packaging helpers, the i18n
  generator, fuzzing helpers, `mariadb_test.sh`, `h2compat/`, and agent-loop tooling.
- `portades/` — 3 seed cover images (`default_cover.png`, `q.png`, `r.jpg`).
- `packaging/` — `installer.nsi` (NSIS), `launch4j.xml`, a bundled `jre/`, `icon.svg`.
- `docs/agent/` — `AGENTS.md`, `CLAUDE.md`, `MISTAKES.md` (project conventions and a
  running log of past mistakes).
- `docs/` — `fuzzing.md`, `MARIADB-TEST.md`, `I18N-NEWLINES.md`, `MODERNITZACIO.md`,
  `NAMING_POLICY.md`, and `code-review/` (review artefacts).

---

## 7. How to reproduce the numbers

```bash
# Layer metrics
for d in main domini persistencia herramienta presentacio; do
  printf "%-12s %3d files %6d lines\n" "$d" \
    "$(find src/$d -name '*.java' | wc -l)" \
    "$(find src/$d -name '*.java' -exec cat {} + | wc -l)"
done

# Test surface
find test -name '*.java' | wc -l                              # 69 files
grep -rho '@Test' test/ --include=*.java | wc -l              # 634
grep -rho '@ParameterizedTest' test/ --include=*.java | wc -l # 17
grep -rho '@Property' test/ --include=*.java | wc -l           # 6

# i18n keys
python3 scripts/gen_strings.py --check --no-java   # 595 keys, properties in sync

# Dependency inventory
grep -c ' runtime$' build/deps.lock               # 8 runtime deps
```

`make compile` and `make test` must be run with a working JDK on `PATH`. The Makefile
probes for one and fails with an explicit message if it cannot find a launcher that
prints a version — so a broken JDK cannot silently produce an empty build.

---

## 8. Findings and risks

1. **`gen_strings.py` cannot regenerate `I18n.java` faithfully — and CI cannot see it.**
   The generator's Java template (`gen_java`, `scripts/gen_strings.py:136-142`) emits a
   naive `raw.replace("{0}", …)` loop, while the committed `I18n.java` substitutes
   placeholders through a pre-compiled `PLACEHOLDER_PATTERNS` regex array indexed by
   `args.length`. Consequences:
   - `python3 scripts/gen_strings.py --check` **fails permanently** on `I18n.java`.
   - Running the generator **silently downgrades** the runtime substitution path and
     replaces the Catalan header with an English one.
   - CI's `i18n` job runs `--check --no-java`, which validates only the `.properties`
     exports — so the drift is invisible to CI and the build stays green.
   **Fix:** make `gen_java` emit the placeholder-pattern block, then drop `--no-java`
   from the CI job so the real file is checked.
2. **Two implementations of the same filter.** Below 2 000 books filtering runs in memory
   via `FiltreUtils`; above it, filtering is pushed into SQL via `SqlOp`. Two code paths
   for one behaviour means they can drift.
3. **No connection pool.** Every DB call opens and closes a connection. Fine for a
   single-user desktop app, but it is the main scaling limit if this ever grew.
4. **All `ControladorPersistencia` methods are `synchronized`** — a single global lock
   serialises every DB operation including long bulk imports.
5. **Three parallel ways to build a `LlibreFilter`.** `LlibreFilter` exposes mutable
   `with*(...)` setters that mutate in place and return `this`;
   `domini.ConstructorFiltreLlibre` is a separate 18-method builder ending in `build()`;
   and `new LlibreFilter()` is used directly by the builder itself. Production code uses
   the `with*` path (`ControladorFiltre.filtrar()`), tests use the builder. This is safe
   today only because every call constructs a fresh instance — but the `with*` shape
   invites aliasing, since a filter handed to a background worker can still be mutated by
   whoever owns it.
6. **A misnamed test class.** `test/persistencia/ConnectionFactoryTest.java` tests
   `ConnectionConfig`, not a `ConnectionFactory` — that class was inlined into
   `ConnexioServidor.createDatabase` by a refactor. The test's own Javadoc says so, but
   the filename still points at a class that no longer exists, which misleads grep-based
   navigation.
7. **Heavy-field failure is silent-ish.** If `carregarHeavyFields` fails, the detail
   panel logs a warning that description and notes "will render empty and any save will
   overwrite the originals" — a save after a failed load destroys data.
8. **Cover fetch is a network dependency in a batch path.** `fetchMissingCovers` hits
   OpenLibrary per book. It is protected by a re-entrancy guard, and the client applies
   `rateLimit()` plus 3 retries with exponential backoff — but a large library still
   means hundreds of sequential network round-trips, and the user waits behind a
   modal progress dialog.
9. **`lib/` carries duplicate jar versions.** Older resolutions (assertj 3.26.3 alongside
   3.27.7, h2 2.3.232 alongside 2.5.252, and so on) sit next to the lock-pinned ones.
   Nothing on the classpath picks them up, but the directory is misleading and bloats
   the checkout.
10. **`checkBiblio` is not covered by `make compile`.** `make audit` runs
    `checkBiblio.UIAudit` from `bin/`, but only `checkBiblio/run.sh` ever compiles those
    classes there. A clean `make compile` leaves `make audit` failing on a missing class.

---

## 9. Where to look first

| Question | File |
|---|---|
| How does the app start? | `src/main/Executable.java` |
| What is the domain API? | `src/domini/ControladorDomini.java` |
| Where is the shared UI state? | `src/presentacio/util/EstatVistaBiblioteca.java` |
| How are stale async results rejected? | `src/presentacio/util/CoordinadorGeneracions.java` |
| How is a book stored? | `src/persistencia/internal/ConnexioServidor.java` (DDL + 39 migrations) |
| How does a write reach the DB? | `src/persistencia/dao/` + `ControladorPersistencia` |
| Where does the UI live? | `src/presentacio/panells/` (views) + `src/presentacio/controladors/` (behaviour) |
| How are strings translated? | `src/herramienta/i18n/I18n.java` (generated) — edit `strings/*.csv` and re-run `scripts/gen_strings.py` |
| How is the config stored? | `src/herramienta/config/Configuracio.java` |
| How do CSV imports detect format? | `src/herramienta/io/csv/` |
| How does the gallery draw cards? | `src/presentacio/galeria/FabricantTargetesCoberta.java` |
| How does backup work? | `src/herramienta/io/ServeiCopiaSeguretat.java` |
| How is the test suite selected? | `Makefile` — `TEST_CLASSES` / `JAZZER_TEST_CLASSES` |
