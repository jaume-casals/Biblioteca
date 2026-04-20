# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
make compile          # compile src/ → bin/
make run              # clean + compile + run GUI
make run-only         # run without recompile (bin/ must exist)
make test             # compile + run test/BibliotecaTest.java
make populate         # fetch 2000 books from OpenLibrary and insert
make populate MAX=500 # custom limit
make clean            # rm -rf bin/*
```

Classpath for manual invocation: `lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar:.`

Run a single test class manually:
```bash
java -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar test.BibliotecaTest
```

## Architecture

Three-layer MVC with singletons at each boundary. Language is Catalan/Spanish mixed.

```
presentacio/   ← Swing GUI (panels + controllers, MVC split per screen)
domini/        ← Business logic, in-memory sorted list of Llibre
persistencia/  ← JDBC layer (ControladorPersistencia → ServerConect)
herramienta/   ← Utilities: Config, UITheme, LlibreValidator, FiltreUtils, AutoCompletion
interficie/    ← EnActualizarBBDD callback interface
main/          ← Ejecutable entry point
```

**Data flow:** `ServerConect` opens JDBC → `ControladorPersistencia` (thin wrapper) → `ControladorDomini` loads all rows into `ArrayList<Llibre>` sorted by ISBN (binary search for O(log n) ops) → presentation controllers call domain methods.

**DB:** H2 embedded by default (`~/.biblioteca/biblioteca`). Switch to MariaDB via `~/.biblioteca/config.properties` (`dbType=mariadb`). Schema migrations live in `ServerConect.MIGRATIONS` (versioned via `schema_version` table). Add new migrations there — never alter the base `CREATE_TABLE` string.

**Config:** `herramienta.Config` reads/writes `~/.biblioteca/config.properties`. Covers: `dbType`, `dbHost`, `dbUser`, `dbPassword`, `darkMode`, `defaultImgDir`.

**UI pattern:** Each screen has a `*Panel` (pure Swing layout, getters for components) and a `*Control` (wires listeners, calls domain). `MainFrameControl` is the root singleton; it holds `MostrarBibliotecaControl` which manages the main table. `DetallesLlibrePanel`/`DetallesLlibrePanelControl` handle the book detail/edit view.

**Keyboard shortcuts:** `Ctrl+N` = new book, `Ctrl+F` = focus filters, `Delete` on table row = delete book. Wired in `MainFrameControl`.

**Tests:** Plain-Java, no JUnit. `test.BibliotecaTest` uses H2 in-memory (`jdbc:h2:mem:test`). Set `System.setProperty("biblioteca.test", "true")` before triggering domain code in tests to make DB errors throw instead of showing GUI dialogs. `ControladorDomini.resetForTest()` and `ControladorPersistencia.resetForTest()` reset singletons between test groups.

**Cover images:** Stored two ways — `imatge` (String path/URL) and `imatge_blob` (BLOB). Migration 1 added `imatge_blob`.

**Pending:** Multiple libraries/shelves feature needs new `llista` table + FK on `llibre` (see `todo.txt`).
