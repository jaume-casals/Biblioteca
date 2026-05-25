# Naming policy

## Public API (interfaces, HTTP, export formats)

Use **English** identifiers: `BibliotecaReader`, `onBookAdded`, `LibraryChangeListener`.

## Domain model

Keep established Catalan terms where they are the business vocabulary: `Llibre`, `Llista`, `Prestec`, `Tag`.

## Internal implementation

Catalan/Spanish method names may remain in legacy UI code (`actualitzarLlibre`) but new presentation code should prefer English (`onBookUpdated`). Deprecated aliases live under `interficie` until callers migrate to `presentacio.listener`.

## Types

Prefer `List<T>` in public signatures; concrete `ArrayList` may be used internally.
