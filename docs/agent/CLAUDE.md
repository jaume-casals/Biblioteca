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

Run tests manually:
```bash
java -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar test.BibliotecaTest
```

**Always run `make test` before reporting a task complete. All tests must pass.**

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

**Config:** `herramienta.Config` reads/writes `~/.biblioteca/config.properties`. Keys: `dbType`, `dbHost`, `dbUser`, `dbPassword`, `darkMode`, `fontSize`, `defaultImgDir`, `windowX/Y/Width/Height/Maximized`, `colWidth_N`, `colVisible_N`, `presetCount`, `preset.N.*`.

**UI pattern:** Each screen has a `*Panel` (pure Swing layout, getters for components) and a `*Control` (wires listeners, calls domain). `MainFrameControl` is the root singleton; it holds `MostrarBibliotecaControl` which manages the main table. `DetallesLlibrePanel`/`DetallesLlibrePanelControl` handle the book detail/edit view.

**Keyboard shortcuts** (wired in `MainFrameControl`):
- `Ctrl+N` = new book
- `Ctrl+F` = focus search bar
- `Ctrl+E` = open selected book in edit mode
- `Ctrl+A` = select all table rows
- `Enter` = open selected book details
- `Delete` = delete selected book(s)

**Shelves (Llistes):** `llista` table + `llibre_llista` join (many-to-many). Each membership stores per-shelf `valoracio` and `llegit`. `Llista.valoracioLlibre` / `llegitLlibre` carry per-book shelf values when loaded via `getLlistesForLlibre`.

**Tests:** Plain-Java, no JUnit. `test.BibliotecaTest` uses H2 in-memory (`jdbc:h2:mem:test`). Set `System.setProperty("biblioteca.test", "true")` before triggering domain code to make DB errors throw instead of showing GUI dialogs. `ControladorDomini.resetForTest()` and `ControladorPersistencia.resetForTest()` reset singletons between test groups.

**Cover images:** Stored two ways — `imatge` (String path/URL) and `imatge_blob` (BLOB). Migration 1 added `imatge_blob`.

**Theme:** `UITheme.setDark(bool)` updates color constants + calls `applyUIManager()`. `MostrarBibliotecaPanel.applyTheme()` reinstalls Nimbus L&F (so it re-derives colors) then calls `updateComponentTreeUI`. Font size rebuilt via `UITheme.rebuildFonts(size)` — called at startup and on config save.

**gitignore:** `CLAUDE.md`, `Makefile`, `todo.txt`, `install.exe`, `install.sh`, `.idea/`, `.vscode/`, `test/` are all gitignored and untracked.
