# Biblioteca

A personal library manager built with Java and Swing. Track your books, organise them into shelves, rate what you've read, and fetch metadata straight from OpenLibrary.

---

## Features

- **Book catalogue** — add, edit, and delete books with title, author, ISBN, price, rating, and cover image
- **Shelves** — organise books into named shelves with per-shelf ratings and read status
- **Search & filter** — live search across title, author, ISBN, description, and notes; filter by rating, read status, and shelf
- **Cover images** — load from a local file or fetch from OpenLibrary by ISBN
- **Import / Export** — CSV import and export, SQL backup and restore with auto-backup on startup
- **Statistics** — per-shelf averages, read percentages, and book counts
- **Dark mode** — full dark/light theme with configurable font size
- **Keyboard shortcuts** — `Ctrl+N` new book, `Ctrl+F` search, `Ctrl+E` edit, `Ctrl+A` select all, `Delete` delete, `Enter` open details

---

## Requirements

- Java 11 or later
- A graphical display (X11 on Linux, native on macOS/Windows)

No installation needed. All dependencies are bundled in `lib/`.

---

## Running

```bash
# Compile and launch
make run

# Run without recompiling (bin/ must already exist)
make run-only

# Or run the prebuilt jar directly
java -jar biblioteca.jar
```

---

## Building from source

```bash
make compile          # compile src/ → bin/
make test             # compile + run all tests (37 tests must pass)
make clean            # remove bin/
```

Manual classpath:

```bash
java -cp bin:lib/h2-2.3.232.jar:lib/mariadb-java-client-3.3.3.jar main.Ejecutable
```

---

## Database

Uses **H2 embedded** by default — no setup required. The database lives at `~/.biblioteca/biblioteca`.

To switch to **MariaDB**, edit `~/.biblioteca/config.properties`:

```properties
dbType=mariadb
dbHost=localhost
dbUser=youruser
dbPassword=yourpassword
```

Schema migrations run automatically on startup.

---

## Populate with sample data

```bash
make populate          # fetch 2000 books from OpenLibrary
make populate MAX=500  # custom limit
```

---

## Project structure

```
src/
  main/          Entry point
  presentacio/   Swing GUI — panels (layout) + controls (logic), one pair per screen
  domini/        Business logic — in-memory sorted list of Llibre
  persistencia/  JDBC layer — ControladorPersistencia → ServerConect
  herramienta/   Utilities — Config, UITheme, LlibreValidator, FiltreUtils, AutoCompletion
  interficie/    EnActualizarBBDD callback interface
lib/
  h2-2.3.232.jar
  mariadb-java-client-3.3.3.jar
test/
  BibliotecaTest.java
```

---

## Configuration

All settings live in `~/.biblioteca/config.properties` and are managed through the in-app settings dialog. Includes window geometry, column widths, dark mode, font size, and DB connection.

---

## License

Personal project. No licence.
