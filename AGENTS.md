# AGENTS.md

## Build & Test Commands

```bash
make compile          # compile src/ → bin/ (uses classes.txt, not glob)
make test             # runs BibliotecaTest (plain Java) + BibliotecaJUnit5Test (JUnit 5)
make clean            # rm -rf bin/*
make run              # clean + compile + run GUI
make run-only         # run without recompile
```

**Always run `make test` before reporting a task complete. All tests must pass.**

Manual classpath: `lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:lib/gson-2.11.0.jar:.`

## Test System Properties

- `-Dbiblioteca.test=true` — makes DB errors throw instead of showing GUI dialogs
- `-Dbiblioteca.h2.url="jdbc:h2:mem:junit5;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"` — H2 in-memory URL for JUnit 5 tests
- Between test groups, call `ControladorDomini.resetForTest()` and `ControladorPersistencia.resetForTest()` to reset singletons

## Database Migrations

Schema versioned in `ServerConect.MIGRATIONS` (version via `schema_version` table). **Never alter the `CREATE_TABLE` string** — add new migrations as new entries in the array instead.

## Config

All runtime config in `~/.biblioteca/config.properties` (dbType, darkMode, fontSize, window geometry, column widths, etc.)

## Architecture Notes

- Three-layer MVC: `presentacio/` (Swing), `domini/` (business logic), `persistencia/` (JDBC)
- `MainFrameControl` is the root singleton; `MostrarBibliotecaControl` manages the main table
- Each screen has a `*Panel` (Swing layout) + `*Control` (listeners + domain calls)
- `test/` is gitignored — tests are local-only

## Misc

- Language: Catalan/Spanish mixed
- `lib/junit-platform-console-standalone-1.11.4.jar` used for JUnit 5 console launch
- `opencode.json` is minimal (`lsp: true`) — most config is in Makefile and CLAUDE.md
